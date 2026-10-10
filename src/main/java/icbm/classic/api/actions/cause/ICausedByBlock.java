package icbm.classic.api.actions.cause;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cause containing block information
 */
public interface ICausedByBlock extends IActionCause {
    Level getWorld();

    BlockPos getBlockPos();

    BlockState getBlockState();
}
