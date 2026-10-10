package icbm.classic.api.actions.cause;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Cause containing block information
 */
public interface ICausedByBlock extends IActionCause {
    World getWorld();

    BlockPos getBlockPos();

    IBlockState getBlockState();
}
