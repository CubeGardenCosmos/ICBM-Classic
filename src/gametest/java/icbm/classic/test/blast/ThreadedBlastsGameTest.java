package icbm.classic.test.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.missile.logic.source.ActionSource;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Runs the threaded blasts ported by S3.06 through the same registry path the game uses.
 * <ul>
 *     <li>nuclear → the stone cube around the blast is destroyed,</li>
 *     <li>thermobaric → same raycast/worker path as nuclear without the radiation follow up blasts,</li>
 *     <li>antimatter → bedrock in the blast radius is destroyed when {@code damage_unbreakable} is on.</li>
 * </ul>
 * These blasts collect their blocks on a worker thread and apply them over ticks through
 * {@code BlockEditQueue}, so every case waits with a sequence instead of asserting right after the trigger.
 * <p>
 * The redmatter entity of the same step is not asserted here (it is owned by the sibling change that adds
 * {@code content/blast/redmatter/EntityRedmatter}).
 */
public class ThreadedBlastsGameTest
{
    /** Center of the blast, relative to the default 8x8x8 structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Radius of the stone cube that is built around the center and checked after the blast */
    private static final int CUBE_RADIUS = 1;

    private static final Predicate<BlockState> IS_AIR = BlockState::isAir;

    /**
     * Triggers an explosive by id through the registry, mirroring how the game creates a blast (see
     * {@code BasicBlastsGameTest}).
     *
     * @param helper      test helper
     * @param explosiveId explosive id inside the mod namespace
     * @param size        blast size to force, keeps the affected area inside the test structure
     * @return the action that was started
     */
    private static IAction triggerBlast(GameTestHelper helper, String explosiveId, int size)
    {
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id(explosiveId), false);
        final BlockPos pos = helper.absolutePos(CENTER);
        final Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final IActionSource source = new ActionSource(helper.getLevel(), center, new CauseByTimer("test", 0));

        final IAction action = data.create(helper.getLevel(), center.x, center.y, center.z, source, null);
        if (action instanceof IBlastInit init)
        {
            init.setBlastSize(size);
        }
        action.doAction();
        return action;
    }

    /** Places a solid stone cube around the blast center */
    private static void buildStoneCube(GameTestHelper helper)
    {
        for (int x = -CUBE_RADIUS; x <= CUBE_RADIUS; x++)
        {
            for (int y = -CUBE_RADIUS; y <= CUBE_RADIUS; y++)
            {
                for (int z = -CUBE_RADIUS; z <= CUBE_RADIUS; z++)
                {
                    helper.setBlock(CENTER.offset(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /**
     * Counts the blocks of the stone cube around the center that match the predicate. Only the cube is
     * counted, the test structure itself is empty air.
     */
    private static int countCubeBlocks(GameTestHelper helper, Predicate<BlockState> test)
    {
        int count = 0;
        for (int x = -CUBE_RADIUS; x <= CUBE_RADIUS; x++)
        {
            for (int y = -CUBE_RADIUS; y <= CUBE_RADIUS; y++)
            {
                for (int z = -CUBE_RADIUS; z <= CUBE_RADIUS; z++)
                {
                    if (test.test(helper.getBlockState(CENTER.offset(x, y, z))))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * The stone cube around the blast center has to be gone. The worker collects the raycast hits and the
     * server thread applies them over ticks, so this waits instead of asserting right away.
     */
    private static void succeedWhenCubeCleared(GameTestHelper helper, String explosiveId, Runnable restoreConfig)
    {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(countCubeBlocks(helper, IS_AIR) > 0,
                        explosiveId + " blast did not destroy any of the stone blocks around the center"))
                .thenExecute(restoreConfig)
                .thenSucceed();
    }

    @GameTest(maxTicks = 400)
    public void nuclearBlastClearsStone(GameTestHelper helper)
    {
        buildStoneCube(helper);
        helper.assertTrue(countCubeBlocks(helper, IS_AIR) == 0, "test cube should start solid");

        //The nuclear blast spawns the radiation follow up blasts after its blocks are gone (see
        //BlastNuclear#onBlastCompleted). Their scale comes from config and is far larger than the test
        //structure, so it is lowered for the test and restored before the test succeeds.
        final double previousRotScale = ConfigBlast.nuclear.rotScale;
        final double previousMutationScale = ConfigBlast.nuclear.mutationScale;
        ConfigBlast.nuclear.rotScale = 8;
        ConfigBlast.nuclear.mutationScale = 8;

        //energy and the rest of the config stay as configured, only the size is forced
        triggerBlast(helper, "nuclear", 4);

        succeedWhenCubeCleared(helper, "nuclear", () ->
        {
            ConfigBlast.nuclear.rotScale = previousRotScale;
            ConfigBlast.nuclear.mutationScale = previousMutationScale;
        });
    }

    @GameTest(maxTicks = 400)
    public void thermobaricBlastClearsStone(GameTestHelper helper)
    {
        buildStoneCube(helper);
        helper.assertTrue(countCubeBlocks(helper, IS_AIR) == 0, "test cube should start solid");

        //thermobaric runs the same worker raycast as nuclear without the radiation follow up blasts
        triggerBlast(helper, "thermobaric", 4);

        //no config is changed by this case
        succeedWhenCubeCleared(helper, "thermobaric", () -> { });
    }

    @GameTest(maxTicks = 400)
    public void antimatterBlastClearsBedrock(GameTestHelper helper)
    {
        helper.setBlock(CENTER, Blocks.BEDROCK);
        helper.assertBlockPresent(Blocks.BEDROCK, CENTER);

        //bedrock has a destroy speed of -1, the blast only removes it when the config allows unbreakable
        //blocks to be removed (config.icbmclassic:blast.antimatter.break_unbreakable)
        final boolean previousDamageUnbreakable = ConfigBlast.antimatter.damageUnbreakable;
        ConfigBlast.antimatter.damageUnbreakable = true;

        triggerBlast(helper, "antimatter", 3);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, CENTER))
                .thenExecute(() -> ConfigBlast.antimatter.damageUnbreakable = previousDamageUnbreakable)
                .thenSucceed();
    }
}
