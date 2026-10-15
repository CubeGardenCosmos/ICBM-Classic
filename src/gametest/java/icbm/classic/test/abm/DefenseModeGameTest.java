package icbm.classic.test.abm;

import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.radar.RadarDefenseMode;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DefenseModeGameTest
{
    private static final String EMP_ONLY_CHANNEL = "defense-mode-emp-only";
    private static final String ABM_ONLY_CHANNEL = "defense-mode-abm-only";
    private static final String ABM_THEN_EMP_CHANNEL = "defense-mode-abm-then-emp";
    private static final String OFF_CHANNEL = "defense-mode-off";

    /** A real explosive missile fixture with controlled motion, so it remains a radar threat in the test area. */
    private static final class ThreatMissile extends EntityExplosiveMissile
    {
        private final double verticalStep;

        private ThreatMissile(Level level, double verticalStep)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
            this.verticalStep = verticalStep;
        }

        @Override public int getTicksInAir() { return 2; }

        @Override
        public void tick()
        {
            if (verticalStep != 0) setPos(getX(), getY() - verticalStep, getZ());
        }
    }

    private record Fixture(TileRadarStation radar, TileEMPTower tower, ThreatMissile threat) { }

    private Fixture setup(GameTestHelper helper, RadarDefenseMode mode, double height, double speed, String channel)
    {
        BlockPos localRadar = new BlockPos(2, 1, 2);
        BlockPos localTower = new BlockPos(5, 1, 2);
        helper.setBlock(localRadar, ModBlocks.RADAR_STATION);
        helper.setBlock(localTower, ModBlocks.EMP_TOWER);

        BlockPos radarPos = helper.absolutePos(localRadar);
        TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(radarPos);
        TileEMPTower tower = (TileEMPTower) helper.getLevel().getBlockEntity(helper.absolutePos(localTower));
        helper.assertTrue(radar != null && tower != null, "radar and EMP tower block entities must be present");
        radar.setDefenseMode(mode);
        radar.setDetectionRange(40);
        radar.setTriggerRange(32);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
        radar.getRadio().setChannel(channel);
        tower.setRange(60);
        tower.energyStorage.setStored(tower.energyStorage.getCapacity());
        helper.onEachTick(() -> radar.energyStorage.setStored(radar.energyStorage.getCapacity()));
        helper.onEachTick(() -> {
            if (tower.getCooldown() <= 0) tower.energyStorage.setStored(tower.energyStorage.getCapacity());
        });
        tower.getRadio().setChannel(channel);

        ThreatMissile threat = new ThreatMissile(helper.getLevel(), speed);
        threat.explosive.setStack(new ItemStack(
                ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        threat.setPos(radarPos.getX() + 0.5, radarPos.getY() + height, radarPos.getZ() + 0.5);
        threat.setDeltaMovement(new Vec3(0, -speed, 0));
        threat.setNoGravity(true);
        threat.setFreezeMotion(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(threat), "threat missile must spawn");
        RadarRegistry.add(threat);
        helper.assertTrue(tower.isReady(), "charged EMP tower must start ready");
        return new Fixture(radar, tower, threat);
    }

    private void keepThreatRegistered(Fixture fixture)
    {
        if (fixture.threat().isAlive()) RadarRegistry.add(fixture.threat());
    }

    @GameTest(maxTicks = 100, padding = 144)
    public void empOnlyBroadcastsEvenWithAnAbmLauncher(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, RadarDefenseMode.EMP_ONLY, 20, 1, EMP_ONLY_CHANNEL);
        BlockPos launcherPos = helper.absolutePos(new BlockPos(6, 1, 2));
        helper.setBlock(new BlockPos(6, 1, 2), ModBlocks.CRUISE_LAUNCHER);
        TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(launcherPos);
        helper.assertTrue(launcher != null, "ABM launcher block entity must be present");
        launcher.inventory.setItem(0, new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE));
        launcher.energyStorage.setStored(launcher.energyStorage.getCapacity());
        helper.onEachTick(() -> launcher.energyStorage.setStored(launcher.energyStorage.getCapacity()));
        launcher.radio.setChannel(EMP_ONLY_CHANNEL);

        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            if (fixture.tower().getCooldown() <= 0) {
                fixture.tower().energyStorage.setStored(fixture.tower().energyStorage.getCapacity());
            }
            keepThreatRegistered(fixture);
            helper.assertTrue(fixture.tower().getCooldown() > 0, "EMP tower must fire to discharge");
            helper.assertTrue(!launcher.getMissileStack().isEmpty(),
                    "EMP_ONLY must not consume the loaded ABM");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(EntityAntiBallisticMissile.class,
                            new AABB(launcherPos).inflate(100)).isEmpty(),
                    "EMP_ONLY must not launch an ABM");
            helper.assertTrue(fixture.tower().energyStorage.getStored() < fixture.tower().energyStorage.getCapacity(),
                    "EMP_ONLY broadcast must discharge the charged EMP tower");
            fixture.threat().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100, padding = 144)
    public void abmOnlyDoesNotDischargeEmpTower(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, RadarDefenseMode.ABM_ONLY, 20, 1, ABM_ONLY_CHANNEL);
        long charged = fixture.tower().energyStorage.getStored();
        helper.runAfterDelay(ConfigAbm.EMP_FALLBACK_TICKS + 5, () -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            fixture.tower().energyStorage.setStored(fixture.tower().energyStorage.getCapacity());
            keepThreatRegistered(fixture);
            helper.assertTrue(fixture.radar().getTrackTable().get(fixture.threat().getUUID()) != null,
                    "radar must create the expected track");
            helper.assertTrue(fixture.tower().getCooldown() == 0,
                    "ABM_ONLY without an available launcher must not fire the EMP tower");
            helper.assertTrue(fixture.tower().energyStorage.getStored() == charged,
                    "ABM_ONLY must leave EMP tower charge untouched");
            fixture.threat().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 150, padding = 144)
    public void abmThenEmpWaitsForFallbackThreshold(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, RadarDefenseMode.ABM_THEN_EMP, 30, 0.25, ABM_THEN_EMP_CHANNEL);
        final int[] impactTicksAtFirstDischarge = { Integer.MAX_VALUE };
        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            if (fixture.tower().getCooldown() <= 0) {
                fixture.tower().energyStorage.setStored(fixture.tower().energyStorage.getCapacity());
            }
            keepThreatRegistered(fixture);
            var track = fixture.radar().getTrackTable().get(fixture.threat().getUUID());
            if (fixture.tower().getCooldown() <= 0) {
                helper.assertTrue(track == null || track.ticksToImpact > ConfigAbm.EMP_FALLBACK_TICKS
                                || track.ticksToImpact == Integer.MAX_VALUE,
                        "EMP fallback must not fire before the threshold");
                helper.assertTrue(false, "waiting for EMP fallback to fire");
            }
            if (impactTicksAtFirstDischarge[0] == Integer.MAX_VALUE)
                impactTicksAtFirstDischarge[0] = track == null ? Integer.MAX_VALUE : track.ticksToImpact;
            helper.assertTrue(track != null && track.ticksToImpact <= ConfigAbm.EMP_FALLBACK_TICKS,
                    "EMP fallback must fire only once the tracked impact time is at most the configured threshold");
            helper.assertTrue(impactTicksAtFirstDischarge[0] <= ConfigAbm.EMP_FALLBACK_TICKS,
                    "impact time observed at first EMP discharge must be at most the configured threshold");
            fixture.threat().discard();
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100, padding = 144)
    public void offSuppressesDefenseAndPreservesThreatRedstone(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, RadarDefenseMode.OFF, 20, 1, OFF_CHANNEL);
        long charged = fixture.tower().energyStorage.getStored();
        helper.runAfterDelay(ConfigAbm.EMP_FALLBACK_TICKS + 5, () -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            fixture.tower().energyStorage.setStored(fixture.tower().energyStorage.getCapacity());
            keepThreatRegistered(fixture);
            var track = fixture.radar().getTrackTable().get(fixture.threat().getUUID());
            helper.assertTrue(track != null && track.status == icbm.classic.content.abm.radar.TrackStatus.THREAT,
                    "radar must create the expected threat track");
            helper.assertTrue(fixture.tower().getCooldown() == 0,
                    "OFF must not fire the EMP tower");
            helper.assertTrue(fixture.tower().energyStorage.getStored() == charged,
                    "OFF must leave EMP tower charge untouched");
            helper.assertTrue(fixture.radar().getStrongRedstonePower(Direction.UP) > 0,
                    "OFF must preserve redstone threat output");
            fixture.threat().discard();
            helper.succeed();
        });
    }
}
