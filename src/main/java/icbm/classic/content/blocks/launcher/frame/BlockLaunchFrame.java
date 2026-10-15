package icbm.classic.content.blocks.launcher.frame;

import icbm.classic.content.blocks.launcher.connector.TileLauncherConnector;
import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherNetwork;
import icbm.classic.prefab.tile.BlockICBM;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Vertical launch-column frame; its state selects the corresponding frame model. */
public class BlockLaunchFrame extends BlockICBM
{
    public static final EnumProperty<EnumFrameState> FRAME_STATE = PropertyFrameState.FRAME_STATE;

    public BlockLaunchFrame(BlockBehaviour.Properties properties)
    {
        super(properties.noOcclusion());
        registerDefaultState(defaultBlockState().setValue(FRAME_STATE, EnumFrameState.MIDDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(FRAME_STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return frameState(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation,
                                   boolean movedByPiston)
    {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        refreshFrameState(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
    {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refreshFrameState(state, level, pos);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ILauncherComponent component)
        {
            component.getNetworkNode().connectToTiles();
        }
    }

    private void refreshFrameState(BlockState state, Level level, BlockPos pos)
    {
        if (!level.isClientSide())
        {
            final BlockState updated = frameState(state, level, pos);
            if (updated != state)
            {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    private BlockState frameState(BlockState state, Level level, BlockPos pos)
    {
        final boolean above = isFrameConnection(level, pos.relative(Direction.UP));
        final boolean below = isFrameConnection(level, pos.relative(Direction.DOWN));
        final EnumFrameState frame = above && below ? EnumFrameState.MIDDLE
                : below ? EnumFrameState.TOP
                : above ? EnumFrameState.BOTTOM
                : EnumFrameState.MIDDLE;
        return state.setValue(FRAME_STATE, frame);
    }

    private boolean isFrameConnection(Level level, BlockPos pos)
    {
        if (level.getBlockState(pos).is(this))
        {
            return true;
        }
        final BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof ILauncherComponent && !(entity instanceof TileLauncherConnector);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!stack.is(Items.STONE_AXE))
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide())
        {
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TileLauncherFrame frame)
            {
                final LauncherNetwork network = frame.getNetworkNode().getNetwork();
                player.sendSystemMessage(Component.translatable("icbmclassic.launcher.debug.network",
                        String.valueOf(network)));
                player.sendSystemMessage(Component.translatable("icbmclassic.launcher.debug.launchers",
                        network == null ? 0 : network.getLaunchers().size()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileLauncherFrame(pos, state);
    }
}
