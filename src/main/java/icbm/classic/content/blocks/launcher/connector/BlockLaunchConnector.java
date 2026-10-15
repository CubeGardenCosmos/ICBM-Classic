package icbm.classic.content.blocks.launcher.connector;

import icbm.classic.content.blocks.launcher.frame.TileLauncherFrame;
import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherNetwork;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A launcher network connector, with model flags for adjacent inventories/network nodes. */
public class BlockLaunchConnector extends BaseEntityBlock
{
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    public BlockLaunchConnector(BlockBehaviour.Properties properties)
    {
        super(properties.strength(10.0F, 10.0F).noOcclusion());
        registerDefaultState(defaultBlockState()
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return connectorState(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   @Nullable Orientation orientation, boolean movedByPiston)
    {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        refreshConnections(state, level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
    {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refreshConnections(state, level, pos);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ILauncherComponent component)
        {
            component.getNetworkNode().connectToTiles();
        }
    }

    private void refreshConnections(BlockState state, Level level, BlockPos pos)
    {
        if (!level.isClientSide())
        {
            final BlockState updated = connectorState(state, level, pos);
            if (updated != state)
            {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    private BlockState connectorState(BlockState state, Level level, BlockPos pos)
    {
        return state
                .setValue(UP, isConnection(level, pos, Direction.UP))
                .setValue(DOWN, isConnection(level, pos, Direction.DOWN))
                .setValue(NORTH, isConnection(level, pos, Direction.NORTH))
                .setValue(EAST, isConnection(level, pos, Direction.EAST))
                .setValue(SOUTH, isConnection(level, pos, Direction.SOUTH))
                .setValue(WEST, isConnection(level, pos, Direction.WEST));
    }

    private boolean isConnection(Level level, BlockPos selfPos, Direction side)
    {
        final BlockPos neighborPos = selfPos.relative(side);
        final BlockEntity entity = level.getBlockEntity(neighborPos);
        if (entity instanceof ILauncherComponent)
        {
            return true;
        }
        // Fabric Transfer API is the replacement for the old sided item-handler capability.
        return ItemStorage.SIDED.find(level, neighborPos, side.getOpposite()) != null;
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
        return new TileLauncherConnector(pos, state);
    }
}
