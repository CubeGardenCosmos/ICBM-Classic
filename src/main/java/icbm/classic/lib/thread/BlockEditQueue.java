package icbm.classic.lib.thread;

import icbm.classic.config.ConfigThread;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Applies block edits produced by worker threads on the server thread, with a per tick budget.
 *
 * <p>Replaces the 1.12 {@code lib/transform/BlockEditHandler} (which used {@code addScheduledTask} and a
 * tick time limit). A worker thread can not touch the world (docs/architecture/overview.md §6), so it calls
 * {@link #submit(ResourceKey, List, Consumer, Runnable)} when it is done and the server thread drains the
 * queue from {@code ServerTickEvents.END_LEVEL_TICK}.</p>
 *
 * <p>Every tick at most {@link ConfigThread#BLOCKS_PER_TICK} edits are applied, the budget is shared by all
 * queued jobs in submission order so a single large job cannot consume unbounded main thread time. When the
 * last edit of a job was applied its {@code onComplete} callback runs right away, still inside that tick.</p>
 */
public final class BlockEditQueue
{
    /** Dimension to pending jobs; jobs are applied in submission order, one queue per level */
    private static final Map<ResourceKey<Level>, Queue<Job>> QUEUES = new ConcurrentHashMap<>();

    private BlockEditQueue()
    {
    }

    /**
     * Queues a batch of edits. Thread safe, call from the worker thread that produced the edits; the action
     * and the completion callback always run on the server thread.
     *
     * @param dim        dimension the edits belong to, see {@code level.dimension()}
     * @param edits      positions to edit, read once on submit
     * @param action     applied per position on the server thread
     * @param onComplete run on the server thread after the last edit, may be null
     */
    public static void submit(ResourceKey<Level> dim, List<BlockPos> edits, Consumer<BlockPos> action, Runnable onComplete)
    {
        QUEUES.computeIfAbsent(dim, key -> new ConcurrentLinkedQueue<>()).add(new Job(edits, action, onComplete));
    }

    /**
     * Applies up to {@link ConfigThread#BLOCKS_PER_TICK} edits of the given level. Called from
     * {@code ServerTickEvents.END_LEVEL_TICK}, so this is the server thread.
     */
    public static void tick(ServerLevel level)
    {
        final Queue<Job> queue = QUEUES.get(level.dimension());
        if (queue == null)
        {
            return;
        }

        int budget = Math.max(0, ConfigThread.BLOCKS_PER_TICK);
        while (budget > 0)
        {
            final Job job = queue.peek();
            if (job == null)
            {
                return;
            }

            final BlockPos pos = job.edits.poll();
            if (pos == null)
            {
                //Nothing to apply, only possible for a job submitted with an empty edit list
                queue.poll();
                job.finish();
                continue;
            }

            job.action.accept(pos);
            budget--;

            if (job.edits.isEmpty())
            {
                queue.poll();
                job.finish();
            }
        }
    }

    /** Drops the pending edits of a level, used when the level unloads. Callbacks are not run. */
    public static void clear(ResourceKey<Level> dim)
    {
        QUEUES.remove(dim);
    }

    /** Drops the pending edits of every level, used when the server stops. Callbacks are not run. */
    public static void clearAll()
    {
        QUEUES.clear();
    }

    /** One batch of edits queued by a worker thread; only the server thread touches it after submit */
    private static final class Job
    {
        private final Queue<BlockPos> edits;
        private final Consumer<BlockPos> action;
        private final Runnable onComplete;

        private Job(List<BlockPos> edits, Consumer<BlockPos> action, Runnable onComplete)
        {
            this.edits = new ConcurrentLinkedQueue<>(edits);
            this.action = action;
            this.onComplete = onComplete;
        }

        private void finish()
        {
            if (onComplete != null)
            {
                onComplete.run();
            }
        }
    }
}
