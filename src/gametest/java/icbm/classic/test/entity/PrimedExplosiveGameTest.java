package icbm.classic.test.entity;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.transform.vector.Pos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Covers {@link EntityExplosive} (S3.03, {@code icbmclassic:primed_explosive}):
 *
 * <ul>
 *     <li>a primed explosive spawned for {@code condensed} holds the explosive id, facing and fuse it was
 *     created with,</li>
 *     <li>the fuse counts down once per tick without running the blast,</li>
 *     <li>explosive id, fuse and position survive a save/load round trip.</li>
 * </ul>
 *
 * The fuse is set to a value well above the ticks each test waits for, so no test lets it reach zero: the
 * blast factories in {@code ExplosiveInit} are still {@code todoBlast} placeholders that throw until S3.04
 * ports the blast classes, so the "the explosion happened" assertion lives in the S3.04 blast game tests.
 */
public class PrimedExplosiveGameTest
{
    /** Relative position the explosives are spawned at, inside the test structure */
    private static final BlockPos SPAWN_POS = new BlockPos(2, 2, 2);

    /** Fuse used by every test, far above the ticks the tests wait for */
    private static final int FUSE = 20;

    private static Pos spawnPos(GameTestHelper helper)
    {
        final BlockPos pos = helper.absolutePos(SPAWN_POS);
        return new Pos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    /**
     * Spawns a primed explosive the way the explosive block does, 1.12
     * {@code new EntityExplosive(world, pos, facing, stack)}.
     */
    private static EntityExplosive spawnPrimedExplosive(GameTestHelper helper, Direction facing)
    {
        final EntityExplosive explosive = new EntityExplosive(helper.getLevel(), spawnPos(helper), facing, ICBMExplosives.CONDENSED.getRegistryKey());
        helper.getLevel().addFreshEntity(explosive);
        return explosive;
    }

    @GameTest(maxTicks = 40)
    public void primedExplosiveHoldsIdAndFuse(GameTestHelper helper)
    {
        final EntityExplosive explosive = spawnPrimedExplosive(helper, Direction.SOUTH);
        explosive.setFuse(FUSE);

        helper.assertTrue(ICBMExplosives.CONDENSED.getRegistryKey().equals(explosive.getExplosiveId()),
                "primed explosive should hold the condensed id but holds " + explosive.getExplosiveId());
        helper.assertTrue(explosive.getExplosiveData().equals(ICBMExplosives.CONDENSED),
                "primed explosive should resolve condensed from its id but resolved " + explosive.getExplosiveData());
        helper.assertTrue(explosive.getFuse() == FUSE,
                "primed explosive should hold the fuse it was set to (" + FUSE + ") but holds " + explosive.getFuse());
        helper.assertTrue(explosive.getDirection() == Direction.SOUTH,
                "primed explosive should keep the facing it was spawned with but has " + explosive.getDirection());
        helper.assertTrue(spawnPos(helper).equals(new Pos(explosive)), "primed explosive should sit on its spawn position but is at " + explosive.position());
        // 1.12 provided the explosive through the Forge capability; the 26.3 counterpart is the entity
        // implementing IExplosive, which ICBMClassicHelpers#getExplosive(Entity) reads
        helper.assertTrue(ICBMClassicHelpers.getExplosive(explosive) == explosive,
                "primed explosive should provide itself as its explosive but the helper returned " + ICBMClassicHelpers.getExplosive(explosive));
        helper.assertTrue(explosive.getExplosiveCap() != null, "primed explosive should keep its explosive capability");

        explosive.discard();
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void fuseCountsDownWithoutExploding(GameTestHelper helper)
    {
        final EntityExplosive explosive = helper.spawn(ModEntities.PRIMED_EXPLOSIVE, new Vec3(SPAWN_POS.getX() + 0.5D, SPAWN_POS.getY(), SPAWN_POS.getZ() + 0.5D));
        explosive.setExplosiveId(ICBMExplosives.CONDENSED.getRegistryKey());
        explosive.setFuse(FUSE);

        helper.assertTrue(explosive.getFuse() == FUSE, "the fuse should start at " + FUSE + " but is " + explosive.getFuse());

        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() ->
                {
                    // The fuse must count down without reaching zero, the tick that would run
                    // IExplosiveData#create(...).doAction() is asserted by the S3.04 blast tests
                    helper.assertTrue(explosive.getFuse() < FUSE,
                            "the fuse should have counted down from " + FUSE + " but is " + explosive.getFuse());
                    helper.assertTrue(explosive.getFuse() > 0,
                            "this test must not let the fuse reach the blast but the fuse is " + explosive.getFuse());
                    helper.assertTrue(!explosive.isRemoved(),
                            "a primed explosive must not be removed before its fuse runs out");
                    explosive.discard();
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 40)
    public void primedExplosiveSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final EntityExplosive explosive = spawnPrimedExplosive(helper, Direction.EAST);
        explosive.setFuse(FUSE);

        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        explosive.saveWithoutId(output);
        final CompoundTag tag = output.buildResult();

        final EntityExplosive loaded = ModEntities.PRIMED_EXPLOSIVE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(loaded != null, "failed to create a copy of the primed explosive");
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

        helper.assertTrue(ICBMExplosives.CONDENSED.getRegistryKey().equals(loaded.getExplosiveId()),
                "the explosive id did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.getExplosiveData().equals(ICBMExplosives.CONDENSED),
                "the loaded primed explosive should resolve condensed from its id, saved as " + tag);
        helper.assertTrue(loaded.getFuse() == FUSE,
                "the fuse did not survive the save, saved as " + tag);
        helper.assertTrue(loaded.position().equals(explosive.position()),
                "the position did not survive the save, saved as " + tag + " and loaded at " + loaded.position());

        loaded.discard();
        explosive.discard();
        helper.succeed();
    }
}
