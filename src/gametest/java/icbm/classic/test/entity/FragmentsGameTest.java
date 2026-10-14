package icbm.classic.test.entity;

import icbm.classic.content.entity.EntityFragments;
import icbm.classic.content.reg.ModEntities;
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
 * Covers the S3.03 {@link EntityFragments}:
 *
 * <ul>
 *     <li>the arrow mode falls with the 1.12 gravity constant,</li>
 *     <li>the anvil mode flies into a wall and sticks in it,</li>
 *     <li>the blast mode explodes on ground impact and destroys the block it landed on,</li>
 *     <li>the values 1.12 persisted (explosive flag, shake timer) plus the vanilla velocity survive a
 *     save/load round trip.</li>
 * </ul>
 *
 * The 1.12 class did not save the in-air tick counter or the anvil flag (see {@link EntityFragments}), so
 * neither is asserted here.
 */
public class FragmentsGameTest
{
    /** Relative x of the stone wall the anvil fragment is fired at */
    private static final int WALL_X = 5;

    /** X of the column the fragments fly and fall down */
    private static final double COLUMN_X = 1.5D;

    @GameTest(maxTicks = 60)
    public void arrowFragmentFallsWithGravity(GameTestHelper helper)
    {
        final EntityFragments fragment = helper.spawn(ModEntities.FRAGMENT, new Vec3(COLUMN_X, 6.0D, COLUMN_X));
        final double startY = fragment.getY();

        helper.startSequence()
                .thenIdle(8)
                .thenExecute(() ->
                {
                    helper.assertTrue(!fragment.isRemoved(), "the arrow fragment should still be flying");
                    // 0.99 air resistance plus 0.05 gravity per tick, 1.76 blocks over 8 ticks
                    helper.assertTrue(fragment.getY() < startY - 1.0D,
                            "arrow fragment did not fall, y=" + fragment.getY());
                    helper.assertTrue(fragment.getY() > startY - 2.5D,
                            "arrow fragment fell faster than gravity allows, y=" + fragment.getY());
                    helper.assertTrue(fragment.getDeltaMovement().y < -0.25D,
                            "gravity should have pulled the fragment down, motion=" + fragment.getDeltaMovement());
                    fragment.discard();
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void anvilFragmentSticksIntoWall(GameTestHelper helper)
    {
        for (int y = 0; y < 4; y++)
        {
            for (int z = 0; z < 4; z++)
            {
                helper.setBlock(WALL_X, y, z, Blocks.STONE);
            }
        }

        final EntityFragments fragment = helper.spawn(ModEntities.FRAGMENT, new Vec3(COLUMN_X, 3.5D, COLUMN_X));
        fragment.setAnvil(true);
        fragment.setDeltaMovement(0.5D, 0.0D, 0.0D);

        // entity positions are absolute, the wall constant is relative to the test structure
        final double spawnX = fragment.getX();
        final double wallX = helper.absolutePos(new BlockPos(WALL_X, 0, 0)).getX();

        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(fragment.getX() > spawnX + 1.0D,
                        "anvil fragment did not fly towards the wall, x=" + fragment.getX() + " spawn=" + spawnX))
                // 1.12 set the shake timer to 7 when the fragment hit the block
                .thenWaitUntil(() -> helper.assertTrue(fragment.arrowShake > 0,
                        "anvil fragment did not stick into the wall"))
                // the fragment is pushed back 0.05 blocks from the hit position, so it ends up on the face
                .thenExecute(() -> helper.assertTrue(Math.abs(fragment.getX() - wallX) < 0.5D,
                        "anvil fragment should have stopped at the wall, x=" + fragment.getX() + " wall=" + wallX))
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void blastFragmentExplodesOnGroundImpact(GameTestHelper helper)
    {
        final BlockPos ground = new BlockPos((int) COLUMN_X, 0, (int) COLUMN_X);
        helper.setBlock(ground, Blocks.STONE);

        final EntityFragments fragment = helper.spawn(ModEntities.FRAGMENT, new Vec3(COLUMN_X, 4.0D, COLUMN_X));
        fragment.setExplosive(true);
        // 1.12 set the size from the blast config, a small one keeps the test short and quiet
        fragment.explosionSize = 2.0F;

        helper.startSequence()
                // the blast mode explodes when it reaches the ground instead of dropping dead
                .thenWaitUntil(() -> helper.assertTrue(fragment.isRemoved(),
                        "explosive fragment did not explode after landing"))
                .thenExecute(() -> helper.assertBlockNotPresent(Blocks.STONE, ground))
                .thenSucceed();
    }

    @GameTest(maxTicks = 40)
    public void fragmentSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final EntityFragments fragment = helper.spawn(ModEntities.FRAGMENT, new Vec3(2.5D, 3.5D, 2.5D));
        // 1.12 writeEntityToNBT wrote the shake timer and the explosive flag, nothing else
        fragment.arrowShake = 7;
        fragment.setExplosive(true);
        fragment.setDeltaMovement(0.25D, -0.125D, 0.5D);

        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        fragment.saveWithoutId(output);
        final CompoundTag tag = output.buildResult();

        final EntityFragments loaded = ModEntities.FRAGMENT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loaded != null, "failed to create a copy of the fragment");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

        helper.assertTrue(loaded.isExplosive(), "the explosive mode did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.arrowShake == 7, "the shake timer did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.getDeltaMovement().distanceToSqr(fragment.getDeltaMovement()) < 1.0E-8D,
                "the velocity did not survive the save, saved as " + tag);

        loaded.discard();
        fragment.discard();
        helper.succeed();
    }
}
