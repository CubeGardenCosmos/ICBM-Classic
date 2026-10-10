package icbm.classic.lib.thread;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Read only copy of the blocks a worker thread is allowed to look at.
 *
 * <p>1.12 explosion threads read the live world, which is a data race against the server thread
 * (docs/architecture/overview.md §6). 26.3 splits the work: the server thread calls
 * {@link #capture(ServerLevel, BlockPos, int)} to copy the chunk sections around the blast, hands the
 * snapshot to a {@link WorkerThread} and applies the resulting edits later from {@link BlockEditQueue}.</p>
 *
 * <p>The snapshot is never mutated once {@code capture} returned, so it is safe to read from any thread.
 * Chunks that are not loaded are treated as air, matching 1.12 skipping positions outside loaded chunks.</p>
 */
public final class LevelSnapshot
{
    /** key = SectionPos.asLong(sectionX, sectionY, sectionZ), value = copy of that section's block container */
    private final Long2ObjectMap<PalettedContainer<BlockState>> sections;
    /** Lowest block Y of the captured sections; empty snapshots use a range nothing can be inside of */
    private final int minY;
    /** Highest block Y of the captured sections */
    private final int maxY;

    private LevelSnapshot(Long2ObjectMap<PalettedContainer<BlockState>> sections, int minY, int maxY)
    {
        this.sections = sections;
        this.minY = minY;
        this.maxY = maxY;
    }

    /**
     * Copies every chunk section of the loaded chunks around the given center.
     *
     * <p><b>Must only be called from the server thread</b>, it reads the live chunk data.</p>
     *
     * @param level  server level to copy from
     * @param center center of the area the worker is allowed to see
     * @param radius block radius around the center; chunks touched by {@code center +/- radius} are copied
     * @return immutable snapshot, never null
     */
    public static LevelSnapshot capture(ServerLevel level, BlockPos center, int radius)
    {
        final Long2ObjectMap<PalettedContainer<BlockState>> sections = new Long2ObjectOpenHashMap<>();

        final int minChunkX = (center.getX() - radius) >> 4;
        final int maxChunkX = (center.getX() + radius) >> 4;
        final int minChunkZ = (center.getZ() - radius) >> 4;
        final int maxChunkZ = (center.getZ() + radius) >> 4;

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
            {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null)
                {
                    //Unloaded chunk, treated as air (1.12 skipped positions outside of loaded chunks)
                    continue;
                }

                final LevelChunkSection[] chunkSections = chunk.getSections();
                for (int index = 0; index < chunkSections.length; index++)
                {
                    final LevelChunkSection section = chunkSections[index];
                    if (section == null || section.hasOnlyAir())
                    {
                        continue;
                    }

                    final int sectionY = level.getSectionYFromSectionIndex(index);
                    sections.put(SectionPos.asLong(chunkX, sectionY, chunkZ), section.getStates().copy());
                    minY = Math.min(minY, SectionPos.sectionToBlockCoord(sectionY));
                    maxY = Math.max(maxY, SectionPos.sectionToBlockCoord(sectionY, 15));
                }
            }
        }

        if (sections.isEmpty())
        {
            return new LevelSnapshot(sections, 0, -1);
        }
        return new LevelSnapshot(sections, minY, maxY);
    }

    /**
     * Reads a block from the snapshot. <b>Safe to call from a worker thread</b>, the snapshot is immutable.
     *
     * @return the captured state, or {@code Blocks.AIR.defaultBlockState()} when the position was not copied
     * (unloaded chunk, out of range or a section that only contains air)
     */
    public BlockState getBlockState(int x, int y, int z)
    {
        if (y < minY || y > maxY)
        {
            return Blocks.AIR.defaultBlockState();
        }

        final PalettedContainer<BlockState> container = sections.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        if (container == null)
        {
            return Blocks.AIR.defaultBlockState();
        }
        return container.get(x & 15, y & 15, z & 15);
    }

    /** See {@link #getBlockState(int, int, int)} */
    public BlockState getBlockState(BlockPos pos)
    {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * @return number of captured chunk sections; 0 when nothing around the center was loaded
     */
    public int getSectionCount()
    {
        return sections.size();
    }
}
