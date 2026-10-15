package icbm.classic.content.blocks.launcher.network;

import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

@Data
public class LauncherNode {

    private LauncherNetwork network;
    private final BlockEntity self;
    private final boolean acceptsItems;
    private boolean isInvalid = false;

    public LauncherNode(BlockEntity self, boolean acceptsItems) {
        this.self = self;
        this.acceptsItems = acceptsItems;
    }

    /** Invalidate this node and rebuild the remaining connected components. */
    public void onTileRemoved() {
        final LauncherNetwork currentNetwork = network;
        if (currentNetwork != null) {
            network = null;
            isInvalid = true;
            currentNetwork.invalidate(this);
        }
    }

    public void onNetworkInvalidate() {
        network = null;
    }

    public void connectToTiles() {
        if (self.getLevel() == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            final BlockPos nextPos = self.getBlockPos().relative(side);
            if (!self.getLevel().isLoaded(nextPos)) {
                continue;
            }
            final BlockEntity tile = self.getLevel().getBlockEntity(nextPos);
            if (!(tile instanceof ILauncherComponent component) || !component.isValid()) {
                continue;
            }
            final LauncherNode node = component.getNetworkNode();
            if (node == null || node.isInvalid()) {
                continue;
            }

            if (getNetwork() == null && node.getNetwork() != null) {
                node.getNetwork().addToNetwork(this);
            } else if (getNetwork() != null && node.getNetwork() == null) {
                getNetwork().addToNetwork(node);
            } else if (getNetwork() == null) {
                final LauncherNetwork newNetwork = new LauncherNetwork();
                newNetwork.addToNetwork(this);
                newNetwork.addToNetwork(node);
            } else if (node.getNetwork() != getNetwork()) {
                getNetwork().mergeNetwork(node.getNetwork());
            }
        }
    }

    @Override
    public int hashCode() {
        return self.getBlockPos().hashCode();
    }
}
