package icbm.classic.content.blocks.launcher.network;

/** A block entity that participates in a launcher network. */
public interface ILauncherComponent {

    LauncherNode getNetworkNode();

    /** Checks if the component may connect to neighboring components. */
    default boolean isValid() {
        final LauncherNode node = getNetworkNode();
        return node == null || !node.isInvalid();
    }
}
