package icbm.classic.content.reg;

import icbm.classic.content.blocks.BlockGlassButton;
import icbm.classic.content.blocks.BlockGlassPressurePlate;
import icbm.classic.content.blocks.BlockReinforcedGlass;
import icbm.classic.content.blocks.BlockSpikes;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Block registration. Renamed from the 1.12 {@code BlockReg} (porting-guide.md §3).
 * <p>
 * The 1.12 class registered every block plus the tile entity types in one event. 26.3 splits this into
 * {@code ModBlocks} (blocks + block items) and {@link ModBlockEntities} (block entity types) so the
 * documented order in overview.md §5 can be followed.
 * <p>
 * Filled by S2.03 (simple blocks), S2.05 (flat explosives) and S5 (machines); S3.08 adds the radioactive
 * blocks.
 */
public final class ModBlocks
{
    //=================== S2.03 simple blocks
    /** 1.12 {@code concrete} meta 0: hardness 10, explosion resistance 28. */
    public static Block CONCRETE;
    /** 1.12 {@code concrete} meta 1 (COMPACT): explosion resistance 280. */
    public static Block COMPACT_CONCRETE;
    /** 1.12 {@code concrete} meta 2 (REINFORCED): explosion resistance 2800. */
    public static Block REINFORCED_CONCRETE;

    /** 1.12 {@code reinforcedGlass}: hardness 10, resistance 48, silk harvest (loot table in S2.07). */
    public static Block REINFORCED_GLASS;

    /** 1.12 {@code spikes} meta 0. */
    public static Block SPIKES;
    /** 1.12 {@code spikes} meta 1, poison damage + poison effect. */
    public static Block POISON_SPIKES;
    /** 1.12 {@code spikes} meta 2, fire damage + set on fire. */
    public static Block FIRE_SPIKES;

    /** 1.12 {@code glassButton}: stone button behaviour, glass sound. */
    public static Block GLASS_BUTTON;
    /** 1.12 {@code glassPressurePlate}: everything sensitive, glass sound. */
    public static Block GLASS_PRESSURE_PLATE;

    /**
     * Flat explosive blocks by explosive id (registry-ids.md §1, form B). S3.09 turns these into working
     * explosive blocks with a shared block entity.
     */
    public static final Map<Identifier, Block> EXPLOSIVE_BLOCKS = new LinkedHashMap<>();

    private ModBlocks() {}

    /** Registers every block and its block item. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        //Concrete: 1.12 used one block with three metadata values and a per value getExplosionResistance
        CONCRETE = RegHelper.block("concrete", Block::new, concreteProperties(28f));
        COMPACT_CONCRETE = RegHelper.block("compact_concrete", Block::new, concreteProperties(280f));
        REINFORCED_CONCRETE = RegHelper.block("reinforced_concrete", Block::new, concreteProperties(2800f));

        // 1.12: Material.GLASS, setHardness(10), setResistance(48), not opaque, not a full cube
        REINFORCED_GLASS = RegHelper.block("reinforced_glass", BlockReinforcedGlass::new,
                BlockBehaviour.Properties.of().strength(10f, 48f).noOcclusion());

        // 1.12: Material.IRON, setHardness(1), no collision box, not opaque, not a full cube
        final BlockBehaviour.Properties spikes = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1f)
                .noCollision()
                .noOcclusion();
        SPIKES = RegHelper.block("spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.NORMAL, p), spikes);
        POISON_SPIKES = RegHelper.block("poison_spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.POISON, p), spikes);
        FIRE_SPIKES = RegHelper.block("fire_spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.FIRE, p), spikes);

        // 1.12: stone button behaviour with setSoundType(SoundType.GLASS) and hardness 0.5
        GLASS_BUTTON = RegHelper.block("glass_button", BlockGlassButton::new,
                BlockBehaviour.Properties.of().noCollision().strength(0.5f).sound(SoundType.GLASS));

        // 1.12: Material.GLASS, Sensitivity.EVERYTHING, hardness 0.3, resistance 1, setSoundType(SoundType.GLASS)
        GLASS_PRESSURE_PLATE = RegHelper.block("glass_pressure_plate", BlockGlassPressurePlate::new,
                BlockBehaviour.Properties.of().noCollision().strength(0.3f, 1f).sound(SoundType.GLASS));

        //Block items are registered by ModItems, the same split as 1.12 BlockReg/ItemReg

        //Flat explosive blocks: one block per explosive instead of the 1.12 metadata container (ADR 0002)
        // 1.12: BlockExplosive with Material.TNT, setHardness(2), setSoundType(SoundType.CLOTH)
        final BlockBehaviour.Properties explosive = BlockBehaviour.Properties.of()
                .mapColor(MapColor.FIRE)
                .strength(2f, 0f)
                .sound(SoundType.WOOL);
        for (Identifier ex : ExplosiveForms.enabled(ExplosiveForm.BLOCK))
        {
            final Block block = RegHelper.block(ExplosiveForm.BLOCK.idFor(ex),
                    p -> new ExplosiveBlock(ex, p), explosive);
            EXPLOSIVE_BLOCKS.put(ex, block);
        }

        // TODO(S3.08) radioactive_dirt, radioactive_stone
        // TODO(S5) launcher network, radar station, EMP tower
    }

    /** 1.12 {@code BlockConcrete} used Material.ROCK (hardness 10) and a per type explosion resistance. */
    private static BlockBehaviour.Properties concreteProperties(float explosionResistance)
    {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(10f, explosionResistance)
                .requiresCorrectToolForDrops();
    }
}
