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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Drains energy around EMP activations; ICBM machines are drained internally because their public storage is insert-only. */
public final class EmpEnergyDrain {
    /** Ascending x, then y, then z; the order the historical cube scan visited positions in. */
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    private EmpEnergyDrain() {}

    /** Drains at most {@code maxPerBlock} from each block within the spherical radius; the energy is discarded. */
    public static long drainAround(ServerLevel level, BlockPos center, int radius, long maxPerBlock) {
        if (radius < 0 || maxPerBlock <= 0) return 0;
        final List<BlockPos> targets = collectLoadedBlockEntities(level, center, radius);
        if (targets.isEmpty()) return 0;
        // Keep the historical traversal order (ascending x, then y, then z) so the drain sequence is unchanged.
        // Note: BlockPos' natural order is y, z, x, so the comparator has to be explicit.
        targets.sort(POSITION_ORDER);

        long total = 0;
        final Set<EnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : targets) {
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
        return total;
    }

    /**
     * Collects the positions of every block entity inside the sphere that lives in an already loaded chunk.
     * <p>
     * The previous implementation scanned every position of the bounding cube and only acted on positions that
     * held a block entity, so enumerating the block entities of the loaded chunks yields exactly the same
     * candidates while dropping the O(radius^3) coordinate walk. Chunks are looked up in a non-loading way
     * ({@code requireChunk = false}, the same condition {@code Level#isLoaded} uses), so no chunk is ever loaded.
     */
    private static List<BlockPos> collectLoadedBlockEntities(ServerLevel level, BlockPos center, int radius) {
        final long radiusSquared = (long) radius * radius;
        final int centerX = center.getX();
        final int centerY = center.getY();
        final int centerZ = center.getZ();
        final List<BlockPos> targets = new ArrayList<>();
        for (int chunkX = (centerX - radius) >> 4, maxChunkX = (centerX + radius) >> 4; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = (centerZ - radius) >> 4, maxChunkZ = (centerZ + radius) >> 4; chunkZ <= maxChunkZ; chunkZ++) {
                if (!(level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) instanceof LevelChunk chunk)) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    final BlockPos pos = blockEntity.getBlockPos();
                    final long dx = pos.getX() - centerX;
                    final long dy = pos.getY() - centerY;
                    final long dz = pos.getZ() - centerZ;
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        targets.add(pos.immutable());
                    }
                }
            }
        }
        return targets;
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
