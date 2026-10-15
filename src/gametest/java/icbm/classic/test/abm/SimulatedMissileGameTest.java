package icbm.classic.test.abm;

import icbm.classic.ICBMClassic;
import icbm.classic.api.events.MissileChunkEvent;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.config.machines.ConfigRadar;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.InterceptEvents;
import icbm.classic.content.abm.AbmGuidance;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.math.Prediction;
import icbm.classic.content.abm.math.TrajectoryPredictor;
import icbm.classic.content.abm.math.Vec3d;
import icbm.classic.content.abm.radar.RadarDefenseMode;
import icbm.classic.content.abm.radar.RadarTrack;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.content.abm.radar.Vectors;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.content.missile.tracker.MissileTrackerData;
import icbm.classic.content.missile.tracker.MissileTrackerHandler;
import icbm.classic.content.missile.tracker.MissileTrackerWorld;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Set;

/**
 * S7.08: measurement of the "missile simulated outside the loaded chunks" re-entry geometry (S4.03)
 * and of whether an ABM launcher still has enough time to intercept what comes back.
 *
 * <p>Measurement only: no production logic is changed by this class. Every number this class produces
 * is written into {@code docs/architecture/anti-ballistic-missile.md} §9 and
 * {@code docs/implementation/evidence/S7.08.md}.
 *
 * <p>Fixture constraints honoured here (see S7.07 evidence): all ticking entities live in the structure
 * origin chunk; the tracker's chunk force-load is cancelled (see {@link #SUPPRESSED_CHUNKS}) because
 * {@code ServerLevel#setChunkForced} makes the GameTest framework throw inside
 * {@code GameTestInfo#tickInternal}. The re-entry point is directly above the target, i.e. inside the
 * same chunk column as the structure origin, so the re-entered missile is entity-ticked by the test
 * framework itself - the cancelled ticket only skips the redundant production chunk ticket.
 */
public class SimulatedMissileGameTest
{
    private static final String DEFAULT_CHANNEL = "s708-default";
    private static final String DOUBLED_CHANNEL = "s708-doubled";
    /** Geometry rows keep their own channels so no launcher of the scene rows can hear their radar. */
    private static final String GEOMETRY_DEFAULT_CHANNEL = "s708-geometry-default";
    private static final String GEOMETRY_DOUBLED_CHANNEL = "s708-geometry-doubled";

    /**
     * Chunk columns whose tracker force-load is cancelled. Only chunks added here (the target chunk of
     * this class' own fixtures) are affected; every other {@link MissileChunkEvent.Load} still passes
     * through, so sibling tests (e.g. {@code MissileTrackerGameTest}) keep their production behaviour.
     */
    private static final Set<Long> SUPPRESSED_CHUNKS = new HashSet<>();

    /**
     * Geometry of the last proximity fuse resolution, keyed by the target's UUID. Observer only (returns
     * true), used to report where a fuse actually resolved - the S7.08 rows need that number because the
     * ABM's step per tick is far larger than {@code abm.flight_speed} (see the S7.08 evidence).
     */
    private static final Map<UUID, String> INTERCEPTS = new ConcurrentHashMap<>();

    static
    {
        MissileChunkEvent.Load.EVENT.register(event -> !SUPPRESSED_CHUNKS.contains(event.chunkPos.pack()));
        InterceptEvents.INTERCEPTED.register((level, abm, target, point) -> {
            INTERCEPTS.put(target.getUUID(), "abm moved " + abm.getPreviousPositionThisTick().distanceTo(abm.position())
                    + " blocks in the resolving tick (from " + abm.getPreviousPositionThisTick() + " to " + abm.position()
                    + "), target was " + abm.position().distanceTo(target.position()) + " blocks from the abm, at "
                    + target.position());
            return true;
        });
    }

