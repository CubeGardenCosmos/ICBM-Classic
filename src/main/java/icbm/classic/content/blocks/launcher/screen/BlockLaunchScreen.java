package icbm.classic.content.blocks.launcher.screen;

import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.prefab.tile.BlockICBM;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
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

/** Launch screen block; interaction opens its machine menu. */
public class BlockLaunchScreen extends BlockICBM
{
    public BlockLaunchScreen(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        return openMenu(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit)
    {
        return openMenu(level, pos, player);
    }

    private static InteractionResult openMenu(Level level, BlockPos pos, Player player)
    {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileLauncherScreen screen)
        {
            if (!level.isClientSide())
            {
                player.openMenu(screen);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TileLauncherScreen(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return type == ModBlockEntities.LAUNCHER_SCREEN
                ? (world, pos, blockState, blockEntity) -> TileLauncherScreen.tick(world, pos, blockState, (TileLauncherScreen) blockEntity)
                : null;
    }
}
