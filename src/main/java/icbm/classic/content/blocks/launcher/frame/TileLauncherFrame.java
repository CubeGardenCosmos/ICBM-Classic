package icbm.classic.content.blocks.launcher.frame;

import icbm.classic.content.blocks.launcher.network.ILauncherComponent;
import icbm.classic.content.blocks.launcher.network.LauncherNode;
import icbm.classic.content.reg.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A frame segment participating in a launcher network. */
public class TileLauncherFrame extends BlockEntity implements ILauncherComponent
{
    private final LauncherNode launcherNode = new LauncherNode(this, false);

    public TileLauncherFrame(BlockPos pos, BlockState state)
    {
        this(ModBlockEntities.LAUNCHER_FRAME, pos, state);
    }

    protected TileLauncherFrame(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    public void setRemoved()
    {
        launcherNode.onTileRemoved();
        super.setRemoved();
    }

    @Override
    public LauncherNode getNetworkNode()
    {
        return launcherNode;
    }
}

