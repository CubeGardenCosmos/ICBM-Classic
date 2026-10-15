package icbm.classic.datagen;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ExplosiveForms;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

/**
 * Recipes of the 1.12 {@code assets/icbmclassic/recipes/**} tree (S2.08).
 * <p>
 * 1.12 used Forge's {@code _factories.json} with two factories: {@code icbmclassic:explosive} recipes (a shaped
 * recipe whose result and {@code icbmclassic:explosive} ingredients resolve the device stack of an explosive,
 * see {@code lib/recipe/ExRecipeFactory}) and {@code forge:ore_dict} ingredients. ADR 0002 flattens the
 * metadata, so every one of those turns into a plain 26.3 shaped/shapeless recipe with concrete items: the
 * device stack becomes {@code <explosive>_explosive}/{@code _missile}/{@code _grenade}/{@code _bomb_cart}/
 * {@code _bomblet} and the OreDictionary names become {@code c:} tags (S2.07) or vanilla items/tags.
 * <p>
 * Recipes whose output is not registered yet (tools, machines, launcher parts, battery, balloon, parachute,
 * cluster/held item missiles) are listed in the evidence file with the step that ports them
 * instead of being generated here, and the 1.12 fallback variants that only exist for "other mod is absent"
 * (uranium, mekanism, carbon dust) are replaced by their vanilla variants where 1.12 shipped one.
 */
