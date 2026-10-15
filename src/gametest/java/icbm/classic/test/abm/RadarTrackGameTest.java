package icbm.classic.test.abm;

import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.abm.radar.RadarTrack;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RadarTrackGameTest
{
    private static final class TestMissile extends EntityExplosiveMissile
    {
        private TestMissile(Level level)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
        }

        @Override
        public int getTicksInAir()
        {
            return 2;
        }

        @Override
        public void tick()
        {
            // Test trajectory is controlled explicitly; the radar still reads this entity's velocity.
        }
    }

    private record Fixture(TileRadarStation radar, BlockPos pos, TestMissile missile)
    {
    }

    private Fixture setup(GameTestHelper helper, double horizontalOffset, double horizontalVelocity)
    {
        final BlockPos localRadar = new BlockPos(2, 1, 2);
        helper.setBlock(localRadar, ModBlocks.RADAR_STATION);
        final BlockPos radarPos = helper.absolutePos(localRadar);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(radarPos);
        helper.assertTrue(radar != null, "radar station block entity must be present");
        radar.setDetectionRange(100);
        radar.setTriggerRange(10);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());

        final TestMissile missile = new TestMissile(helper.getLevel());
        missile.explosive.setStack(new net.minecraft.world.item.ItemStack(
                ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        missile.setPos(radarPos.getX() + 0.5 + horizontalOffset, radarPos.getY() + 40.5, radarPos.getZ() + 0.5);
        missile.setDeltaMovement(new Vec3(horizontalVelocity, -1, 0));
        missile.setNoGravity(true);
        missile.setFreezeMotion(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(missile), "test missile must spawn");
        RadarRegistry.add(missile);
        return new Fixture(radar, radarPos, missile);
    }

    @GameTest(maxTicks = 10, padding = 16)
    public void inboundMissileProducesThreatTrackAndRedstone(GameTestHelper helper)
    {
        final Fixture fixture = setup(helper, 0, 0);
        helper.succeedWhen(() -> {
            RadarRegistry.add(fixture.missile());
            final RadarTrack track = fixture.radar().getTrackTable().get(fixture.missile().getUUID());
            if (track == null) return;
            helper.assertTrue(track.status == TrackStatus.THREAT, "impact inside protection range must be THREAT");
            helper.assertTrue(track.predictedImpact != null, "threat must have predicted impact point");
            final double dx = track.predictedImpact.x() - (fixture.pos().getX() + 0.5);
            final double dz = track.predictedImpact.z() - (fixture.pos().getZ() + 0.5);
            helper.assertTrue(Math.sqrt(dx * dx + dz * dz) < 10, "predicted impact must be within 10 blocks horizontally");
            helper.assertTrue(fixture.radar().getStrongRedstonePower(null) == 1,
                    "redstone strength must equal the single THREAT track count");
            fixture.missile().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 10, padding = 16)
    public void sidewaysMissileOutsideProtectionIsDetected(GameTestHelper helper)
    {
        final Fixture fixture = setup(helper, 5, 1);
        helper.succeedWhen(() -> {
            RadarRegistry.add(fixture.missile());
            final RadarTrack track = fixture.radar().getTrackTable().get(fixture.missile().getUUID());
            if (track == null) return;
            helper.assertTrue(track.status == TrackStatus.DETECTED,
                    "sideways trajectory landing outside protection range must remain DETECTED");
            helper.assertTrue(track.predictedImpact != null, "detected missile still records a predicted impact");
            fixture.missile().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 12, padding = 16)
    public void missingTargetIsLostThenRemoved(GameTestHelper helper)
    {
        final Fixture fixture = setup(helper, 0, 0);
        helper.succeedWhen(() -> {
            RadarRegistry.add(fixture.missile());
            final RadarTrack existing = fixture.radar().getTrackTable().get(fixture.missile().getUUID());
            if (existing == null) return;
            fixture.missile().discard();
            helper.runAfterDelay(1, () -> {
                final RadarTrack lost = fixture.radar().getTrackTable().get(fixture.missile().getUUID());
                helper.assertTrue(lost != null && lost.status == TrackStatus.LOST,
                        "first scan after target removal must mark its track LOST");
                helper.runAfterDelay(1, () -> {
                    helper.assertTrue(fixture.radar().getTrackTable().get(fixture.missile().getUUID()) == null,
                            "second scan after target removal must remove the lost track");
                    helper.succeed();
                });
            });
        });
    }
}
