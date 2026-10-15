package icbm.classic.content.blocks.radarstation;

import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.prefab.tile.BlockICBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockRadarStation extends BlockICBM
{
    public static final BooleanProperty REDSTONE_PROPERTY = BooleanProperty.create("redstone");
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<EnumRadarState> RADAR_STATE = PropertyRadarState.RADAR_STATE;

    public BlockRadarStation(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)
    {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(REDSTONE_PROPERTY, false).setValue(RADAR_STATE, EnumRadarState.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(REDSTONE_PROPERTY, RADAR_STATE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        return openOrToggle(level, pos, player, ItemStack.EMPTY);
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit)
    {
        return openOrToggle(level, pos, player, stack);
    }

    private InteractionResult openOrToggle(Level level, BlockPos pos, Player player, ItemStack stack)
    {
        if (!(level.getBlockEntity(pos) instanceof TileRadarStation tile)) return InteractionResult.PASS;
        if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.REDSTONE))
        {
            if (!level.isClientSide())
            {
                tile.setOutputRedstone(!tile.isOutputRedstone());
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        tile.isOutputRedstone() ? "icbmclassic.message.radar.redstone.on" : "icbmclassic.message.radar.redstone.off"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) player.openMenu(tile);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isSignalSource(BlockState state)
    {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return level.getBlockEntity(pos) instanceof TileRadarStation tile ? tile.getStrongRedstonePower(direction) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return getSignal(state, level, pos, direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileRadarStation(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return type == ModBlockEntities.RADAR_STATION
                ? (world, pos, blockState, entity) -> TileRadarStation.tick(world, pos, blockState, entity)
                : null;
    }
}
