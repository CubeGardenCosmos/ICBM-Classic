package icbm.classic.lib.energy;

import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import team.reborn.energy.api.EnergyStorage;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Drains energy around EMP activations; ICBM machines are drained internally because their public storage is insert-only. */
public final class EmpEnergyDrain {
    private EmpEnergyDrain() {}

    /** Drains at most {@code maxPerBlock} from each block within the spherical radius; the energy is discarded. */
    public static long drainAround(ServerLevel level, BlockPos center, int radius, long maxPerBlock) {
        if (radius < 0 || maxPerBlock <= 0) return 0;
        long total = 0;
        final long radiusSquared = (long) radius * radius;
        final Set<EnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((long) x * x + (long) y * y + (long) z * z > radiusSquared) continue;
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(pos)) continue;
                    final BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity == null) continue;
                    final IcbmEnergyBuffer buffer = machineBuffer(blockEntity);
                    if (buffer != null) {
                        total += buffer.drain(maxPerBlock);
                        continue;
                    }
                    long remaining = maxPerBlock;
                    for (Direction direction : Direction.values()) {
                        final EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, direction);
                        if (storage != null && storage.supportsExtraction() && seen.add(storage)) {
                            try (Transaction transaction = Transaction.openOuter()) {
                                final long extracted = storage.extract(remaining, transaction);
                                if (extracted > 0) {
                                    transaction.commit();
                                    total += extracted;
                                    remaining -= extracted;
                                }
                            }
                            if (remaining == 0) break;
                        }
                    }
                    if (remaining > 0) {
                        final EnergyStorage unsided = EnergyStorage.SIDED.find(level, pos, null);
                        if (unsided != null && unsided.supportsExtraction() && seen.add(unsided)) {
                            try (Transaction transaction = Transaction.openOuter()) {
                                final long extracted = unsided.extract(remaining, transaction);
                                if (extracted > 0) {
                                    transaction.commit();
                                    total += extracted;
                                }
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    private static IcbmEnergyBuffer machineBuffer(BlockEntity blockEntity) {
        if (blockEntity instanceof TileLauncherBase tile) return tile.energyStorage;
        if (blockEntity instanceof TileCruiseLauncher tile) return tile.energyStorage;
        if (blockEntity instanceof TileLauncherScreen tile) return tile.energyStorage;
        if (blockEntity instanceof TileRadarStation tile) return tile.energyStorage;
        if (blockEntity instanceof TileEMPTower tile) return tile.energyStorage;
        return null;
    }
}
