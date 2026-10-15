package icbm.classic.content.blocks.launcher.base;

import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.prefab.tile.BlockICBM;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Base block of the launcher network. */
public class BlockLauncherBase extends BlockICBM
{
    public BlockLauncherBase(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
    {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide())
        {
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ILauncherComponent component)
            {
                component.getNetworkNode().connectToTiles();
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit)
    {
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof TileLauncherBase base && base.tryInsertMissile(player, hand, stack))
        {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit)
    {
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof TileLauncherBase base)
        {
            if (base.tryInsertMissile(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY))
            {
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide())
            {
                player.openMenu(base);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileLauncherBase(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type)
    {
        return type == ModBlockEntities.LAUNCHER_BASE
                ? (world, pos, blockState, blockEntity) -> TileLauncherBase.tick(world, pos, blockState, blockEntity)
                : null;
    }
}
