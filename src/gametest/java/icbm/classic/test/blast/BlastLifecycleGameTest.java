package icbm.classic.test.blast;

import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.content.blast.BlastStatus;
import icbm.classic.lib.actions.WorkTickingActionHandler;
import icbm.classic.lib.actions.status.ActionResponses;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Covers the {@link icbm.classic.content.blast.Blast} framework (S3.02):
 *
 * <ul>
 *     <li>a blast without a ticking holder runs setup, explodes once with call count -1, completes and
 *     unregisters itself from {@link WorkTickingActionHandler},</li>
 *     <li>a blast that fails setup reports {@link BlastStatus#SETUP_ERROR} and never explodes.</li>
 * </ul>
 */
public class BlastLifecycleGameTest
{
    /** Block the test blast is told to clear, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(3, 2, 3);

    private static TestBlast buildBlast(GameTestHelper helper, TestBlast blast)
    {
        final BlockPos center = helper.absolutePos(CENTER);
        blast.setBlastWorld(helper.getLevel())
                .setBlastPosition(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D)
                .setBlastSize(3);
        blast.setTarget(center);
        blast.buildBlast();
        return blast;
    }

    @GameTest(maxTicks = 40)
    public void blastRunsLifecycleAndUnregisters(GameTestHelper helper)
    {
        helper.setBlock(CENTER, Blocks.STONE);

        final TestBlast blast = buildBlast(helper, new TestBlast());
        final IActionStatus status = blast.doAction();

        helper.assertTrue(status == ActionResponses.COMPLETED,
                "a finished blast should report COMPLETED but reported " + status);
        helper.assertTrue(blast.lifecycle.equals(List.of("setup", "explode")),
                "blast lifecycle hooks ran in the wrong order: " + blast.lifecycle);
        helper.assertTrue(blast.explodeCalls.equals(List.of(-1)),
                "a non ticking blast should explode once with call count -1, was " + blast.explodeCalls);
        helper.assertTrue(blast.isCompleted(), "blast should flag itself as completed");
        helper.assertTrue(!WorkTickingActionHandler.activeBlasts.contains(blast),
                "a completed blast must not stay in the active blast list");
        helper.assertBlockPresent(Blocks.AIR, CENTER);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void blastWithFailedSetupReportsSetupError(GameTestHelper helper)
    {
        helper.setBlock(CENTER, Blocks.STONE);

        final TestBlast blast = buildBlast(helper, new TestBlast(false));
        final IActionStatus status = blast.doAction();

        helper.assertTrue(status == BlastStatus.SETUP_ERROR,
                "a failed setup should report SETUP_ERROR but reported " + status);
        helper.assertTrue(blast.explodeCalls.isEmpty(),
                "a blast that failed setup must not explode, calls were " + blast.explodeCalls);
        helper.assertBlockPresent(Blocks.STONE, CENTER);

        // close the blast so the shared active list is clean for the next test
        blast.completeBlast();
        helper.assertTrue(!WorkTickingActionHandler.activeBlasts.contains(blast),
                "completing a blast must remove it from the active blast list");
        helper.succeed();
    }
}
