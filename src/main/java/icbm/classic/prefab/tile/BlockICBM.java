package icbm.classic.prefab.tile;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Generic block base of the ICBM machines, 1.12 {@code icbm.classic.prefab.tile.BlockICBM} ported onto
 * {@link BaseEntityBlock}.
 * <p>
 * Kept from 1.12: the rotation property (all six directions, default NORTH) and the placement rotation taken
 * from the horizontal facing of the placing entity, plus the 1.12 hardness/resistance of 10.
 * <p>
 * Not ported, with the 26.3 counterpart:
 * <ul>
 *     <li>the registry name, unlocalized name, material and creative tab of the 1.12 constructor are handled
 *     by {@code RegHelper}/{@code ModCreativeTabs} and the block {@code Properties} (S2),</li>
 *     <li>the metadata helpers {@code getStateFromMeta}/{@code getMetaFromState} are gone with block states
 *     (ADR 0002),</li>
 *     <li>1.12 {@code dropInventory} and the {@code breakBlock} drop: 26.3 drops the contents of a
 *     {@code Container} block entity in {@code BlockEntity#preRemoveSideEffects}, which runs before the block
 *     entity is removed ({@code LevelChunk#setBlockState}: preRemoveSideEffects, removeBlockEntity,
 *     affectNeighborsAfterRemoval). The block itself can not reach the inventory at removal time any more, so
 *     a machine that drops its inventory implements {@code preRemoveSideEffects} and calls
 *     {@link icbm.classic.lib.InventoryUtility#dropInventory(net.minecraft.world.level.Level, net.minecraft.core.BlockPos)}
 *     there.</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin).
 */
public abstract class BlockICBM extends BaseEntityBlock
{
    /**
     * 1.12 {@code BlockICBM.ROTATION_PROP} ({@code PropertyDirection.create("rotation")}, all six directions,
     * default NORTH), mapped to 26.3 like the explosive block did in S3.09.
     */
    public static final EnumProperty<Direction> ROTATION_PROP = BlockStateProperties.FACING;

    protected BlockICBM(BlockBehaviour.Properties properties)
    {
        //1.12: blockHardness = 10f; blockResistance = 10f
        super(properties.strength(10.0F, 10.0F));
        //1.12 PropertyDirection defaults to NORTH, BlockStateProperties.FACING also starts at NORTH; spelled
        //out so the default state of an unplaced machine stays NORTH as in 1.12
        this.registerDefaultState(this.defaultBlockState().setValue(ROTATION_PROP, Direction.NORTH));
    }

    /** 1.12 {@code createBlockState} added {@code ROTATION_PROP} */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(ROTATION_PROP);
    }

    /**
     * 1.12 {@code getStateForPlacement} started from the default state and used the horizontal facing of the
     * placing entity ({@code placer.getHorizontalFacing()}, 26.3 {@link net.minecraft.world.entity.Entity#getDirection()}).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final Player player = context.getPlayer();
        return this.defaultBlockState().setValue(ROTATION_PROP, player != null ? player.getDirection() : Direction.NORTH);
    }
}
