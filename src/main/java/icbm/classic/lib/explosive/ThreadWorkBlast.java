package icbm.classic.lib.explosive;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.thread.BlockEditQueue;
import icbm.classic.lib.thread.IThreadWork;
import icbm.classic.lib.thread.LevelSnapshot;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Generic worker task that collects the positions a blast wants to edit.
 *
 * <p>The run function is expected to read the provided {@link LevelSnapshot} instead of the world, and
 * {@link #onCompleted()} runs on the worker thread so the collected positions have to be handed to
 * {@link BlockEditQueue} by the completion callback.</p>
 *
 * Created by Dark(DarkGuardsman, Robin) on 10/8/2018.
 */
public class ThreadWorkBlast implements IThreadWork
{
    public final String name;
    public List<BlockPos> editPositions = new ArrayList<>();
    public BiFunction<Integer, Consumer<BlockPos>, Boolean> runFunction;
    public Consumer<List<BlockPos>> onComplete;


    public ThreadWorkBlast(String name, BiFunction<Integer, Consumer<BlockPos>, Boolean> runFunction, Consumer<List<BlockPos>> onComplete)
    {
        this.name = name;
        this.runFunction = runFunction;
        this.onComplete = onComplete;
    }

    /**
     * Runs one step of the collection pass. The run function owns the snapshot it closes over (it is also
     * available as the given parameter), which is why it does not receive it separately.
     */
    @Override
    public boolean doRun(int steps, LevelSnapshot snapshot)
    {
        return runFunction.apply(steps, (blockPos) -> editPositions.add(blockPos));
    }

    @Override
    public void onStarted(LevelSnapshot snapshot)
    {
        ICBMClassic.logger().debug("ThreadWorkerBlast({}): started", name);
    }

    @Override
    public void onCompleted()
    {
        ICBMClassic.logger().debug("ThreadWorkerBlast({}): completed", name);
        onComplete.accept(editPositions);
    }
}
