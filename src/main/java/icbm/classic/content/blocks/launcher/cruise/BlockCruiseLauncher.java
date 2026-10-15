package icbm.classic.content.blocks.launcher.cruise;

import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.prefab.tile.BlockICBM;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Cruise missile launcher block. */
public class BlockCruiseLauncher extends BlockICBM
{
    public BlockCruiseLauncher(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)
    {
        super(properties.noOcclusion());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof TileCruiseLauncher launcher)
        {
            if (!level.isClientSide())
            {
                player.openMenu(launcher);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit)
    {
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileCruiseLauncher(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return type == ModBlockEntities.CRUISE_LAUNCHER
                ? (world, pos, blockState, entity) -> TileCruiseLauncher.tick(world, pos, blockState, entity)
                : null;
    }

    @Override
    public boolean isSignalSource(BlockState state)
    {
        return true;
    }
}
