package icbm.classic.lib.energy;

import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.network.LauncherNetwork;
import icbm.classic.content.blocks.launcher.network.LauncherNode;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/** Insert-only storage that distributes incoming energy across a launcher network. */
public final class LauncherNetworkEnergy implements EnergyStorage {
    private final LauncherNetwork network;

    public LauncherNetworkEnergy(LauncherNetwork network) {
        this.network = network;
    }

    @Override
    public long insert(long max, TransactionContext transaction) {
        if (max <= 0) return 0;

        int remainingNodes = 0;
        for (LauncherNode node : network.getComponents()) {
            if (isEnergyNode(node)) remainingNodes++;
        }

        long inserted = 0;
        for (LauncherNode node : network.getComponents()) {
            if (!isEnergyNode(node)) continue;

            final long remaining = max - inserted;
            final long share = Math.min(remaining, remaining / remainingNodes + 1);
            inserted += exposedStorage(node).insert(share, transaction);
            remainingNodes--;
            if (inserted >= max) break;
        }
        return inserted;
    }

    @Override
    public long extract(long max, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        long amount = 0;
        for (LauncherNode node : network.getComponents()) {
            if (isEnergyNode(node)) amount += exposedStorage(node).getAmount();
        }
        return amount;
    }

    @Override
    public long getCapacity() {
        long capacity = 0;
        for (LauncherNode node : network.getComponents()) {
            if (isEnergyNode(node)) capacity += exposedStorage(node).getCapacity();
        }
        return capacity;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    private static boolean isEnergyNode(LauncherNode node) {
        return node.getSelf() instanceof TileLauncherBase || node.getSelf() instanceof TileCruiseLauncher;
    }

    private static ExposedEnergyStorage exposedStorage(LauncherNode node) {
        if (node.getSelf() instanceof TileLauncherBase base) return base.getExposedEnergy();
        return ((TileCruiseLauncher) node.getSelf()).getExposedEnergy();
    }
}
