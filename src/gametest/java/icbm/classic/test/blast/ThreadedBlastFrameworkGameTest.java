package icbm.classic.test.blast;

import icbm.classic.lib.thread.BlockEditQueue;
import icbm.classic.lib.thread.IThreadWork;
import icbm.classic.lib.thread.LevelSnapshot;
import icbm.classic.lib.thread.WorkerThreadManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Covers the threaded blast framework (S1.09):
 *
 * <ul>
 *     <li>the server thread captures a 5x5x5 stone cube into a {@link LevelSnapshot},</li>
 *     <li>a worker thread collects the stone positions from that snapshot,</li>
 *     <li>{@link BlockEditQueue} turns them into air from the server thread and calls {@code onComplete} once.</li>
 * </ul>
 *
 * <p>One block is replaced by gold <i>after</i> the capture: the worker may only see the captured state, so
 * that position still has to be turned into air. A live world read (or a snapshot that aliases the live
 * chunk data) would skip it and fail the final assertion.</p>
 *
 * <p>{@code maxTicks} is well above the default 20: the game test server runs far faster than 20 ticks per
 * second (about 1.3ms per tick on the reference machine), so 20 ticks is under 30ms of wall clock, less than
 * the 100ms the worker thread sleeps while it has no work. The test still finishes within a handful of ticks
 * once the worker picked the task up.</p>
 */
public class ThreadedBlastFrameworkGameTest
{
    /** Edge length of the cube the test builds, 5x5x5 = 125 blocks */
    private static final int EDGE = 5;
    /** Capture radius that covers the whole cube */
    private static final int RADIUS = EDGE / 2;
    /** First relative position of the cube inside the test structure (the default structure is 8x8x8) */
    private static final BlockPos CORNER = new BlockPos(1, 1, 1);

    @GameTest(maxTicks = 600)
    public void snapshotDrivesBudgetedMainThreadEdits(GameTestHelper helper)
    {
        if (WorkerThreadManager.INSTANCE == null)
        {
            helper.fail("WorkerThreadManager was not created by ICBMClassic#onInitialize");
        }

        final ServerLevel level = helper.getLevel();
        final List<BlockPos> relativePositions = new ArrayList<>();
        final List<BlockPos> worldPositions = new ArrayList<>();

        for (int x = 0; x < EDGE; x++)
        {
            for (int y = 0; y < EDGE; y++)
            {
                for (int z = 0; z < EDGE; z++)
                {
                    final BlockPos pos = CORNER.offset(x, y, z);
                    helper.setBlock(pos, Blocks.STONE);
                    relativePositions.add(pos);
                    worldPositions.add(helper.absolutePos(pos));
                }
            }
        }

        //Server thread captures the blocks, the worker thread is only allowed to read this copy
        final BlockPos center = helper.absolutePos(CORNER.offset(RADIUS, RADIUS, RADIUS));
        final LevelSnapshot snapshot = LevelSnapshot.capture(level, center, RADIUS);
        helper.assertTrue(snapshot.getSectionCount() > 0,
                "capture should have copied the loaded chunk sections around the test blocks");

        //Change a block inside the captured area after the capture: the worker has to see the stone that
        //was captured, not the gold that is in the world now
        final BlockPos mutated = relativePositions.get(0);
        helper.setBlock(mutated, Blocks.GOLD_BLOCK);

        final CollectStoneWork work = new CollectStoneWork(level.dimension(), level, worldPositions);
        WorkerThreadManager.INSTANCE.addWork(work, snapshot);

        helper.startSequence()
                .thenWaitUntil(() -> relativePositions.forEach(pos -> helper.assertBlockPresent(Blocks.AIR, pos)))
                .thenExecute(() -> helper.assertTrue(work.completions.get() == 1,
                        "BlockEditQueue should have called onComplete once, was " + work.completions.get()))
                .thenSucceed();
    }

    /**
     * Test work: collects every stone the snapshot shows and hands the positions to {@link BlockEditQueue}.
     * The collection runs on a worker thread and only reads the snapshot, the queued action is the only
     * place that touches the level (and it runs on the server thread).
     */
    private static final class CollectStoneWork implements IThreadWork
    {
        private final ResourceKey<Level> dimension;
        private final ServerLevel level;
        private final List<BlockPos> positions;
        private final List<BlockPos> edits = new ArrayList<>();
        private final AtomicInteger completions = new AtomicInteger();

        private CollectStoneWork(ResourceKey<Level> dimension, ServerLevel level, List<BlockPos> positions)
        {
            this.dimension = dimension;
            this.level = level;
            this.positions = positions;
        }

        @Override
        public boolean doRun(int steps, LevelSnapshot snapshot)
        {
            for (BlockPos pos : positions)
            {
                if (snapshot.getBlockState(pos).getBlock() == Blocks.STONE)
                {
                    edits.add(pos);
                }
            }
            return false;
        }

        @Override
        public void onStarted(LevelSnapshot snapshot)
        {
        }

        @Override
        public void onCompleted()
        {
            BlockEditQueue.submit(dimension, edits,
                    pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL),
                    completions::incrementAndGet);
        }
    }
}
