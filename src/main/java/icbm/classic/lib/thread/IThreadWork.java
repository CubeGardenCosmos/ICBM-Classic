package icbm.classic.lib.thread;

/**
 * A unit of work executed on a {@link WorkerThread}.
 *
 * <p>1.12 handed the live level to the work and let it run world code off thread; 26.3 workers
 * never touch the world (docs/architecture/overview.md §6). The main thread captures a
 * {@link LevelSnapshot} before the work is queued and the worker only reads that snapshot. An
 * implementation MUST NOT call any method taking a {@code Level}/{@code BlockGetter} and MUST hand block
 * changes to {@link BlockEditQueue} from {@link #onCompleted()} instead of editing them itself.</p>
 *
 * Created by Dark(DarkGuardsman, Robin) on 10/8/2018.
 */
public interface IThreadWork
{
    /**
     * Called to run the work
     *
     * @param steps    - number of steps to run
     * @param snapshot - read only view of the world captured by the main thread before queuing the work
     * @return true to keep running, false to end work task
     */
    boolean doRun(int steps, LevelSnapshot snapshot);

    /**
     * Called when work is started
     *
     * @param snapshot - read only view of the world captured by the main thread before queuing the work
     */
    void onStarted(LevelSnapshot snapshot);

    /**
     * Called when the work is completed. Runs on the worker thread, so the implementation has to hand any
     * world change over to {@link BlockEditQueue}.
     */
    void onCompleted();

    /**
     * Called to get the number of steps required
     *
     * @return
     */
    default int getStepsRequired()
    {
        return 1;
    }

    /**
     * Called to get the number of steps left.
     * Only used for debug.
     *
     * @return
     */
    default int getStepsLeft()
    {
        return -1;
    }
}
