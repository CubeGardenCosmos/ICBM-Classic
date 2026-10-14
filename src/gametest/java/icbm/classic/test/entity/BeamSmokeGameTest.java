package icbm.classic.test.entity;

import icbm.classic.content.entity.EntityLightBeam;
import icbm.classic.content.entity.EntitySmoke;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the two visual blast entities (S3.03):
 *
 * <ul>
 *     <li>{@link EntityLightBeam} spawns, grows to the progress the blast asked for and removes itself once
 *     its death cycle finished or the 1.12 five minute safety time out is reached,</li>
 *     <li>{@link EntitySmoke} spawns and removes itself once it is older than its lifetime,</li>
 *     <li>both keep the values the client renders from over a save/load round trip.</li>
 * </ul>
 */
public class BeamSmokeGameTest
{
    /** Relative spawn position used by every test */
    private static final Vec3 SPAWN = new Vec3(1.5D, 1.5D, 1.5D);

    @GameTest(maxTicks = 40)
    public void lightBeamGrowsThenDiesOnDeathCycle(GameTestHelper helper)
    {
        final EntityLightBeam beam = helper.spawn(ModEntities.LIGHT_BEAM, SPAWN);
        // 1.12 BlastBeam spat the beam at 0.1 progress and grew it as the blast progressed
        beam.setTargetBeamProgress(0.2f);

        helper.startSequence()
                .thenExecute(() -> helper.assertEntityPresent(ModEntities.LIGHT_BEAM))
                .thenWaitUntil(() -> helper.assertTrue(beam.getBeamProgress() >= 0.2f,
                        "the beam should grow towards its target progress, was " + beam.getBeamProgress()))
                .thenExecute(() ->
                {
                    helper.assertTrue(!beam.isRemoved(), "the beam must keep living while it grows");
                    beam.startDeathCycle();
                })
                .thenWaitUntil(() -> helper.assertTrue(beam.isRemoved(),
                        "a beam that finished its death cycle should remove itself"))
                .thenExecute(() -> helper.assertEntityNotPresent(ModEntities.LIGHT_BEAM))
                .thenSucceed();
    }

    @GameTest(maxTicks = 20)
    public void lightBeamIsKilledByLegacySafetyTimeout(GameTestHelper helper)
    {
        final EntityLightBeam beam = helper.spawn(ModEntities.LIGHT_BEAM, SPAWN);
        // 1.12 kills the beam after 20 * 60 * 5 ticks in case the blast never runs the death cycle. The
        // counter is jumped here instead of waiting for the five minutes.
        beam.tickCount = 20 * 60 * 5;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(beam.isRemoved(),
                        "a beam older than five minutes should be removed by the safety timeout"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 40)
    public void smokeIsRemovedAfterItsLifetime(GameTestHelper helper)
    {
        final EntitySmoke smoke = helper.spawn(ModEntities.SMOKE, SPAWN);
        // 1.12 default is 100 ticks, lowered so the test does not wait for the full cloud
        smoke.ticksToLive = 20;

        helper.startSequence()
                .thenExecute(() -> helper.assertEntityPresent(ModEntities.SMOKE))
                .thenIdle(5)
                .thenExecute(() -> helper.assertTrue(!smoke.isRemoved(),
                        "the cloud should still be alive while it is younger than ticksToLive"))
                .thenWaitUntil(() -> helper.assertTrue(smoke.isRemoved(),
                        "the cloud should be removed once it is older than ticksToLive"))
                .thenExecute(() -> helper.assertEntityNotPresent(ModEntities.SMOKE))
                .thenSucceed();
    }

    @GameTest(maxTicks = 60)
    public void lightBeamSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final EntityLightBeam beam = helper.spawn(ModEntities.LIGHT_BEAM, SPAWN);
        beam.setColor(0.2f, 0.4f, 0.6f);
        beam.beamSize = 1f;
        beam.beamGlowSize = 2f;
        beam.setTargetBeamProgress(0.5f);
        beam.beamGrowthRate = 0.1f;
        beam.startDeathCycle();

        helper.startSequence()
                // let the beam animate, so the synced progress that is carried is not the initial value
                .thenIdle(2)
                .thenExecute(() ->
                {
                    final float progress = beam.getBeamProgress();

                    final HolderLookup.Provider provider = helper.getLevel().registryAccess();
                    final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
                    beam.saveWithoutId(output);
                    final CompoundTag tag = output.buildResult();

                    final EntityLightBeam loaded = ModEntities.LIGHT_BEAM.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
                    helper.assertTrue(loaded != null, "failed to create a copy of the beam");
                    loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

                    helper.assertTrue(loaded.red == 0.2f && loaded.green == 0.4f && loaded.blue == 0.6f,
                            "the beam colour did not survive the save, saved as " + tag);
                    helper.assertTrue(loaded.beamSize == 1f && loaded.beamGlowSize == 2f,
                            "the beam size did not survive the save, saved as " + tag);
                    helper.assertTrue(loaded.targetBeamProgress == 0.5f && loaded.beamGrowthRate == 0.1f
                                    && loaded.deathCycle,
                            "the beam animation state did not survive the save, saved as " + tag);
                    helper.assertTrue(loaded.getBeamProgress() == progress,
                            "the synced beam progress did not survive the save, saved as " + tag);

                    loaded.discard();
                    beam.discard();
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 60)
    public void smokeSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final EntitySmoke smoke = helper.spawn(ModEntities.SMOKE, SPAWN);
        smoke.setColor(0.2f, 0.4f, 0.6f);
        smoke.ticksToLive = 123;

        helper.startSequence()
                .thenExecute(() ->
                {
                    final HolderLookup.Provider provider = helper.getLevel().registryAccess();
                    final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
                    smoke.saveWithoutId(output);
                    final CompoundTag tag = output.buildResult();

                    final EntitySmoke loaded = ModEntities.SMOKE.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
                    helper.assertTrue(loaded != null, "failed to create a copy of the cloud");
                    loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

                    helper.assertTrue(loaded.ticksToLive == 123,
                            "the cloud lifetime did not survive the save, saved as " + tag);
                    // 1.12 only ever persisted "red"; the "green" node saved and loaded red and the third
                    // node was named "red" again, so only the value 1.12 carried is checked here
                    helper.assertTrue(loaded.red == 0.2f,
                            "the cloud colour 1.12 persisted did not survive the save, saved as " + tag);

                    loaded.discard();
                    smoke.discard();
                })
                .thenSucceed();
    }
}