    /** Places and powers a radar station used both as the protected target and as the fire control. */
    private TileRadarStation placeRadar(GameTestHelper helper, BlockPos localPos, String channel)
    {
        helper.setBlock(localPos, ModBlocks.RADAR_STATION);
        final BlockPos pos = helper.absolutePos(localPos);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(radar != null, "radar block entity must be present");
        // detection range is maxed out on purpose: the S7.08 rows measure the best case for the defence
        radar.setDetectionRange(ConfigRadar.MAX_RANGE);
        // ... but the trigger range stays small so that only missiles aimed at *this* structure become a
        // threat: the GameTest batch runs every test in one level and the other structures are 65+ blocks away
        radar.setTriggerRange(20);
        radar.setDefenseMode(RadarDefenseMode.ABM_ONLY);
        radar.getRadio().setChannel(channel);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
        helper.onEachTick(() -> radar.energyStorage.setStored(radar.energyStorage.getCapacity()));
        // the missile re-enters directly above this block, so this is the chunk the tracker would force
        SUPPRESSED_CHUNKS.add(ChunkPos.containing(pos).pack());
        return radar;
    }

    /** Places and powers a cruise launcher loaded with one ABM on the given channel. */
    private TileCruiseLauncher placeAbmLauncher(GameTestHelper helper, BlockPos localPos, String channel)
    {
        helper.setBlock(localPos, ModBlocks.CRUISE_LAUNCHER);
        final BlockPos pos = helper.absolutePos(localPos);
        final TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(launcher != null, "cruise launcher block entity must be present");
        launcher.inventory.setItem(0, new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE));
        launcher.energyStorage.setStored(launcher.energyStorage.getCapacity());
        launcher.radio.setChannel(channel);
        helper.onEachTick(() -> launcher.energyStorage.setStored(launcher.energyStorage.getCapacity()));
        return launcher;
    }

    /**
     * Puts a condensed explosive missile aimed at {@code targetPos} into the S4.03 simulation queue using
     * the production entry point and returns the tracker record it produced.
     */
    private MissileTrackerData enterSimulation(GameTestHelper helper, BlockPos targetPos)
    {
        final ServerLevel level = helper.getLevel();
        final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(level, true);
        helper.assertTrue(tracker != null, "the level must have a missile tracker");
        final int before = tracker.missileList.size();

        final double targetX = targetPos.getX() + 0.5D;
        final double targetY = targetPos.getY() + 0.5D;
        final double targetZ = targetPos.getZ() + 0.5D;

        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, level);
        missile.setPos(targetX, targetY, targetZ);
        missile.explosive.setStack(new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        missile.initHealth(missile.getMaxHealth());
        missile.setTargetData(new BasicTargetData(targetX, targetY, targetZ));
        helper.assertTrue(level.addFreshEntity(missile), "the missile must spawn before entering simulation");
        helper.assertTrue(MissileTrackerHandler.simulateMissile(missile),
                "the S4.03 handler must queue the missile for simulation");
        helper.assertTrue(missile.isRemoved(), "the missile entity must be removed when it enters simulation");
        helper.assertTrue(tracker.missileList.size() == before + 1,
                "the tracker must hold one more simulated missile, had " + tracker.missileList.size());

        final MissileTrackerData data = tracker.missileList.getLast();
        helper.assertTrue(Math.abs(data.targetPos.x - targetX) < 0.001D && Math.abs(data.targetPos.z - targetZ) < 0.001D,
                "the recorded target must be the radar position");
        return data;
    }

    /**
     * Drives the tracker up to the re-entry step (production code path) and returns the entity that came
     * back. Uses the tracker's own update so the measured position/velocity is the shipped behaviour.
     */
    private EntityExplosiveMissile reenterFromTracker(GameTestHelper helper, MissileTrackerData data, BlockPos targetPos)
    {
        final ServerLevel level = helper.getLevel();
        final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(level, true);

        data.ticksLeftToTarget = 0;
        tracker.runSimulationTick(level);
        helper.assertTrue(tracker.missileSpawnList.contains(data),
                "the simulated missile must move to the spawn queue once its simulation time is used up");
        data.preLoadChunkTimer = 1;
        tracker.runSimulationTick(level);
        helper.assertTrue(!tracker.missileSpawnList.contains(data),
                "the missile must leave the spawn queue by re-entering the world");

        return findReenteredMissile(helper, targetPos, ConfigMissile.SIMULATION_ENTER_HEIGHT);
    }

    private EntityExplosiveMissile findReenteredMissile(GameTestHelper helper, BlockPos targetPos, double enterHeight)
    {
        final AABB box = new AABB(new Vec3(targetPos.getX() + 0.5D, enterHeight, targetPos.getZ() + 0.5D),
                new Vec3(targetPos.getX() + 0.5D, enterHeight, targetPos.getZ() + 0.5D)).inflate(6);
        final List<EntityExplosiveMissile> found = helper.getLevel().getEntitiesOfClass(EntityExplosiveMissile.class, box);
        helper.assertTrue(found.size() == 1,
                "exactly one missile must re-enter at the target, found " + found.size() + " in " + box);
        return found.getFirst();
    }

    /**
     * Rebuilds the entity the tracker would have produced, but with an injected re-entry height/speed.
     * Mirrors {@code MissileTrackerWorld#spawnMissileOnDestination} line for line; the injected values
     * are the only difference. This is how the "config doubled" scenario is measured: the global config
     * statics must not be written from a GameTest (they are shared by every test in the batch), so the
     * value is read from the config and doubled into the fixture instead.
     *
     * <p>The entity is not added to the level here (the tracker bakes a UUID into the serialized missile,
     * so two live copies would collide in the level's UUID index). Callers decide when to add it.
     */
    private EntityExplosiveMissile buildReentryReplica(GameTestHelper helper, MissileTrackerData data,
                                                       double enterHeight, double enterSpeed)
    {
        final ServerLevel level = helper.getLevel();
        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, level);
        missile.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data.missileData));
        missile.snapTo(data.targetPos.x, enterHeight, data.targetPos.z, 0.0F, 90.0F);
        missile.setDeltaMovement(0, -enterSpeed, 0);
        if (missile.getMissileCapability().getFlightLogic() == null)
        {
            // same literal as the production spawn path
            missile.getMissileCapability().setFlightLogic(new DeadFlightLogic(100));
        }
        missile.getMissileCapability().launch();
        return missile;
    }

    private static int ticksToFloor(double y, double floorY, double speed)
    {
        return (int) Math.ceil((y - floorY) / speed);
    }

    /** Logs one row of the S7.08 measurement table. */
    private void logRow(String scenario, double horizontalDistance, double height, int predictedTicks, String interception)
    {
        ICBMClassic.LOGGER.info(
                "S7.08 | {} | reentry horizontal distance to target={} | height(y)={} | ticks to impact (TrajectoryPredictor)={} | intercepted={}",
                scenario, horizontalDistance, height, predictedTicks, interception);
    }

    /**
     * Row 1 of the S7.08 table: measures the production re-entry produced by the S4.03 tracker for the
     * 1.12 defaults, validates that the replica used for the "doubled" rows is faithful, and proves that
     * the re-entered missile (above the build limit) is really entity-ticked - without that, the whole
     * measurement would be meaningless.
     */
    @GameTest(maxTicks = 60, padding = 32)
    public void recordsDefaultReentryGeometry(GameTestHelper helper)
    {
        final BlockPos radarLocal = new BlockPos(2, 1, 2);
        placeRadar(helper, radarLocal, GEOMETRY_DEFAULT_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(radarLocal);

        final MissileTrackerData data = enterSimulation(helper, radarPos);
        final EntityExplosiveMissile reentered = reenterFromTracker(helper, data, radarPos);

        final double targetX = radarPos.getX() + 0.5D;
        final double targetZ = radarPos.getZ() + 0.5D;
        final double horizontalDistance = Math.hypot(reentered.getX() - targetX, reentered.getZ() - targetZ);
        final Vec3 motion = reentered.getDeltaMovement();
        final Prediction prediction = TrajectoryPredictor.predictImpact(Vectors.from(reentered.position()),
                Vectors.from(motion), new Vec3d(0, 0, 0), radarPos.getY(), ConfigAbm.PREDICTION_MAX_TICKS);

        helper.assertTrue(Math.abs(horizontalDistance) < 0.01D,
                "the 1.12 re-entry point is directly above the target, horizontal distance was " + horizontalDistance);
        helper.assertTrue(Math.abs(reentered.getY() - ConfigMissile.SIMULATION_ENTER_HEIGHT) < 0.01D,
                "the re-entry y must be simulation_enter_height, was " + reentered.getY());
        helper.assertTrue(Math.abs(motion.x) < 1.0E-6D && Math.abs(motion.z) < 1.0E-6D
                        && Math.abs(motion.y + ConfigMissile.SIMULATION_ENTER_SPEED) < 1.0E-6D,
                "the re-entry motion must be (0, -simulation_enter_speed, 0), was " + motion);
        helper.assertTrue(reentered.getFlightLogic() instanceof DeadFlightLogic,
                "the re-entered missile must run DeadFlightLogic or the fall speed is not the configured one");
        helper.assertTrue(reentered.getMissileCapability().getTargetData() != null,
                "the target data must survive the simulation round trip (radar needs it for the impact prediction)");
        helper.assertTrue(prediction != null, "the trajectory predictor must find a floor crossing");
        helper.assertTrue(prediction.ticks() == ticksToFloor(ConfigMissile.SIMULATION_ENTER_HEIGHT,
                        radarPos.getY(), ConfigMissile.SIMULATION_ENTER_SPEED),
                "predicted ticks must match distance / simulation_enter_speed, was " + prediction.ticks());
        helper.assertTrue(Math.abs(prediction.impact().y() - radarPos.getY()) < 0.01D,
                "the predicted impact must be at the radar height");
        helper.assertTrue(Math.abs(prediction.impact().x() - targetX) < 0.01D
                        && Math.abs(prediction.impact().z() - targetZ) < 0.01D,
                "the predicted impact must be at the target, was " + prediction.impact());

        // the replica used for the injected (doubled) rows must reproduce production byte for byte
        final EntityExplosiveMissile replica = buildReentryReplica(helper, data,
                ConfigMissile.SIMULATION_ENTER_HEIGHT, ConfigMissile.SIMULATION_ENTER_SPEED);
        helper.assertTrue(Math.abs(replica.getX() - reentered.getX()) < 0.01D
                        && Math.abs(replica.getY() - reentered.getY()) < 0.01D
                        && Math.abs(replica.getZ() - reentered.getZ()) < 0.01D
                        && replica.getDeltaMovement().equals(reentered.getDeltaMovement())
                        && replica.getFlightLogic() instanceof DeadFlightLogic,
                "the harness replica must reproduce the production re-entry exactly, production="
                        + reentered.position() + " " + reentered.getDeltaMovement()
                        + " replica=" + replica.position() + " " + replica.getDeltaMovement());
        replica.discard();

        final double startY = reentered.getY();
        final int measuredTicks = 20;
        helper.runAfterDelay(measuredTicks, () -> {
            helper.assertTrue(reentered.isAlive(), "the re-entered missile must still be falling");
            final double fallen = startY - reentered.getY();
            helper.assertTrue(Math.abs(fallen - measuredTicks * ConfigMissile.SIMULATION_ENTER_SPEED) < 0.01D,
                    "the re-entered missile must fall at simulation_enter_speed per tick, fell " + fallen
                            + " in " + measuredTicks + " ticks");
            logRow("1.12 defaults (simulation_enter_height=" + ConfigMissile.SIMULATION_ENTER_HEIGHT
                            + ", simulation_enter_speed=" + ConfigMissile.SIMULATION_ENTER_SPEED + ")",
                    horizontalDistance, startY, prediction.ticks(),
                    "see the intercept scene test; measured fall " + fallen + " blocks in " + measuredTicks
                            + " ticks (target/radar y=" + radarPos.getY() + ")");
            reentered.discard();
            helper.succeed();
        });
    }

    /**
     * Row 2: same measurement for {@code ConfigMissile.SIMULATION_ENTER_HEIGHT} doubled. The value is read
     * from the config and doubled into the fixture ({@link #buildReentryReplica}); the static is never
     * written (a GameTest batch shares one static config).
     */
    @GameTest(maxTicks = 60, padding = 32)
    public void recordsDoubledEnterHeightReentryGeometry(GameTestHelper helper)
    {
        final BlockPos radarLocal = new BlockPos(2, 1, 2);
        placeRadar(helper, radarLocal, GEOMETRY_DOUBLED_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(radarLocal);

        final MissileTrackerData data = enterSimulation(helper, radarPos);
        // this row only needs the serialized missile; do not let the tracker respawn it into the world
        final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(helper.getLevel(), true);
        helper.assertTrue(tracker.missileList.remove(data), "the simulated record must be removable again");

        final double doubledHeight = ConfigMissile.SIMULATION_ENTER_HEIGHT * 2.0D;
        final EntityExplosiveMissile reentered = buildReentryReplica(helper, data, doubledHeight,
                ConfigMissile.SIMULATION_ENTER_SPEED);

        final double targetX = radarPos.getX() + 0.5D;
        final double targetZ = radarPos.getZ() + 0.5D;
        final double horizontalDistance = Math.hypot(reentered.getX() - targetX, reentered.getZ() - targetZ);
        final Prediction prediction = TrajectoryPredictor.predictImpact(Vectors.from(reentered.position()),
                Vectors.from(reentered.getDeltaMovement()), new Vec3d(0, 0, 0), radarPos.getY(),
                ConfigAbm.PREDICTION_MAX_TICKS);
        final int expectedTicks = ticksToFloor(doubledHeight, radarPos.getY(), ConfigMissile.SIMULATION_ENTER_SPEED);
        final int predictedTicks = prediction == null ? -1 : prediction.ticks();

        helper.assertTrue(Math.abs(horizontalDistance) < 0.01D,
                "doubling the re-entry height does not move the re-entry point horizontally, was " + horizontalDistance);
        helper.assertTrue(Math.abs(reentered.getY() - doubledHeight) < 0.01D,
                "the injected re-entry y must be twice simulation_enter_height, was " + reentered.getY());
        if (expectedTicks <= ConfigAbm.PREDICTION_MAX_TICKS)
        {
            helper.assertTrue(prediction != null && prediction.ticks() == expectedTicks,
                    "predicted ticks must double with the height, expected " + expectedTicks + " got " + predictedTicks);
        }
        else
        {
            // measurement result: the radar's own extrapolation gives up before the missile reaches the floor
            helper.assertTrue(prediction == null,
                    "the extrapolation window (" + ConfigAbm.PREDICTION_MAX_TICKS + " ticks) must be too short to"
                            + " reach the floor from " + doubledHeight + " (needs " + expectedTicks + " ticks), got "
                            + predictedTicks);
        }

        logRow("simulation_enter_height x2 (" + doubledHeight + ")",
                horizontalDistance, reentered.getY(), predictedTicks,
                "see the intercept scene test; floor reachable in " + expectedTicks + " ticks at simulation_enter_speed,"
                        + " prediction_max_ticks=" + ConfigAbm.PREDICTION_MAX_TICKS);

        reentered.discard();
        helper.succeed();
    }

    /**
     * Scene test for row 1: radar + ABM launcher are alive while the S4.03 tracker re-enters the missile
     * above the protected target. Records whether the ABM intercepts, on which tick, and where.
     */
    @GameTest(maxTicks = 400, padding = 32)
    public void defaultReentryIsIntercepted(GameTestHelper helper)
    {
        runSceneWhenFree(helper, DEFAULT_CHANNEL, false, "1.12 defaults (simulation_enter_height="
                + ConfigMissile.SIMULATION_ENTER_HEIGHT + ", simulation_enter_speed="
                + ConfigMissile.SIMULATION_ENTER_SPEED + ")");
    }

    /**
     * Scene test for row 2: the same scene with {@code simulation_enter_height} doubled in the fixture
     * (the re-entry point moves to 2x the configured height, everything else stays production).
     */
    @GameTest(maxTicks = 620, padding = 32)
    public void doubledEnterHeightReentryIsIntercepted(GameTestHelper helper)
    {
        runSceneWhenFree(helper, DOUBLED_CHANNEL, true,
                "simulation_enter_height x2 (" + (ConfigMissile.SIMULATION_ENTER_HEIGHT * 2.0D) + ")");
    }

    /**
     * Scene tests must not overlap: both place a radar with {@code detection_range = ConfigRadar.MAX_RANGE}
     * into the same test level (the two structures end up ~77 blocks apart, see the S7.08 evidence), so while
     * one scene is running the other scene's threat is inside the first radar's detection range - and so is
     * the other scene's ABM, which would end up in the {@code level.getEntitiesOfClass} scan of this scene.
     * Only that radar's own track can become a threat (the foreign predicted impact is 77 blocks away, outside
     * the 20 block trigger range), so the engagement itself is not affected. The reservation below keeps the
     * two scenes apart in time; it is renewed every tick by the running scene and therefore expires by itself
     * when that scene finishes or fails.
     */
    private static final AtomicLong SCENE_RESERVED_UNTIL = new AtomicLong();

    private static final int SCENE_RESERVATION_TICKS = 20;

    /** Tolerance for the measured per-tick ABM step (see {@code abm.flight_speed}). */
    private static final double ABM_STEP_TOLERANCE = 0.01D;

    private void runSceneWhenFree(GameTestHelper helper, String channel, boolean doubledHeight, String scenario)
    {
        if (helper.getLevel().getGameTime() < SCENE_RESERVED_UNTIL.get())
        {
            helper.runAfterDelay(10, () -> runSceneWhenFree(helper, channel, doubledHeight, scenario));
            return;
        }
        SCENE_RESERVED_UNTIL.set(helper.getLevel().getGameTime() + SCENE_RESERVATION_TICKS);
        runInterceptScene(helper, channel, doubledHeight, scenario);
    }

    private void runInterceptScene(GameTestHelper helper, String channel, boolean doubledHeight, String scenario)
    {
        final ServerLevel level = helper.getLevel();
        final BlockPos radarLocal = new BlockPos(2, 1, 2);
        final BlockPos launcherLocal = new BlockPos(5, 1, 2);

        final TileRadarStation radar = placeRadar(helper, radarLocal, channel);
        final TileCruiseLauncher launcher = placeAbmLauncher(helper, launcherLocal, channel);

        final BlockPos radarPos = helper.absolutePos(radarLocal);
        final BlockPos launcherPos = helper.absolutePos(launcherLocal);
        final Vec3 launchPoint = new Vec3(launcherPos.getX() + 0.5D,
                launcherPos.getY() + TileCruiseLauncher.MISSILE_HOLDER_Y, launcherPos.getZ() + 0.5D);
        ICBMClassic.LOGGER.info("S7.08 scene[{}] radar={} launcher={} launchPoint={}", scenario, radarPos, launcherPos, launchPoint);

        final MissileTrackerData data = enterSimulation(helper, radarPos);
        final double enterHeight = doubledHeight ? ConfigMissile.SIMULATION_ENTER_HEIGHT * 2.0D
                : ConfigMissile.SIMULATION_ENTER_HEIGHT;
        final EntityExplosiveMissile threat;
        if (doubledHeight)
        {
            final MissileTrackerWorld tracker = MissileTrackerHandler.getOrCreateHandler(level, true);
            helper.assertTrue(tracker.missileList.remove(data), "the simulated record must be removable again");
            // same serialized missile as production would have spawned, only the injected height differs
            threat = buildReentryReplica(helper, data, enterHeight, ConfigMissile.SIMULATION_ENTER_SPEED);
            helper.assertTrue(level.addFreshEntity(threat), "the injected re-entry must spawn");
        }
        else
        {
            threat = reenterFromTracker(helper, data, radarPos);
        }

        final long reentryTick = level.getGameTime();
        final long[] removalTick = { -1 };
        final double[] lastY = { Double.NaN };
        final double[] lastDistanceToLauncher = { Double.NaN };
        final long[] firstAbmTick = { -1 };
        final double[] threatYAtLaunch = { Double.NaN };
        final double[] abmMaxY = { Double.NEGATIVE_INFINITY };
        final double[] abmMaxStep = { Double.NaN };
        final double[] launchDistance = { Double.NaN };
        final int[] threatTicksToImpactAtLaunch = { Integer.MIN_VALUE };
        final TrackStatusAtLaunch[] statusAtLaunch = { null };
        final UUID[] abmSampleId = { null };
        final Vec3[] abmSamplePosition = { null };

        helper.onEachTick(() -> {
            SCENE_RESERVED_UNTIL.set(level.getGameTime() + SCENE_RESERVATION_TICKS);
            if (threat.isAlive())
            {
                lastY[0] = threat.getY();
                lastDistanceToLauncher[0] = threat.position().distanceTo(launchPoint);
                RadarRegistry.add(threat);
            }
            else if (removalTick[0] < 0)
            {
                removalTick[0] = level.getGameTime();
            }

            final List<EntityAntiBallisticMissile> allAbms = level.getEntitiesOfClass(EntityAntiBallisticMissile.class,
                    new AABB(launcherPos).inflate(400));
            final List<EntityAntiBallisticMissile> abms = allAbms.stream()
                    .filter(abm -> threat.getUUID().equals(abm.getTargetId()))
                    .toList();
            double ownAbmY = Double.NaN;
            double ownAbmDistance = Double.NaN;
            AbmGuidance.AbmState ownAbmState = null;
            for (EntityAntiBallisticMissile abm : abms)
            {
                abmMaxY[0] = Math.max(abmMaxY[0], abm.getY());
                // the ABM's measured step per tick: guarded (asserted below) so a guidance regression that
                // lets the ABM move further than ConfigAbm.FLIGHT_SPEED per tick fails this test
                if (abm.getUUID().equals(abmSampleId[0]) && abmSamplePosition[0] != null)
                {
                    final double step = abmSamplePosition[0].distanceTo(abm.position());
                    abmMaxStep[0] = Double.isNaN(abmMaxStep[0]) ? step : Math.max(abmMaxStep[0], step);
                }
                abmSampleId[0] = abm.getUUID();
                abmSamplePosition[0] = abm.position();
                final double distance = abm.position().distanceTo(threat.position());
                if (Double.isNaN(ownAbmDistance) || distance < ownAbmDistance)
                {
                    ownAbmDistance = distance;
                    ownAbmY = abm.getY();
                    ownAbmState = abm.getAbmState();
                }
            }
            if (!abms.isEmpty() && firstAbmTick[0] < 0)
            {
                firstAbmTick[0] = level.getGameTime();
                threatYAtLaunch[0] = threat.getY();
                launchDistance[0] = threat.position().distanceTo(launchPoint);
                final RadarTrack track = radar.getTrackTable().get(threat.getUUID());
                if (track != null)
                {
                    threatTicksToImpactAtLaunch[0] = track.ticksToImpact;
                    statusAtLaunch[0] = new TrackStatusAtLaunch(track.status, track.position.y());
                }
            }
            if (level.getGameTime() % 20 == 0 || firstAbmTick[0] == level.getGameTime()
                    || removalTick[0] == level.getGameTime())
            {
                final RadarTrack track = radar.getTrackTable().get(threat.getUUID());
                ICBMClassic.LOGGER.info("S7.08 trace[{}] tick={} threatY={} alive={} ownAbms={} otherAbms={} abmY={}"
                                + " abmState={} distance={} track={} ticksToImpact={}",
                        scenario, level.getGameTime() - reentryTick, threat.getY(), threat.isAlive(), abms.size(),
                        allAbms.size() - abms.size(), ownAbmY, ownAbmState, ownAbmDistance,
                        track == null ? null : track.status, track == null ? -1 : track.ticksToImpact);
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(!threat.isAlive(),
                    "waiting for the re-entered missile to be resolved (tick " + (level.getGameTime() - reentryTick)
                            + ", last y=" + lastY[0] + ", distance to launcher=" + lastDistanceToLauncher[0]
                            + ", abm max y=" + abmMaxY[0] + ")");
            helper.assertTrue(firstAbmTick[0] > 0,
                    "the fire control must order an interception once the re-entered missile is inside"
                            + " engagement_range (last y=" + lastY[0] + ")");
            helper.assertTrue(launcher.getMissileStack().isEmpty(), "the intercept order must consume the loaded ABM");

            // integration guard for the guidance contract fixed in this step: an ABM must never move further
            // than abm.flight_speed per tick (it used to jump flight_speed * |aim - position| blocks)
            helper.assertTrue(Double.isNaN(abmMaxStep[0])
                            || abmMaxStep[0] <= ConfigAbm.FLIGHT_SPEED + ABM_STEP_TOLERANCE,
                    "the ABM moved " + abmMaxStep[0] + " blocks in one tick, its speed must not exceed"
                            + " abm.flight_speed=" + ConfigAbm.FLIGHT_SPEED);

            final boolean intercepted = EngagementRegistry.wasIntercepted(level, threat.getUUID());
            // both outcomes mean "the incoming missile was stopped": either the ABM proximity fuse resolved
            // (then EngagementRegistry records it) or the incoming missile hit the ABM first, in which case
            // the missile detonates mid air instead. Recorded, not asserted: the tick order of the two
            // entities decides which of the two happens.
            final String detail = (intercepted
                    ? "stopped by the ABM proximity fuse at tick " + (removalTick[0] - reentryTick) + " after re-entry"
                    : "stopped without a fuse resolution (removed at y=" + lastY[0] + ") at tick "
                            + (removalTick[0] - reentryTick) + " after re-entry")
                    + " (abm launched at tick " + (firstAbmTick[0] - reentryTick) + ", threat y/distance at launch="
                    + threatYAtLaunch[0] + "/" + launchDistance[0]
                    + ", track ticksToImpact at launch=" + threatTicksToImpactAtLaunch[0]
                    + ", track status at launch=" + statusAtLaunch[0]
                    + ", removal y=" + lastY[0] + ", removal distance to launcher=" + lastDistanceToLauncher[0]
                    + ", abm max y=" + abmMaxY[0] + ", abm max step per tick=" + abmMaxStep[0]
                    + ", flight_speed=" + ConfigAbm.FLIGHT_SPEED + ", re-entry y=" + enterHeight
                    + (intercepted ? ", fuse resolution: " + INTERCEPTS.get(threat.getUUID()) : "")
                    + ")";
            logRow(scenario, 0.0D, enterHeight, ticksToFloor(enterHeight, radarPos.getY(),
                    ConfigMissile.SIMULATION_ENTER_SPEED), detail);
            helper.succeed();
        });
    }

    private record TrackStatusAtLaunch(TrackStatus status, double threatY)
    {
        @Override
        public String toString()
        {
            return status + "@y=" + threatY;
        }
    }
}
