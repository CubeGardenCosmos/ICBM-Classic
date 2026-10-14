package icbm.classic.datagen;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Block states, block/item models and {@code items/<id>.json} model definitions of every block and item
 * registered by {@link ModBlocks}/{@link ModItems} (S2.06, registry-ids.md §1).
 * <p>
 * 26.3 made the {@code BlockModelGenerators} helpers ({@code createTrivialCube}, {@code createFlatItemModel},
 * {@code registerSimpleItemModel}, ...) private, so the provider drives the generator through the public
 * primitives instead: {@link ModelTemplate#create}, {@link MultiVariantGenerator} and
 * {@link BlockModelGenerators.BlockFamilyProvider}. The output of the three vanilla collectors
 * (block states, generated models, item model definitions) is collected here and written with the same
 * codecs vanilla uses ({@code BlockStateModelDispatcher.CODEC}, {@code ClientItem.CODEC},
 * {@code ModelInstance#get}), which is why the files match what vanilla's {@code ModelProvider} would emit.
 * <p>
 * Everything that is a copy of a 1.12 model (3D missile/grenade/bomblet models, machine block models) is a
 * handwritten asset under {@code src/main/resources/assets/icbmclassic/models/} instead.
 */
public class ICBMModelProvider implements DataProvider
{
    /** {@code cube_bottom_top} plus an explicit particle slot: the 1.12 explosive blocks had per face textures. */
    private static final ModelTemplate CUBE_BOTTOM_TOP = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("block/cube_bottom_top")),
            Optional.empty(),
            TextureSlot.TOP, TextureSlot.SIDE, TextureSlot.BOTTOM, TextureSlot.PARTICLE);

    /** Textures of one 1.12 explosive block ({@code blockstates/explosives/<explosive>.json}). */
    private record ExplosiveTextures(String top, String side, String bottom, String particle) {}

    /**
     * Per explosive face textures, copied from the 1.12 {@code blockstates/explosives/<explosive>.json}
     * (up -&gt; top, down -&gt; bottom, the four sides -&gt; side, particle -&gt; particle). File names follow the
     * migrated textures, see docs/implementation/evidence/S2.06.md.
     */
    private static final Map<String, ExplosiveTextures> EXPLOSIVE_TEXTURES = explosiveTextures();

    private final PackOutput.PathProvider blockStatePath;
    private final PackOutput.PathProvider itemPath;
    private final PackOutput.PathProvider modelPath;

    public ICBMModelProvider(FabricPackOutput output)
    {
        this.blockStatePath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.itemPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        this.modelPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache)
    {
        final ModelRegistrar output = new ModelRegistrar();
        generateBlockModels(output);
        generateItemModels(output);
        output.validate();
        return output.save(cache, blockStatePath, itemPath, modelPath);
    }

    @Override
    public String getName()
    {
        return "ICBM block states, models and item model definitions";
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // blocks
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private void generateBlockModels(ModelRegistrar output)
    {
        // 1.12 BlockConcrete: one block with three metadata values, now three blocks (ADR 0002)
        trivialCube(output, ModBlocks.CONCRETE, texture("block/concrete"));
        trivialCube(output, ModBlocks.COMPACT_CONCRETE, texture("block/concrete_compact"));
        trivialCube(output, ModBlocks.REINFORCED_CONCRETE, texture("block/concrete_reinforced"));

        // 1.12 BlockReinforcedGlass: Material.GLASS, so the cube has to be rendered translucent
        trivialCube(output, ModBlocks.REINFORCED_GLASS, new Material(id("block/glass_reinforced"), true));

        // 1.12 BlockSpikes: parent block/tinted_cross with one texture per damage type
        crossBlock(output, ModBlocks.SPIKES, texture("block/spikes"));
        crossBlock(output, ModBlocks.POISON_SPIKES, texture("block/spikes_poison"));
        crossBlock(output, ModBlocks.FIRE_SPIKES, texture("block/spikes_fire"));

        // 1.12 BlockGlassButton/BlockGlassPressurePlate extend the vanilla blocks, so the vanilla family
        // provider generates the models, the block state rotations and (for the button) the item model.
        output.family(TextureMapping.defaultTexture(texture("block/glass_button"))).button(ModBlocks.GLASS_BUTTON);
        output.family(TextureMapping.defaultTexture(texture("block/glass_pressure_plate")))
                .pressurePlate(ModBlocks.GLASS_PRESSURE_PLATE);
        // vanilla renders the pressure plate item with the unpressed model, the family provider leaves that to us
        output.blockItemModel(ModBlocks.GLASS_PRESSURE_PLATE, ModelLocationUtils.getModelLocation(ModBlocks.GLASS_PRESSURE_PLATE));

        // 1.12 BlockRadioactive: models/block/radioactive_{dirt,stone}.json were block/cube_all with the
        // radioactive/<type>/side texture, one state per metadata value (S3.08, ADR 0002)
        trivialCube(output, ModBlocks.RADIOACTIVE_DIRT, texture("block/radioactive/dirt/side"));
        trivialCube(output, ModBlocks.RADIOACTIVE_STONE, texture("block/radioactive/stone/side"));

        // Flat explosive blocks (registry-ids.md §1 form B)
        ModBlocks.EXPLOSIVE_BLOCKS.forEach((explosive, block) -> explosiveBlock(output, block, explosive));
    }

    /** A block rendered as a full cube with the same texture on every face. */
    private static void trivialCube(ModelRegistrar output, Block block, Material material)
    {
        final Identifier model = ModelTemplates.CUBE_ALL.create(block, TextureMapping.cube(material), output::model);
        output.singleVariant(block, model);
        output.blockItemModel(block, model);
    }

    /** A block rendered as a cross (1.12 {@code block/tinted_cross}) with a flat item model, like vanilla plants. */
    private static void crossBlock(ModelRegistrar output, Block block, Material material)
    {
        final Identifier model = ModelTemplates.CROSS.create(block, TextureMapping.cross(material), output::model);
        output.singleVariant(block, model);
        output.flatItemModel(block.asItem(), material);
    }

    /**
     * One flat explosive block: top/side/bottom textures taken from the 1.12 block state.
     * <p>
     * 1.12 {@code explosives.json} rotated the cube by the {@code rotation} property (the face the block was
     * placed with): {@code up {}}, {@code down {x:180}}, {@code north {x:90}}, {@code south {x:270}},
     * {@code east {x:90,y:90}}, {@code west {x:90,y:270}}. That is the vanilla
     * {@code BlockModelGenerators#ROTATIONS_COLUMN_WITH_FACING} dispatch ({@code minecraft:barrel});
     * {@code x:270} and {@code x:90,y:180} are the same rotation, the other five entries match as is.
     */
    private static void explosiveBlock(ModelRegistrar output, Block block, Identifier explosive)
    {
        final ExplosiveTextures textures = EXPLOSIVE_TEXTURES.get(explosive.getPath());
        if (textures == null)
        {
            throw new IllegalStateException("No 1.12 face textures recorded for explosive " + explosive);
        }
        final TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.TOP, texture("block/explosive/" + textures.top()))
                .put(TextureSlot.SIDE, texture("block/explosive/" + textures.side()))
                .put(TextureSlot.BOTTOM, texture("block/explosive/" + textures.bottom()))
                .put(TextureSlot.PARTICLE, texture("block/explosive/" + textures.particle()));
        final Identifier model = CUBE_BOTTOM_TOP.create(block, mapping, output::model);
        output.facingVariants(block, model, BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING);
        output.blockItemModel(block, model);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // items
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private void generateItemModels(ModelRegistrar output)
    {
        // 1.12 ItemCrafting and the plain items, one texture per ported item
        // (docs/implementation/evidence/S2.06.md lists the 1.12 -> 26.3 texture names)
        flatItem(output, ModItems.STEEL_INGOT, "item/steel_ingot");
        flatItem(output, ModItems.COPPER_INGOT, "item/copper_ingot");
        flatItem(output, ModItems.STEEL_CLUMP, "item/steel_clump");
        flatItem(output, ModItems.STEEL_PLATE, "item/steel_plate");
        flatItem(output, ModItems.IRON_PLATE, "item/iron_plate");
        flatItem(output, ModItems.BASIC_CIRCUIT, "item/basic_circuit");
        flatItem(output, ModItems.ADVANCED_CIRCUIT, "item/advanced_circuit");
        flatItem(output, ModItems.ELITE_CIRCUIT, "item/elite_circuit");
        flatItem(output, ModItems.COPPER_WIRE, "item/copper_wire");
        flatItem(output, ModItems.GOLD_WIRE, "item/gold_wire");
        flatItem(output, ModItems.SULFUR_DUST, "item/sulfur_dust");
        flatItem(output, ModItems.SALTPETER, "item/saltpeter");
        flatItem(output, ModItems.SALTPETER_BALL, "item/saltpeter_ball");
        flatItem(output, ModItems.POISON_POWDER, "item/poison_powder");
        flatItem(output, ModItems.ANTIDOTE, "item/antidote");

        // 1.12 models/item/bombcarts/<explosive>.json all render the vanilla TNT minecart texture
        ModItems.EXPLOSIVE_BOMB_CARTS.values().forEach(item -> flatItem(output, item, "minecraft:item/tnt_minecart"));

        // Handwritten models copied from 1.12 (3D missiles and bomblets, displayed grenades)
        ModItems.EXPLOSIVE_MISSILES.forEach((explosive, item) ->
                copiedModel(output, item, "item/missiles/" + explosive.getPath()));
        ModItems.EXPLOSIVE_GRENADES.forEach((explosive, item) ->
                copiedModel(output, item, "item/grenades/" + explosive.getPath()));
        ModItems.EXPLOSIVE_BOMBLETS.values().forEach(item -> copiedModel(output, item, "item/bomblet"));
        copiedModel(output, ModItems.EMPTY_BOMBLET, "item/bomblet");
        copiedModel(output, ModItems.MISSILE_MODULE, "item/missiles/missile");
    }

    /**
     * A flat 2D item model generated from a texture, the 26.3 equivalent of 1.12 {@code item/generated}.
     * {@code spritePath} is either a mod texture path ({@code item/steel_ingot}) or a full sprite of
     * another namespace ({@code minecraft:item/tnt_minecart}, reused by the 1.12 bomb cart models).
     */
    private static void flatItem(ModelRegistrar output, Item item, String spritePath)
    {
        output.flatItemModel(item, spritePath.contains(":") ? new Material(Identifier.parse(spritePath)) : texture(spritePath));
    }

    /** An item that renders a model copied from 1.12 as is (3D missile, grenade, bomblet). */
    private static void copiedModel(ModelRegistrar output, Item item, String modelPath)
    {
        output.itemModel(item, id(modelPath));
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // 1.12 face textures of the flat explosive blocks
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static Map<String, ExplosiveTextures> explosiveTextures()
    {
        final Map<String, ExplosiveTextures> textures = new LinkedHashMap<>();
        // tiers 1-3 share one "bottom" texture for top and bottom, the sides carry the explosive artwork
        for (String explosive : List.of("anvil", "attractive", "chemical", "colors", "debilitation", "incendiary",
                "repulsive", "smoke"))
        {
            textures.put(explosive, uniform(explosive, "bottom_1"));
        }
        for (String explosive : List.of("contagious", "fragmentation", "sonic", "thermobaric"))
        {
            textures.put(explosive, uniform(explosive, "bottom_2"));
        }
        for (String explosive : List.of("antigravitational", "emp", "ender", "endothermic", "exothermic", "nuclear"))
        {
            textures.put(explosive, uniform(explosive, "bottom_3"));
        }
        // explosives with dedicated top/bottom art
        textures.put("condensed", new ExplosiveTextures("condensed_top", "condensed_side", "bottom_1", "condensed_top"));
        textures.put("shrapnel", new ExplosiveTextures("bottom_1", "shrapnel_side", "bottom_1", "shrapnel_side"));
        textures.put("breaching", new ExplosiveTextures("breaching_top", "breaching_side", "bottom_2", "breaching_top"));
        textures.put("antimatter", new ExplosiveTextures("antimatter", "antimatter", "redmatter_bottom", "antimatter"));
        textures.put("redmatter", new ExplosiveTextures("redmatter_top", "redmatter_side", "redmatter_bottom", "redmatter_side"));
        return Map.copyOf(textures);
    }

    private static ExplosiveTextures uniform(String explosive, String bottom)
    {
        return new ExplosiveTextures(bottom, explosive, bottom, explosive);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // helpers
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static Identifier id(String path)
    {
        return ICBMConstants.id(path);
    }

    private static Material texture(String path)
    {
        return new Material(id(path));
    }

    /**
     * Collects everything the model generators emit and writes it with the vanilla codecs.
     * <p>
     * {@link BlockModelGenerators} keeps its three outputs private, this is the collector set it is
     * constructed with (the same three the vanilla {@code ModelProvider} passes).
     */
    private static final class ModelRegistrar
    {
        private final Map<Block, BlockModelDefinitionGenerator> blockStates = new LinkedHashMap<>();
        private final Map<Identifier, ModelInstance> models = new LinkedHashMap<>();
        private final ItemModels items = new ItemModels();
        private final BlockModelGenerators generator =
                new BlockModelGenerators(this::blockState, this.items, this.models::put);

        /** Vanilla button/pressure plate model, block state and item model generation. */
        BlockModelGenerators.BlockFamilyProvider family(TextureMapping mapping)
        {
            return generator.new BlockFamilyProvider(mapping);
        }

        /** A block with one block state pointing at a single model (1.12 {@code "defaults": {"model": ...}}). */
        void singleVariant(Block block, Identifier model)
        {
            blockState(MultiVariantGenerator.dispatch(block, new MultiVariant(WeightedList.of(new Variant(model)))));
        }

        /**
         * A block whose model is rotated per block state property, like vanilla's barrel/end rod
         * ({@code MultiVariantGenerator#dispatch(Block, MultiVariant)} plus a {@link PropertyDispatch} of
         * {@link VariantMutator}s).
         */
        void facingVariants(Block block, Identifier model, PropertyDispatch<VariantMutator> rotations)
        {
            blockState(MultiVariantGenerator
                    .dispatch(block, new MultiVariant(WeightedList.of(new Variant(model))))
                    .with(rotations));
        }

        void blockState(BlockModelDefinitionGenerator definition)
        {
            if (blockStates.put(definition.block(), definition) != null)
            {
                throw new IllegalStateException("Duplicate block state definition for " + definition.block());
            }
        }

        void model(Identifier model, ModelInstance instance)
        {
            if (models.put(model, instance) != null)
            {
                throw new IllegalStateException("Duplicate model definition for " + model);
            }
        }

        /** {@code items/<id>.json} for a block item that renders the block model, like vanilla full blocks. */
        void blockItemModel(Block block, Identifier model)
        {
            itemModel(block.asItem(), model);
        }

        /** {@code items/<id>.json} pointing at a generated {@code item/<id>} model, like 1.12 {@code item/generated}. */
        void flatItemModel(Item item, Material material)
        {
            itemModel(item, ModelTemplates.FLAT_ITEM.create(item, TextureMapping.layer0(material), this::model));
        }

        void itemModel(Item item, Identifier model)
        {
            items.accept(item, ItemModelUtils.plainModel(model), ClientItem.Properties.DEFAULT);
        }

        /** Fails the data generation when a registered block or item has no model, the check the card asks for. */
        void validate()
        {
            items.resolveCopies();
            final List<String> missing = new ArrayList<>();
            BuiltInRegistries.BLOCK.forEach(block ->
            {
                final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (isOurs(id) && !blockStates.containsKey(block))
                {
                    missing.add("block " + id);
                }
            });
            BuiltInRegistries.ITEM.forEach(item ->
            {
                final Identifier id = BuiltInRegistries.ITEM.getKey(item);
                if (isOurs(id) && !items.has(item))
                {
                    missing.add("item " + id);
                }
            });
            if (!missing.isEmpty())
            {
                throw new IllegalStateException("Missing model definitions: " + missing);
            }
        }

        private static boolean isOurs(Identifier identifier)
        {
            return ICBMConstants.DOMAIN.equals(identifier.getNamespace());
        }

        CompletableFuture<?> save(CachedOutput cache, PackOutput.PathProvider blockStatePath,
                PackOutput.PathProvider itemPath, PackOutput.PathProvider modelPath)
        {
            final Map<Block, BlockStateModelDispatcher> dispatchers = new LinkedHashMap<>();
            blockStates.forEach((block, definition) -> dispatchers.put(block, definition.create()));
            return CompletableFuture.allOf(
                    DataProvider.saveAll(cache, BlockStateModelDispatcher.CODEC,
                            block -> blockStatePath.json(BuiltInRegistries.BLOCK.getKey(block)), dispatchers),
                    DataProvider.saveAll(cache, ClientItem.CODEC,
                            item -> itemPath.json(BuiltInRegistries.ITEM.getKey(item)), items.models),
                    DataProvider.saveAll(cache, ModelInstance::get, modelPath::json, models));
        }
    }

    /** The {@link ItemModelOutput} of the model generator, one {@code items/<id>.json} per item. */
    private static final class ItemModels implements ItemModelOutput
    {
        private final Map<Item, ClientItem> models = new LinkedHashMap<>();
        private final Map<Item, Item> copies = new LinkedHashMap<>();

        @Override
        public void accept(Item item, ItemModel.Unbaked model, ClientItem.Properties properties)
        {
            if (models.put(item, new ClientItem(model, properties)) != null)
            {
                throw new IllegalStateException("Duplicate item model definition for " + item);
            }
        }

        @Override
        public void copy(Item source, Item target)
        {
            if (copies.put(target, source) != null)
            {
                throw new IllegalStateException("Duplicate item model copy for " + target);
            }
        }

        boolean has(Item item)
        {
            return models.containsKey(item) || copies.containsKey(item);
        }

        void resolveCopies()
        {
            copies.forEach((target, source) ->
            {
                final ClientItem model = models.get(source);
                if (model == null)
                {
                    throw new IllegalStateException("Cannot copy the item model of " + source + " to " + target);
                }
                models.put(target, model);
            });
            copies.clear();
        }
    }
}