public class ICBMRecipeProvider extends FabricRecipeProvider
{
    public ICBMRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes,
            BootstrapContext<Advancement> advancements)
    {
        return new ICBMRecipes(recipes, advancements, registries.lookupOrThrow(Registries.ITEM));
    }

    /** The recipe builders of the mod, one method per 1.12 recipe folder. */
    private static final class ICBMRecipes extends RecipeProvider
    {
        private final HolderGetter<Item> items;

        private ICBMRecipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, HolderGetter<Item> items)
        {
            super(recipes, advancements);
            this.items = items;
        }

        @Override
        public void buildRecipes()
        {
            simpleBlocks();
            materials();
            explosiveBlocks();
            flatForms();
            missileSpecials();
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // 1.12 recipes/*.json: concrete, reinforced glass, spikes, glass button and pressure plate
        //////////////////////////////////////////////////////////////////////////////////////////////

        private void simpleBlocks()
        {
            // 1.12 concrete.json: sand + gravel + water bucket, eight at a time
            ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE, 8)
                    .define('s', Items.SAND).define('g', Items.GRAVEL).define('w', Items.WATER_BUCKET)
                    .pattern("sgs").pattern("gwg").pattern("sgs")
                    .unlockedBy("has_sand", has(Items.SAND))
                    .save(output, key("concrete"));

            // 1.12 concrete.1.json (meta 1) and concrete.2.json (meta 2)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ModBlocks.COMPACT_CONCRETE, 8)
                    .define('c', ModBlocks.CONCRETE).define('o', Blocks.OBSIDIAN)
                    .pattern("coc").pattern("oco").pattern("coc")
                    .unlockedBy("has_concrete", has(ModBlocks.CONCRETE))
                    .save(output, key("compact_concrete"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_CONCRETE, 8)
                    .define('c', ModBlocks.COMPACT_CONCRETE).define('o', Ingredient.of(Items.IRON_INGOT))
                    .pattern("coc").pattern("oco").pattern("coc")
                    .unlockedBy("has_compact_concrete", has(ModBlocks.COMPACT_CONCRETE))
                    .save(output, key("reinforced_concrete"));

            // 1.12 reinforcedGlass.json (ore:ingotIron -> vanilla iron ingot, like the other vanilla ores)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_GLASS, 8)
                    .define('g', Blocks.GLASS).define('i', Ingredient.of(Items.IRON_INGOT))
                    .pattern("igi").pattern("gig").pattern("igi")
                    .unlockedBy("has_glass", has(Blocks.GLASS))
                    .save(output, key("reinforced_glass"));

            // 1.12 spikes.json, spikes.1.json (meta 1, poison) and spikes.2.json (meta 2, fire)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ModBlocks.SPIKES, 6)
                    .define('c', Blocks.CACTUS).define('i', Ingredient.of(Items.IRON_INGOT))
                    .pattern("ccc").pattern("iii")
                    .group("icbmclassic:spikes")
                    .unlockedBy("has_cactus", has(Blocks.CACTUS))
                    .save(output, key("spikes"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ModBlocks.POISON_SPIKES)
                    .define('c', ModItems.POISON_POWDER).define('i', ModBlocks.SPIKES)
                    .pattern("c").pattern("i")
                    .group("icbmclassic:spikes_poison")
                    .unlockedBy("has_poison_powder", has(ModItems.POISON_POWDER))
                    .save(output, key("poison_spikes"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.DECORATIONS, ModBlocks.FIRE_SPIKES)
                    .define('c', sulfurTag()).define('i', ModBlocks.SPIKES)
                    .pattern("c").pattern("i")
                    .group("icbmclassic:spikes_fire")
                    .unlockedBy("has_sulfur_dust", has(ModItems.SULFUR_DUST))
                    .save(output, key("fire_spikes"));

            // 1.12 glassButton.json / glassPressurePlate.json
            ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ModBlocks.GLASS_BUTTON, 2)
                    .define('g', Blocks.GLASS)
                    .pattern("g").pattern("g").pattern("g")
                    .unlockedBy("has_glass", has(Blocks.GLASS))
                    .save(output, key("glass_button"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.REDSTONE, ModBlocks.GLASS_PRESSURE_PLATE)
                    .define('g', Blocks.GLASS)
                    .pattern("ggg")
                    .unlockedBy("has_glass", has(Blocks.GLASS))
                    .save(output, key("glass_pressure_plate"));
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // 1.12 recipes/{parts,parts/vanilla,bomblet,missile}: materials, gunpowder, saltpeter, module
        //////////////////////////////////////////////////////////////////////////////////////////////

        private void materials()
        {
            // 1.12 antidote.json: nine seeds, any of the three vanilla seed types
            ShapedRecipeBuilder.shaped(items, RecipeCategory.FOOD, ModItems.ANTIDOTE, 6)
                    .define('s', Ingredient.of(Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.WHEAT_SEEDS))
                    .pattern("sss").pattern("sss").pattern("sss")
                    .unlockedBy("has_wheat_seeds", has(Items.WHEAT_SEEDS))
                    .save(output, key("antidote"));

            // 1.12 powder.poison.json
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.POISON_POWDER, 3)
                    .define('e', Items.SPIDER_EYE).define('r', Items.ROTTEN_FLESH)
                    .pattern("er")
                    .unlockedBy("has_spider_eye", has(Items.SPIDER_EYE))
                    .save(output, key("poison_powder"));

            // 1.12 parts/clump.steel.json (coal meta 0/1 = coal or charcoal)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.STEEL_CLUMP)
                    .define('i', Ingredient.of(Items.IRON_INGOT))
                    .define('c', Ingredient.of(Items.COAL, Items.CHARCOAL))
                    .pattern("ic").pattern("cc")
                    .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                    .save(output, key("steel_clump"));

            // 1.12 parts/plate.steel.json (ore:ingotSteel -> c:ingots/steel, S2.07)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.STEEL_PLATE, 4)
                    .define('i', steelTag())
                    .pattern("ii").pattern("ii")
                    .unlockedBy("has_steel_ingot", has(ModItems.STEEL_INGOT))
                    .save(output, key("steel_plate"));

            // 1.12 parts/circuit.basic.json (ore:plankWood -> minecraft:planks)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.BASIC_CIRCUIT, 4)
                    .define('r', Items.REDSTONE).define('c', Items.CLAY_BALL)
                    .define('i', copperTag()).define('b', ItemTags.PLANKS)
                    .pattern("rcr").pattern("iii").pattern("bbb")
                    .unlockedBy("has_copper_ingot", has(ModItems.COPPER_INGOT))
                    .save(output, key("basic_circuit"));

            // 1.12 parts/circuit.advanced.json
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.ADVANCED_CIRCUIT)
                    .define('w', ModItems.COPPER_WIRE).define('c', copperTag())
                    .define('i', Items.REDSTONE_TORCH).define('b', ModItems.BASIC_CIRCUIT)
                    .pattern("wcw").pattern("ibi").pattern("wcw")
                    .unlockedBy("has_basic_circuit", has(ModItems.BASIC_CIRCUIT))
                    .save(output, key("advanced_circuit"));

            // 1.12 parts/circuit.elite.json
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.ELITE_CIRCUIT)
                    .define('w', ModItems.GOLD_WIRE).define('c', Items.QUARTZ)
                    .define('i', Ingredient.of(Items.GOLD_INGOT)).define('b', ModItems.BASIC_CIRCUIT)
                    .pattern("wcw").pattern("ibi").pattern("wcw")
                    .unlockedBy("has_basic_circuit", has(ModItems.BASIC_CIRCUIT))
                    .save(output, key("elite_circuit"));

            // 1.12 parts/wire.copper.json and parts/wire.gold.json (ore:stickWood -> vanilla stick)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.COPPER_WIRE, 18)
                    .define('c', copperTag()).define('s', Ingredient.of(Items.STICK)).define('w', ItemTags.PLANKS)
                    .pattern("cwc").pattern("csc").pattern("cwc")
                    .unlockedBy("has_copper_ingot", has(ModItems.COPPER_INGOT))
                    .save(output, key("copper_wire"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.GOLD_WIRE, 18)
                    .define('g', Ingredient.of(Items.GOLD_INGOT)).define('s', Ingredient.of(Items.STICK)).define('w', ItemTags.PLANKS)
                    .pattern("gwg").pattern("gsg").pattern("gwg")
                    .unlockedBy("has_gold_ingot", has(Items.GOLD_INGOT))
                    .save(output, key("gold_wire"));

            // 1.12 parts/vanilla/gunpowder.json: the variant used when no carbon dust exists (26.3 has none)
            ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, Items.GUNPOWDER, 3)
                    .requires(Ingredient.of(Items.COAL, Items.CHARCOAL))
                    .requires(sulfurTag())
                    .requires(saltpeterTag())
                    .unlockedBy("has_sulfur_dust", has(ModItems.SULFUR_DUST))
                    .save(output, key("gunpowder"));

            // 1.12 parts/vanilla/saltpeter.json: dirt + (leaves or saplings) + water bucket. 26.3 has no tag
            // that holds both, so the OR becomes two recipes with the same output.
            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.SALTPETER_BALL, 4)
                    .define('D', ItemTags.DIRT).define('P', ItemTags.LEAVES).define('W', Items.WATER_BUCKET)
                    .pattern("DPD").pattern("PWP").pattern("DPD")
                    .unlockedBy("has_leaves", has(ItemTags.LEAVES))
                    .save(output, key("saltpeter_ball"));

            ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, ModItems.SALTPETER_BALL, 4)
                    .define('D', ItemTags.DIRT).define('P', ItemTags.SAPLINGS).define('W', Items.WATER_BUCKET)
                    .pattern("DPD").pattern("PWP").pattern("DPD")
                    .unlockedBy("has_saplings", has(ItemTags.SAPLINGS))
                    .save(output, key("saltpeter_ball_from_saplings"));

            // 1.12 bomblet/bomblet.json (wooden_button meta 0 = oak button)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.EMPTY_BOMBLET, 16)
                    .define('i', steelTag()).define('c', ModItems.BASIC_CIRCUIT).define('p', Blocks.OAK_BUTTON)
                    .pattern(" i ").pattern("ici").pattern("ipi")
                    .unlockedBy("has_basic_circuit", has(ModItems.BASIC_CIRCUIT))
                    .save(output, key("empty_bomblet"));

            // 1.12 missile/missile.module.json: the empty missile shell (flat id missile_module)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.MISSILE_MODULE)
                    .define('i', steelTag()).define('c', ModItems.BASIC_CIRCUIT).define('f', Items.FLINT_AND_STEEL)
                    .pattern(" i ").pattern("ici").pattern("ifi")
                    .unlockedBy("has_basic_circuit", has(ModItems.BASIC_CIRCUIT))
                    .save(output, key("missile_module"));
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // 1.12 recipes/explosives/**: one recipe per flat explosive block (23)
        //////////////////////////////////////////////////////////////////////////////////////////////

        private void explosiveBlocks()
        {
            // tier 1
            ring(block("condensed"), 3, Blocks.TNT, Items.REDSTONE)
                    .save(output, key("condensed_explosive"));
            ring(block("shrapnel"), 1, Items.ARROW, block("condensed"))
                    .save(output, key("shrapnel_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("incendiary"))
                    .define('a', sulfurTag()).define('l', Items.LAVA_BUCKET).define('r', block("shrapnel"))
                    .pattern("aaa").pattern("ara").pattern("ala")
                    .unlockedBy("has_sulfur_dust", has(ModItems.SULFUR_DUST))
                    .save(output, key("incendiary_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("debilitation"), 3)
                    .define('s', sulfurTag()).define('w', Items.WATER_BUCKET).define('r', block("shrapnel"))
                    .pattern("sss").pattern("wrw").pattern("sss")
                    .unlockedBy("has_sulfur_dust", has(ModItems.SULFUR_DUST))
                    .save(output, key("debilitation_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("chemical"))
                    .define('p', ModItems.POISON_POWDER).define('r', block("debilitation"))
                    .pattern("ppp").pattern("prp").pattern("ppp")
                    .unlockedBy("has_poison_powder", has(ModItems.POISON_POWDER))
                    .save(output, key("chemical_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("anvil"), 10)
                    .define('a', Blocks.ANVIL).define('r', block("repulsive"))
                    .pattern("aaa").pattern("ara").pattern("aaa")
                    .unlockedBy("has_anvil", has(Blocks.ANVIL))
                    .save(output, key("anvil_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("repulsive"))
                    .define('c', block("condensed"))
                    .pattern("c").pattern("c")
                    .unlockedBy("has_condensed_explosive", has(block("condensed")))
                    .save(output, key("repulsive_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("attractive"))
                    .define('c', block("condensed"))
                    .pattern("cc")
                    .unlockedBy("has_condensed_explosive", has(block("condensed")))
                    .save(output, key("attractive_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("colors"))
                    .define('C', ItemTags.DYES).define('r', block("repulsive"))
                    .pattern("CCC").pattern("CrC").pattern("CCC")
                    .unlockedBy("has_repulsive_explosive", has(block("repulsive")))
                    .save(output, key("colors_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("smoke"), 16)
                    .define('C', Ingredient.of(Items.CHARCOAL)).define('r', block("repulsive"))
                    .pattern("CCC").pattern("CrC").pattern("CCC")
                    .unlockedBy("has_repulsive_explosive", has(block("repulsive")))
                    .save(output, key("smoke_explosive"));

            // tier 2
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("fragmentation"))
                    .define('a', block("shrapnel")).define('r', block("incendiary"))
                    .pattern(" a ").pattern("ara").pattern(" a ")
                    .unlockedBy("has_shrapnel_explosive", has(block("shrapnel")))
                    .save(output, key("fragmentation_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("contagious"), 2)
                    .define('c', block("chemical")).define('r', Items.ROTTEN_FLESH)
                    .pattern(" c ").pattern("crc").pattern(" c ")
                    .unlockedBy("has_chemical_explosive", has(block("chemical")))
                    .save(output, key("contagious_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("sonic"))
                    .define('i', Ingredient.of(Items.IRON_INGOT)).define('n', Blocks.NOTE_BLOCK).define('r', block("repulsive"))
                    .pattern("ini").pattern("nrn").pattern("ini")
                    .unlockedBy("has_repulsive_explosive", has(block("repulsive")))
                    .save(output, key("sonic_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("breaching"), 2)
                    .define('g', Items.GUNPOWDER).define('r', block("condensed"))
                    .pattern("grg").pattern("grg").pattern("grg")
                    .unlockedBy("has_condensed_explosive", has(block("condensed")))
                    .save(output, key("breaching_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("thermobaric"))
                    .define('i', block("incendiary")).define('c', block("chemical")).define('r', block("repulsive"))
                    .pattern("cic").pattern("iri").pattern("cic")
                    .unlockedBy("has_repulsive_explosive", has(block("repulsive")))
                    .save(output, key("thermobaric_explosive"));

            // tier 3
            // 1.12 explosives/nuclear.json needs uranium (only a fallback recipe exists for "no uranium mod"),
            // 26.3 has no uranium at all, so the 1.12 explosives/vanilla/nuclear.json fallback is the recipe
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("nuclear"))
                    .define('t', block("thermobaric")).define('i', Items.WITHER_SKELETON_SKULL)
                    .pattern("tit").pattern("iii").pattern("tit")
                    .unlockedBy("has_thermobaric_explosive", has(block("thermobaric")))
                    .save(output, key("nuclear_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("exothermic"))
                    .define('i', block("incendiary")).define('g', Blocks.GLASS)
                    .pattern("iii").pattern("igi").pattern("iii")
                    .unlockedBy("has_incendiary_explosive", has(block("incendiary")))
                    .save(output, key("exothermic_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("endothermic"))
                    .define('i', Blocks.ICE).define('s', Blocks.SNOW).define('a', block("attractive"))
                    .pattern("isi").pattern("sas").pattern("isi")
                    .unlockedBy("has_attractive_explosive", has(block("attractive")))
                    .save(output, key("endothermic_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("antigravitational"))
                    .define('e', Items.ENDER_EYE).define('r', block("shrapnel"))
                    .pattern("eee").pattern("ere").pattern("eee")
                    .unlockedBy("has_shrapnel_explosive", has(block("shrapnel")))
                    .save(output, key("antigravitational_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("ender"))
                    .define('s', Items.END_STONE).define('e', Items.ENDER_EYE).define('a', block("attractive"))
                    .pattern("ses").pattern("eae").pattern("ses")
                    .unlockedBy("has_attractive_explosive", has(block("attractive")))
                    .save(output, key("ender_explosive"));

            // tier 4: 1.12 explosives/vanilla/*.json (the variants without mekanism/uranium)
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("antimatter"))
                    .define('n', Items.NETHER_STAR).define('r', block("nuclear"))
                    .pattern("nnn").pattern("nrn").pattern("nnn")
                    .unlockedBy("has_nuclear_explosive", has(block("nuclear")))
                    .save(output, key("antimatter_explosive"));
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, block("redmatter"))
                    .define('n', Items.NETHER_STAR).define('r', block("antimatter"))
                    .pattern("nnn").pattern("nrn").pattern("nnn")
                    .unlockedBy("has_antimatter_explosive", has(block("antimatter")))
                    .save(output, key("redmatter_explosive"));

            // TODO(S6) emp_explosive: 1.12 explosives/emp.json needs the battery item (S6)
        }

        /** 8 of {@code ring} around one {@code center}, the shape of most 1.12 explosive block recipes. */
        private ShapedRecipeBuilder ring(ItemLike result, int count, ItemLike ring, ItemLike center)
        {
            return ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, result, count)
                    .define('a', ring).define('r', center)
                    .pattern("aaa").pattern("ara").pattern("aaa")
                    .unlockedBy("has_" + getItemName(center), has(center));
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // 1.12 recipes/{missile/ex,grenades,bombcarts,bomblet}: the flat forms of every explosive
        //////////////////////////////////////////////////////////////////////////////////////////////

        private void flatForms()
        {
            // 1.12 missile/ex/<explosive>.json: pattern "mx" (missile module + the explosive block)
            for (Identifier explosive : ExplosiveForms.enabled(ExplosiveForm.MISSILE))
            {
                ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.EXPLOSIVE_MISSILES.get(explosive))
                        .define('m', ModItems.MISSILE_MODULE).define('x', block(explosive.getPath()))
                        .pattern("m").pattern("x")
                        .unlockedBy("has_missile_module", has(ModItems.MISSILE_MODULE))
                        .save(output, key(ExplosiveForm.MISSILE.idFor(explosive)));
            }

            // 1.12 grenades/<explosive>.json: pattern "s", "x" (string above the explosive block)
            for (Identifier explosive : ExplosiveForms.enabled(ExplosiveForm.GRENADE))
            {
                ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.EXPLOSIVE_GRENADES.get(explosive))
                        .define('s', Items.STRING).define('x', block(explosive.getPath()))
                        .pattern("s").pattern("x")
                        .unlockedBy("has_string", has(Items.STRING))
                        .save(output, key(ExplosiveForm.GRENADE.idFor(explosive)));
            }

            // 1.12 bombcarts/<explosive>.json: pattern "a", "c" (explosive block above a minecart)
            for (Identifier explosive : ExplosiveForms.enabled(ExplosiveForm.BOMB_CART))
            {
                ShapedRecipeBuilder.shaped(items, RecipeCategory.TRANSPORTATION, ModItems.EXPLOSIVE_BOMB_CARTS.get(explosive))
                        .define('a', block(explosive.getPath())).define('c', Items.MINECART)
                        .pattern("a").pattern("c")
                        .unlockedBy("has_minecart", has(Items.MINECART))
                        .save(output, key(ExplosiveForm.BOMB_CART.idFor(explosive)));
            }

            // 1.12 bomblet/<explosive>.json: pattern "mx" (empty bomblet + the explosive block)
            for (Identifier explosive : ExplosiveForms.enabled(ExplosiveForm.BOMBLET))
            {
                ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.EXPLOSIVE_BOMBLETS.get(explosive))
                        .define('m', ModItems.EMPTY_BOMBLET).define('x', block(explosive.getPath()))
                        .pattern("m").pattern("x")
                        .unlockedBy("has_empty_bomblet", has(ModItems.EMPTY_BOMBLET))
                        .save(output, key(ExplosiveForm.BOMBLET.idFor(explosive)));
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // 1.12 recipes/missile/special/missile.anti_ballistic.json: circuitBasic / condensed / missile module
        //////////////////////////////////////////////////////////////////////////////////////////////

        private void missileSpecials()
        {
            ShapedRecipeBuilder.shaped(items, RecipeCategory.COMBAT, ModItems.ANTI_BALLISTIC_MISSILE)
                    .define('c', ModItems.BASIC_CIRCUIT).define('e', block("condensed")).define('m', ModItems.MISSILE_MODULE)
                    .pattern("c").pattern("e").pattern("m")
                    .unlockedBy("has_missile_module", has(ModItems.MISSILE_MODULE))
                    .save(output, key("anti_ballistic_missile"));
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        // helpers
        //////////////////////////////////////////////////////////////////////////////////////////////

        /** The flat explosive block of an explosive id. */
        private ItemLike block(String explosive)
        {
            return ModBlocks.EXPLOSIVE_BLOCKS.get(ICBMConstants.id(explosive));
        }

        private static ResourceKey<Recipe<?>> key(String path)
        {
            return ResourceKey.create(Registries.RECIPE, ICBMConstants.id(path));
        }

        private static net.minecraft.tags.TagKey<Item> steelTag()
        {
            return commonTag("ingots/steel");
        }

        private static net.minecraft.tags.TagKey<Item> copperTag()
        {
            return commonTag("ingots/copper");
        }

        private static net.minecraft.tags.TagKey<Item> sulfurTag()
        {
            return commonTag("dusts/sulfur");
        }

        private static net.minecraft.tags.TagKey<Item> saltpeterTag()
        {
            return commonTag("dusts/saltpeter");
        }

        private static net.minecraft.tags.TagKey<Item> commonTag(String path)
        {
            return net.minecraft.tags.TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
        }
    }
}
