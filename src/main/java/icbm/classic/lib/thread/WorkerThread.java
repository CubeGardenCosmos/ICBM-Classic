package icbm.classic.lib.thread;

import icbm.classic.ICBMClassic;
import icbm.classic.config.ConfigDebug;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Daemon thread that runs {@link IThreadWork} against a {@link LevelSnapshot}.
 *
 * <p>1.12 stopped these threads with {@code Thread.stop()}, which was removed from the JDK. Shutdown now
 * sets {@link #doRun} to false, interrupts the thread (to break its idle sleep) and waits for it to leave
 * its run loop, see {@link #stopTasks(long)} and {@link WorkerThreadManager#killThreads()}.</p>
 *
 * Created by Dark(DarkGuardsman, Robin) on 10/8/2018.
 */
public class WorkerThread extends Thread
{
    /** Time {@link #stopTasks()} waits for the run loop to exit */
    public static final long STOP_TIMEOUT_MS = 5000;

    /** Set to false to let the run loop exit; volatile so the stopping thread sees the change */
    public volatile boolean doRun = true;

    private IThreadWork activateTask;
    private LevelSnapshot activateSnapshot;
    private final ConcurrentLinkedQueue<WorkEntry> queue = new ConcurrentLinkedQueue<>();
    /** Queued but not started tasks; atomic because the worker decrements it while the main thread reads it */
    private final AtomicInteger workCount = new AtomicInteger();

    public WorkerThread(int index)
    {
        super(null, null, "ICBM-Classic-WorkerThread-" + index, 0);
        this.setPriority(Thread.MIN_PRIORITY); //We don't care how fast this runs
        this.setDaemon(true); //Fix for threads still running when MC closes
    }

    @Override
    public void interrupt()
    {
        if (ConfigDebug.DEBUG_THREADS)
        {
            //1.12 logged this as an error, but interrupting is now the normal shutdown path
            ICBMClassic.logger().debug(toString() + " was interrupted while running tasks",
                    new RuntimeException("Trace"));
        }
        super.interrupt();
    }

    @Override
    public final void run()
    {
        while (doRun)
        {
            //IF task is set run
            if (activateTask != null)
            {
                //Run step
                if (!activateTask.doRun(1, activateSnapshot))
                {
                    //Complete
                    activateTask.onCompleted();
                    ICBMClassic.logger().debug(toString() + " complete work task " + activateTask);

                    //Clear
                    activateTask = null;
                    activateSnapshot = null;
                }
            }

            //Get next task if no task is set
            if (activateTask == null)
            {
                nextTask();
            }

            //If no tasks, sleep to give time to main thread
            if (activateTask == null)
            {
                try
                {
                    sleep(100);
                }
                catch (InterruptedException e)
                {
                    //stopTasks() interrupts the thread to break this sleep, treating it as an error is wrong
                    if (doRun)
                    {
                        ICBMClassic.logger().error(toString() + " was interrupted while sleeping",
                                e);
                    }
                }
            }
        }
    }

    protected void nextTask()
    {
        final WorkEntry entry = queue.poll();
        if (entry != null)
        {
            activateTask = entry.work();
            activateSnapshot = entry.snapshot();
            workCount.decrementAndGet();
            activateTask.onStarted(activateSnapshot);
            if (FabricLoader.getInstance().isDevelopmentEnvironment())
            {
                ICBMClassic.logger().info(toString() + " starting work task " + activateTask);
            }
        }
    }

    protected IThreadWork getCurrentTask()
    {
        return activateTask;
    }

    public void addWork(IThreadWork work, LevelSnapshot snapshot)
    {
        queue.add(new WorkEntry(work, snapshot));
        workCount.incrementAndGet();
    }

    public int getWorkCount()
    {
        return workCount.get();
    }

    /**
     * Sets the stop flag and interrupts the thread to break its idle sleep. Use {@link #stopTasks()} when
     * the caller also has to wait for the run loop to exit.
     */
    public void signalStop()
    {
        doRun = false;
        interrupt();
    }

    /**
     * Signals the thread to stop and waits up to the given time for the run loop to exit. Tasks still
     * queued when the thread stopped are dropped, {@link WorkerThreadManager#killThreads()} reports them.
     *
     * @param timeoutMs time to wait for the thread to exit, must be positive
     */
    public void stopTasks(long timeoutMs)
    {
        signalStop();
        try
        {
            join(timeoutMs);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            ICBMClassic.logger().warn(toString() + " was interrupted while waiting for the worker to stop");
        }
    }

    /** Calls {@link #stopTasks(long)} with {@link #STOP_TIMEOUT_MS} */
    public void stopTasks()
    {
        stopTasks(STOP_TIMEOUT_MS);
    }

    /** Task plus the world snapshot it is allowed to read */
    private record WorkEntry(IThreadWork work, LevelSnapshot snapshot)
    {
    }
}
