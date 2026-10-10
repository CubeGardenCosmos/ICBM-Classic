package icbm.classic.content.reg;

import icbm.classic.content.blocks.BlockGlassButton;
import icbm.classic.content.blocks.BlockGlassPressurePlate;
import icbm.classic.content.blocks.BlockReinforcedGlass;
import icbm.classic.content.blocks.BlockSpikes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks
{
    //=================== S2.03 simple blocks
    public static Block CONCRETE;
    public static Block COMPACT_CONCRETE;
    public static Block REINFORCED_CONCRETE;
    public static Block REINFORCED_GLASS;
    public static Block SPIKES;
    public static Block POISON_SPIKES;
    public static Block FIRE_SPIKES;
    public static Block GLASS_BUTTON;
    public static Block GLASS_PRESSURE_PLATE;

    private ModBlocks() {}

    public static void register()
    {
        CONCRETE = RegHelper.block("concrete", Block::new, concreteProperties(28f));
        COMPACT_CONCRETE = RegHelper.block("compact_concrete", Block::new, concreteProperties(280f));
        REINFORCED_CONCRETE = RegHelper.block("reinforced_concrete", Block::new, concreteProperties(2800f));

        REINFORCED_GLASS = RegHelper.block("reinforced_glass", BlockReinforcedGlass::new,
                BlockBehaviour.Properties.of().strength(10f, 48f).noOcclusion());

        final BlockBehaviour.Properties spikes = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(1f)
                .noCollision()
                .noOcclusion();
        SPIKES = RegHelper.block("spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.NORMAL, p), spikes);
        POISON_SPIKES = RegHelper.block("poison_spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.POISON, p), spikes);
        FIRE_SPIKES = RegHelper.block("fire_spikes", p -> new BlockSpikes(BlockSpikes.SpikeType.FIRE, p), spikes);

        GLASS_BUTTON = RegHelper.block("glass_button", BlockGlassButton::new,
                BlockBehaviour.Properties.of().noCollision().strength(0.5f).sound(SoundType.GLASS));

        GLASS_PRESSURE_PLATE = RegHelper.block("glass_pressure_plate", BlockGlassPressurePlate::new,
                BlockBehaviour.Properties.of().noCollision().strength(0.3f, 1f).sound(SoundType.GLASS));
    }

    private static BlockBehaviour.Properties concreteProperties(float explosionResistance)
    {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(10f, explosionResistance)
                .requiresCorrectToolForDrops();
    }
}
