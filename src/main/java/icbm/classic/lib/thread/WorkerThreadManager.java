package icbm.classic.lib.thread;

import icbm.classic.ICBMClassic;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Owns the {@link WorkerThread}s and spreads work over the least loaded one.
 *
 * <p>1.12 created the pool in {@code serverStarting} and killed it in {@code serverStopping} with
 * {@code Thread.stop()}. 26.3 creates it from {@code ICBMClassic.onInitialize()} and stops it from
 * {@code ServerLifecycleEvents.SERVER_STOPPING}, see {@link #killThreads()} for the shutdown rules.</p>
 *
 * Created by Dark(DarkGuardsman, Robin) on 10/8/2018.
 */
public class WorkerThreadManager
{
    /** Global instance, created by the entry point. Null until {@code ICBMClassic.onInitialize()} ran. */
    public static WorkerThreadManager INSTANCE;

    /** Time the whole shutdown may spend waiting for the worker threads to exit */
    private static final long SHUTDOWN_TIMEOUT_MS = 5000;

    private final List<WorkerThread> threads = new ArrayList<>();

    public WorkerThreadManager(int thread_count)
    {
        for (int i = 0; i < thread_count; i++)
        {
            threads.add(new WorkerThread(i));
        }
    }

    /**
     * Queues the work on the least loaded thread.
     *
     * @param workerTask task to run on a worker thread
     * @param snapshot   read only world view the task is allowed to read
     */
    public void addWork(IThreadWork workerTask, LevelSnapshot snapshot)
    {
        WorkerThread thread = threads.get(0);
        int workCount = threads.get(0).getWorkCount();
        for (int i = 1; i < threads.size(); i++)
        {
            WorkerThread t = threads.get(i);
            int w = t.getWorkCount();
            if (w < workCount)
            {
                thread = t;
                workCount = w;
            }
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment())
        {
            ICBMClassic.logger().info("Adding work task '" + workCount + "' to thread '" + thread + "'");
        }
        if (!thread.doRun)
        {
            //The pool was stopped, queueing would silently leak the work (and the blast waiting on it)
            ICBMClassic.logger().warn("Dropping work task '" + workerTask + "' on stopped thread '" + thread + "'");
            return;
        }
        thread.addWork(workerTask, snapshot);
    }

    public void startThreads()
    {
        ICBMClassic.logger().info("Starting threads");
        threads.forEach(thread -> thread.start());
    }

    /**
     * Stops every worker thread and waits at most {@link #SHUTDOWN_TIMEOUT_MS} in total for them to exit.
     * Tasks that are still queued (or in flight) when a thread stops are dropped and reported with a warning.
     */
    public void killThreads()
    {
        ICBMClassic.logger().info("Stopping threads");

        //Signal every thread first so a slow task cannot eat the timeout of the threads behind it
        threads.forEach(WorkerThread::signalStop);

        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SHUTDOWN_TIMEOUT_MS);
        for (WorkerThread thread : threads)
        {
            final long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMs <= 0)
            {
                break;
            }
            thread.stopTasks(remainingMs);
        }

        int dropped = 0;
        for (WorkerThread thread : threads)
        {
            final int pending = thread.getWorkCount();
            dropped += pending;
            if (thread.isAlive())
            {
                ICBMClassic.logger().warn("Worker thread '{}' did not stop within {}s, dropping its work",
                        thread.getName(), SHUTDOWN_TIMEOUT_MS / 1000);
            }
        }
        if (dropped > 0)
        {
            ICBMClassic.logger().warn("Dropped {} unfinished work task(s) while stopping the worker threads", dropped);
        }
    }
}
