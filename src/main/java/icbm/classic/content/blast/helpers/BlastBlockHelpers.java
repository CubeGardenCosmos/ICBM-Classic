package icbm.classic.content.blast.helpers;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created by Dark(DarkGuardsman, Robin) on 5/23/2020.
 */
public final class BlastBlockHelpers
{
    private BlastBlockHelpers()
    {
        //Private and empty to prevent creation
    }

    /**
     * Checks if a block state is a fluid
     * <p>
     * 1.12 checked BlockLiquid || IFluidBlock. 26.3 has no IFluidBlock (forbidden in the port) and
     * LiquidBlock covers both the vanilla and the modded flowing-fluid blocks.
     *
     * @param blockState to check
     * @return true if the block state is a fluid
     */
    public static boolean isFluid(BlockState blockState)
    {
        return blockState.getBlock() instanceof LiquidBlock;
    }

    /**
     * Checks if the blocks state is a flowing fluid
     *
     * @param blockState to check
     * @return true if the block state is a flowing fluid
     */
    public static boolean isFlowingWater(BlockState blockState)
    {
        return blockState.getBlock() instanceof LiquidBlock && blockState.getValue(LiquidBlock.LEVEL) < 7;
    }
}
