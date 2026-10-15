package icbm.classic.content.blocks.launcher.connector;

import icbm.classic.content.blocks.launcher.frame.TileLauncherFrame;
import icbm.classic.content.reg.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A launcher-network node at a connector block. */
public class TileLauncherConnector extends TileLauncherFrame
{
    public TileLauncherConnector(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.LAUNCHER_CONNECTOR, pos, state);
    }
}
