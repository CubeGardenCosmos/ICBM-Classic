package icbm.classic.content.blocks.emptower;

import icbm.classic.content.reg.ModBlockEntities;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockEmpTower extends BaseEntityBlock
{
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<PropertyTowerStates.EnumTowerTypes> TOWER_MODELS = PropertyTowerStates.TOWER_MODELS;

    public BlockEmpTower(Properties properties)
    {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOWER_MODELS, PropertyTowerStates.EnumTowerTypes.BASE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(TOWER_MODELS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        return open(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit)
    {
        return open(level, pos, player);
    }

    private InteractionResult open(Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final BlockEntity entity = level.getBlockEntity(pos);
        final TileEMPTower tower = entity instanceof TileEMPTower tile ? tile
                : entity instanceof TileEmpTowerFake fake ? fake.getHost() : null;
        if (tower != null) player.openMenu(tower);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return state.getValue(TOWER_MODELS) == PropertyTowerStates.EnumTowerTypes.SPIN
                ? new TileEmpTowerFake(pos, state) : new TileEMPTower(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (state.getValue(TOWER_MODELS) == PropertyTowerStates.EnumTowerTypes.SPIN) return null;
        return createTickerHelper(type, ModBlockEntities.EMP_TOWER, TileEMPTower::tick);
    }
}
