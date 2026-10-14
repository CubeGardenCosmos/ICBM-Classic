package icbm.classic.test.projectile;

import icbm.classic.lib.projectile.InGroundData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the {@link icbm.classic.lib.projectile.EntityProjectile} base class (S3.01):
 *
 * <ul>
 *     <li>a projectile flying into a stone wall hits it, runs the impact callback and is destroyed,</li>
 *     <li>the save/load of the base class (ticks + the block it is stuck in) survives a round trip.</li>
 * </ul>
 */
public class ProjectileGameTest
{
    /** Relative x of the stone wall the projectile is fired at */
    private static final int WALL_X = 5;

    @GameTest(maxTicks = 200)
    public void projectileImpactsWall(GameTestHelper helper)
    {
        for (int y = 0; y < 4; y++)
        {
            for (int z = 0; z < 4; z++)
            {
                helper.setBlock(WALL_X, y, z, Blocks.STONE);
            }
        }

        final TestProjectile projectile = helper.spawn(TestProjectile.TYPE, new Vec3(1.5D, 1.5D, 1.5D));
        // 0.5 blocks per tick towards the wall, 3.5 blocks away. TestProjectile has no gravity, so the
        // projectile can not fall below the wall.
        projectile.setDeltaMovement(0.5D, 0.0D, 0.0D);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(projectile.impacted,
                        "projectile flew into the wall without an impact"))
                .thenExecute(() -> helper.assertTrue(projectile.isRemoved(),
                        "the impact should have destroyed the projectile"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void projectileSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        // spawn inside a stone block so the projectile records what it is stuck in
        final BlockPos groundPos = new BlockPos(2, 1, 2);
        helper.setBlock(groundPos, Blocks.STONE);
        final BlockPos absoluteGroundPos = helper.absolutePos(groundPos);

        final TestProjectile projectile = helper.spawn(TestProjectile.TYPE, new Vec3(2.5D, 1.5D, 2.5D));
        projectile.setInAirKillTime(1234).setInGroundKillTime(4321);

        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() ->
                {
                    final InGroundData ground = projectile.getInGroundData();
                    helper.assertTrue(ground != null,
                            "projectile should have recorded the block it is stuck in");

                    final HolderLookup.Provider provider = helper.getLevel().registryAccess();
                    final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
                    projectile.saveWithoutId(output);
                    final CompoundTag tag = output.buildResult();

                    final TestProjectile loaded = TestProjectile.TYPE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
                    helper.assertTrue(loaded != null, "failed to create a copy of the projectile");
                    loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

                    helper.assertTrue(loaded.getInAirKillTime() == 1234 && loaded.getInGroundKillTime() == 4321,
                            "the projectile kill times did not survive the save, saved as " + tag);

                    final InGroundData loadedGround = loaded.getInGroundData();
                    helper.assertTrue(loadedGround != null
                                    && absoluteGroundPos.equals(loadedGround.getPos())
                                    && loadedGround.getState() == ground.getState(),
                            "the block the projectile is stuck in did not survive the save, saved as " + tag);

                    loaded.discard();
                    projectile.discard();
                })
                .thenSucceed();
    }
}
