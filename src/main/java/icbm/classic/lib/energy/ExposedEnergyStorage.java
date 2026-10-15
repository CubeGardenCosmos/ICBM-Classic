package icbm.classic.lib.energy;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/** Exposes an ICBM machine buffer as a transaction-safe, insert-only energy storage. */
public final class ExposedEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
    private final IcbmEnergyBuffer buffer;
    private final Runnable onCommit;

    public ExposedEnergyStorage(IcbmEnergyBuffer buffer, Runnable onCommit) {
        this.buffer = buffer;
        this.onCommit = onCommit;
    }

    @Override
    public boolean supportsInsertion() {
        return true;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long insert(long max, TransactionContext transaction) {
        StoragePreconditions.notNegative(max);
        if (EnergyRules.ignoresPower()) {
            return 0;
        }
        long accepted = buffer.receive(max, true);
        if (accepted > 0) {
            updateSnapshots(transaction);
            buffer.receive(accepted, false);
        }
        return accepted;
    }

    @Override
    public long extract(long max, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return buffer.getStored();
    }

    @Override
    public long getCapacity() {
        return buffer.getCapacity();
    }

    @Override
    protected Long createSnapshot() {
        return buffer.getStored();
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        buffer.setStored(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        onCommit.run();
    }
}
