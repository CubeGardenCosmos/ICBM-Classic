package icbm.classic.content.blast.threaded;

import icbm.classic.content.blast.Blast;
import icbm.classic.lib.explosive.ThreadWorkBlast;
import icbm.classic.lib.thread.BlockEditQueue;
import icbm.classic.lib.thread.IThreadWork;
import icbm.classic.lib.thread.LevelSnapshot;
import icbm.classic.lib.thread.WorkerThreadManager;
import icbm.classic.lib.transform.PosDistanceSorter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Blast that does its block math on a worker thread.
 * <p>
 * 1.12 started a {@code ThreadExplosion} per blast (a {@link Thread} subclass) that read the world directly
 * and pushed results into {@code Blast#threadResults}. 26.3 forbids touching the world off the server thread,
 * so the port uses the S1.09 framework: {@link LevelSnapshot} (captured on the server thread) is read by the
 * worker, and the finished list is applied back on the server thread through {@link BlockEditQueue}, which
 * spreads the edits over ticks using the configured per tick budget.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 10/8/2018.
 */
public abstract class BlastThreaded extends Blast
{
    private boolean hasThreadStarted = false;

    /** Worker task computing the edits, 1.12: {@code Blast#thread} ({@code ThreadExplosion}) */
    protected IThreadWork workerTask;

    /** Positions the worker found, 1.12: {@code Blast#threadResults} */
    protected List<BlockPos> threadResults = new ArrayList<>();

    /** True once the worker finished and the results are available on the server thread */
    protected boolean threadComplete = false;

    public BlastThreaded()
    {
    }

    /**
     * Builds the worker task. Kept as a hook so subclasses can swap in their own {@link IThreadWork}.
     *
     * @return task to hand to the {@link WorkerThreadManager}
     */
    protected IThreadWork getWorkerTask()
    {
        return new ThreadWorkBlast(this.toString(), this::doRun, this::onWorkerThreadComplete);
    }

    /**
     * Called by the worker for each collection pass. Only the given snapshot may be read, the world can not be
     * touched from here.
     *
     * @param loops - current loop count
     * @param edits - acceptor for positions the blast wants to edit
     * @return true to run another iteration
     */
    public abstract boolean doRun(int loops, Consumer<BlockPos> edits);

    /**
     * Builds a sorter to sort all of the blocks post thread run
     *
     * @return sorter to use
     */
    protected Comparator<BlockPos> buildSorter()
    {
        return new PosDistanceSorter(location, false, PosDistanceSorter.Sort.SQ);
    }

    /**
     * Radius of the snapshot the worker reads. 1.12 passed the radius into each {@code ThreadExplosion}
     * constructor, the port asks the blast instead.
     *
     * @return radius in blocks
     */
    protected int getThreadRadius()
    {
        return (int) Math.ceil(getBlastRadius());
    }

    /**
     * Called on the server thread once the queued edits finished, 1.12 {@code onPostThreadJoinWorld}.
     */
    protected void onPostThreadJoinWorld()
    {
        doExplode(-1);
        onBlastCompleted();
    }

    /**
     * Called when the worker completes, is still inside of the worker. The edits are handed to the server
     * thread, which then calls {@link #onWorkerResultsReady(List)}.
     *
     * @param edits positions the worker collected
     */
    protected void onWorkerThreadComplete(List<BlockPos> edits)
    {
        //Sort distance
        edits.sort(buildSorter());

        //Hand the sorted result back to the server thread, the worker must not touch the world
        final List<BlockPos> sorted = new ArrayList<>(edits);
        if (world() instanceof ServerLevel serverLevel)
        {
            serverLevel.getServer().execute(() -> onWorkerResultsReady(sorted));
        }
    }

    /**
     * Called on the server thread with the finished worker result.
     *
     * @param edits sorted positions the worker collected
     */
    protected void onWorkerResultsReady(List<BlockPos> edits)
    {
        this.threadResults = edits;
        this.threadComplete = true;

        if (skipQueue())
        {
            edits.forEach(this::destroyBlock);
            onPostThreadJoinWorld();
        }
        else
        {
            //Queue edits, the queue applies them over ticks inside the configured budget
            BlockEditQueue.submit(level().dimension(), edits, this::destroyBlock, this::onPostThreadJoinWorld);
        }
    }

    /**
     * True to apply the edits right away instead of going through {@link BlockEditQueue}.
     *
     * @return true to skip the queue
     */
    protected boolean skipQueue()
    {
        return false;
    }

    @Override
    protected boolean doExplode(int callCount)
    {
        if (!hasThreadStarted)
        {
            hasThreadStarted = true;
            if (level() instanceof ServerLevel serverLevel)
            {
                final LevelSnapshot snapshot = LevelSnapshot.capture(serverLevel,
                        BlockPos.containing(getPosition()), getThreadRadius());
                workerTask = getWorkerTask();
                WorkerThreadManager.INSTANCE.addWork(workerTask, snapshot);
            }
        }
        // the worker is running, the blast finishes when the edits are back in the world
        return false;
    }

    @Override
    protected void onBlastCompleted()
    {
        endBlast();
    }

    /**
     * Removes a block the worker marked for editing. 1.12 called {@code Block#onBlockExploded}; 26.3 handles
     * drops, block removal and {@code Block#wasExploded} inside {@link BlockState#onExplosionHit}.
     *
     * @param pos position to destroy
     */
    public void destroyBlock(BlockPos pos)
    {
        final BlockState state = this.world().getBlockState(pos);
        if (!state.isAir() && level() instanceof ServerLevel serverLevel)
        {
            state.onExplosionHit(serverLevel, pos, this, (stack, dropPos) -> Block.popResource(serverLevel, dropPos, stack));
        }
    }

    /**
     * True once the worker finished and results are available, 1.12 {@code Blast#isThreadCompleted()}.
     *
     * @return true when {@link #threadResults} is filled and reads are safe
     */
    public boolean isThreadCompleted()
    {
        return threadComplete;
    }

    /**
     * Results collected by the worker, 1.12 {@code Blast#getThreadResults()}.
     *
     * @return live list of positions, empty until the worker completes
     */
    public List<BlockPos> getThreadResults()
    {
        return threadResults;
    }

    /**
     * @return worker task, null until {@link #doExplode(int)} started it
     */
    public IThreadWork getWorkerTaskOrNull()
    {
        return workerTask;
    }
}
