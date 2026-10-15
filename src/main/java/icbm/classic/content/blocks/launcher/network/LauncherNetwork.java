package icbm.classic.content.blocks.launcher.network;

import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.content.blocks.launcher.FiringPackage;
import icbm.classic.lib.lookup.IcbmLookups;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Connected launcher components, inventory, and firing dispatch. */
public class LauncherNetwork {

    private final HashSet<LauncherNode> components = new HashSet<>();
    private final LinkedList<LauncherEntry> launchers = new LinkedList<>();
    public final NetworkInventory inventory = new NetworkInventory(this);

    public HashSet<LauncherNode> getComponents() {
        return components;
    }

    public List<LauncherEntry> getLaunchers() {
        return launchers;
    }


    public void invalidate(LauncherNode source) {
        final HashSet<LauncherNode> oldComponents = new HashSet<>(components);
        clearData();
        oldComponents.forEach(LauncherNode::onNetworkInvalidate);
        oldComponents.stream().filter(node -> node != source).forEach(LauncherNode::connectToTiles);
    }

    public List<LauncherEntry> getLaunchers(int group) {
        return streamLaunchers(group).collect(Collectors.toList());
    }

    public Stream<LauncherEntry> streamLaunchers(int group) {
        return launchers.stream().filter(entry -> group < 0 || entry.getLauncher().getLauncherGroup() == group);
    }

    public Stream<LauncherEntry> launch(ILauncherSolution solution, IActionCause cause, boolean simulate) {
        return streamLaunchers(solution.getFiringGroup()).peek(entry -> {
            entry.setLastFiringPackage(new FiringPackage(solution.getTarget(entry.getLauncher()), cause));
            entry.setLastFiringTime(System.currentTimeMillis());
            entry.setLastFiringStatus(entry.getLauncher().launch(solution, cause, simulate));
        });
    }

    protected void clearData() {
        launchers.clear();
        components.clear();
        onNetworkUpdated();
    }

    public void mergeNetwork(LauncherNetwork otherNetwork) {
        if (otherNetwork == this) {
            return;
        }
        for (LauncherNode node : new HashSet<>(otherNetwork.components)) {
            addToNetwork(node);
        }
        otherNetwork.clearData();
    }

    public void addToNetwork(LauncherNode node) {
        if (!components.contains(node)) {
            components.add(node);
            node.setNetwork(this);
            final var level = node.getSelf().getLevel();
            if (level != null) {
                final IMissileLauncher launcher = IcbmLookups.MISSILE_LAUNCHER.find(level, node.getSelf().getBlockPos(), null);
                if (launcher != null) {
                    launchers.add(new LauncherEntry(launcher, node.getSelf(), null));
                    onNetworkUpdated();
                }
            }
        }
    }

    public void onNetworkUpdated() {
        inventory.buildInventory();
    }
}
