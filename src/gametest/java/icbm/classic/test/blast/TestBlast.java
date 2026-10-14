package icbm.classic.test.blast;

import icbm.classic.content.blast.Blast;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal {@link Blast} used by {@link BlastLifecycleGameTest} to drive the 1.12 lifecycle
 * (setup -> explode -> completed) without any explosive content.
 * <p>
 * {@code setupBlast()} and {@code doExplode(int)} record the order they were called in and, when a target
 * block was given, {@code doExplode} replaces that block with air to give the test something observable.
 */
public class TestBlast extends Blast
{
    /** Names of the lifecycle hooks in call order */
    public final List<String> lifecycle = new ArrayList<>();
    /** Values {@code doExplode} was called with */
    public final List<Integer> explodeCalls = new ArrayList<>();

    private final boolean setupSucceeds;
    private BlockPos target;

    public TestBlast()
    {
        this(true);
    }

    /**
     * @param setupSucceeds value returned by {@code setupBlast()}, false to simulate a failed setup
     */
    public TestBlast(boolean setupSucceeds)
    {
        this.setupSucceeds = setupSucceeds;
    }

    /**
     * Block to replace with air when the blast runs.
     *
     * @param target absolute block position
     * @return this
     */
    public TestBlast setTarget(BlockPos target)
    {
        this.target = target;
        return this;
    }

    @Override
    protected boolean setupBlast()
    {
        lifecycle.add("setup");
        return setupSucceeds;
    }

    @Override
    protected boolean doExplode(int callCount)
    {
        explodeCalls.add(callCount);
        lifecycle.add("explode");
        if (target != null && world() != null)
        {
            world().setBlock(target, Blocks.AIR.defaultBlockState(), 3);
        }
        // one pass blast, nothing running in the background
        return true;
    }
}
