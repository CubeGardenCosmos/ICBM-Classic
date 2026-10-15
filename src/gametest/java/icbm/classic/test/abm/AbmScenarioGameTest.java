package icbm.classic.test.abm;

import icbm.classic.ICBMClassic;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.abm.AbmGuidance;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.InterceptEvents;
import icbm.classic.content.abm.radar.RadarDefenseMode;
import icbm.classic.content.abm.radar.RadarTrack;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * S7.10: the ABM scenario matrix of the design document (§11). Every scenario places its own blocks (no
 * structure files) and asserts only deterministic outcomes.
 *
 * <p><b>Scale of the scene geometry (S7.07 constraint).</b> Only the chunk column of the structure origin is
 * entity ticking, so every entity that has to tick (the incoming missile, the ABM and its flight path) must
 * stay inside it: measured usable range is {@code local ≈ -1..16}, practically {@code |local| ≲ 6}
 * (see {@code docs/implementation/evidence/S7.07.md}). {@code @GameTest(padding=...)} only grows the test
 * area, and {@code ServerLevel#setChunkForced} breaks the framework, so neither can be used to reach the
 * "80 blocks horizontal" of §11. The scale used here:
 *
 * <ul>
 *   <li>height: §11's <b>60 blocks</b> is kept unchanged - the y axis of the origin chunk column is free;</li>
 *   <li>horizontal: §11's <b>80 blocks reduced to 4</b> - the incoming missile therefore approaches the
 *       protected block from 4 blocks east instead of 80, which keeps the 3D approach distance inside
 *       {@code abm.engagement_range} (200) and the predicted impact inside the radar trigger range;</li>
 *   <li>approach speed: 1 block/tick (constant, fixture driven), which leaves the interception itself
 *       (ABM boost + guidance) unchanged - the guidance fix of S7.08 is exercised end to end.</li>
 * </ul>
 *
 * <p><b>Determinism.</b> S7.08 §8 measured that "who killed the incoming missile" depends on the entity tick
 * order of the same tick: the ABM proximity fuse (which writes {@code EngagementRegistry} and fires
 * {@link InterceptEvents#INTERCEPTED}) or the incoming missile's own entity impact (which does not). The
 * fixture threat below never runs the inherited projectile tick, so it cannot raytrace into the ABM and the
 * proximity fuse is the only resolution path. That is what makes §11's "INTERCEPTED fires exactly once"
 * assertable instead of merely recordable.
 */
public class AbmScenarioGameTest
{
    private static final String SINGLE_CHANNEL = "abm-scene-single";
    private static final String SALVO_CHANNEL = "abm-scene-salvo";
    private static final String OUTSIDE_CHANNEL = "abm-scene-outside";
    private static final String LOST_TARGET_CHANNEL = "abm-scene-lost-target";
    private static final String DEDUP_A_CHANNEL = "abm-scene-dedup-a";
    private static final String DEDUP_B_CHANNEL = "abm-scene-dedup-b";

    private static final BlockPos RADAR_LOCAL = new BlockPos(2, 1, 2);
    private static final BlockPos RADAR_B_LOCAL = new BlockPos(5, 1, 2);
    private static final BlockPos LAUNCHER_LOCAL = new BlockPos(2, 1, 5);
    private static final BlockPos LAUNCHER_B_LOCAL = new BlockPos(5, 1, 5);

    /** §11: incoming missile 60 blocks above the protected block. Free: the chunk column ticks at any y. */
    private static final double APPROACH_HEIGHT = 60.0D;
    /** §11's 80 blocks horizontal, reduced to the entity ticking chunk column (see the class javadoc). */
    private static final double APPROACH_OFFSET_X = 4.0D;
    /** Constant fixture approach speed in blocks per tick. */
    private static final double APPROACH_SPEED = 1.0D;
    /** §11 budgets 400 ticks for the single target scene. */
    private static final int INTERCEPT_BUDGET_TICKS = 400;
    /** Scans observed by the "impact outside the protection radius" scene before it may pass. */
    private static final int NO_ENGAGE_WINDOW_TICKS = 80;
    /** Fuel injected into the fuel exhaustion fixture (default is {@code abm.fuel_ticks} = 200). */
    private static final int INJECTED_FUEL_TICKS = 14;
    private static final int FUEL_TICK_SLACK = 3;
    /** Slack over {@code abm.reacquire_timeout_ticks} for the state machine bookkeeping ticks. */
    private static final int REACQUIRE_SLACK_TICKS = 10;

    /** Launch orders seen per target id; {@code ABM_LAUNCHED} fires once per launcher that answers an order. */
    private static final Map<UUID, AtomicInteger> LAUNCHES = new ConcurrentHashMap<>();
    /** Proximity fuse resolutions per target id ({@code INTERCEPTED} may be cancelled; this only counts). */
    private static final Map<UUID, AtomicInteger> INTERCEPTS = new ConcurrentHashMap<>();

    static
    {
        InterceptEvents.ABM_LAUNCHED.register((level, abm, targetId, launcherPos) -> {
            if (targetId != null) LAUNCHES.computeIfAbsent(targetId, ignored -> new AtomicInteger()).incrementAndGet();
        });
        InterceptEvents.INTERCEPTED.register((level, abm, target, point) -> {
            INTERCEPTS.computeIfAbsent(target.getUUID(), ignored -> new AtomicInteger()).incrementAndGet();
            return true;
        });
    }

    /**
     * Fixture threat: a real explosive missile entity (so {@code TileRadarStation#isThreat} accepts it) whose
     * motion is driven by the test. {@code reportedVelocity} is what the radar reads and extrapolates the
     * impact from, {@code step} is the motion the fixture applies - the two differ only in the scene that
     * needs a trajectory crossing the protection radius sideways. The inherited projectile tick is not run,
     * so the threat cannot raytrace into the ABM (see the class javadoc).
     */
    private static final class SceneThreat extends EntityExplosiveMissile
    {
        private final Vec3 step;

        private SceneThreat(Level level, Vec3 position, Vec3 reportedVelocity, Vec3 step)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
            this.step = step;
            explosive.setStack(new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
            setPos(position);
            setDeltaMovement(reportedVelocity);
            setNoGravity(true);
            setFreezeMotion(true);
        }

        @Override
        public int getTicksInAir()
        {
            return 2;
        }

        @Override
        public void tick()
        {
            // the engine snapshots the old position in Entity#commonTick before tick(), which is what the
            // proximity fuse reads as prevPosition; move explicitly instead of running the projectile logic
            if (step.lengthSqr() > 0) setPos(position().add(step));
        }
    }

    private static TileRadarStation placeRadar(GameTestHelper helper, BlockPos local, String channel,
                                               int detectionRange, int triggerRange)
    {
        helper.setBlock(local, ModBlocks.RADAR_STATION);
        final BlockPos pos = helper.absolutePos(local);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(radar != null, "radar station block entity must be present");
        radar.setDefenseMode(RadarDefenseMode.ABM_ONLY);
        radar.setDetectionRange(detectionRange);
        radar.setTriggerRange(triggerRange);
        radar.getRadio().setChannel(channel);
        power(radar);
        helper.onEachTick(() -> power(radar));
        return radar;
    }

    private static TileCruiseLauncher placeAbmLauncher(GameTestHelper helper, BlockPos local, String channel)
    {
        helper.setBlock(local, ModBlocks.CRUISE_LAUNCHER);
        final BlockPos pos = helper.absolutePos(local);
        final TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(launcher != null, "cruise launcher block entity must be present");
        launcher.inventory.setItem(0, new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE));
        launcher.radio.setChannel(channel);
        powerLauncher(launcher);
        helper.onEachTick(() -> powerLauncher(launcher));
        return launcher;
    }

    /** A radar charged once holds 20 ticks ({@code radar.power_capacity / radar.power_cost}), so top it up. */
    private static void power(TileRadarStation radar)
    {
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
    }

    private static void powerLauncher(TileCruiseLauncher launcher)
    {
        launcher.energyStorage.setStored(launcher.energyStorage.getCapacity());
    }

    /** Block center of a local position, in absolute coordinates. */
    private static Vec3 center(GameTestHelper helper, BlockPos local)
    {
        final BlockPos pos = helper.absolutePos(local);
        return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private static SceneThreat spawnThreat(GameTestHelper helper, Vec3 position, Vec3 reportedVelocity, Vec3 step)
    {
        final SceneThreat threat = new SceneThreat(helper.getLevel(), position, reportedVelocity, step);
        helper.assertTrue(helper.getLevel().addFreshEntity(threat), "the fixture threat must spawn");
        RadarRegistry.add(threat);
        return threat;
    }

    /**
     * The §11 approach: a missile 60 blocks above the protected block, flying straight at it. The fixture
     * uses a constant velocity so the radar extrapolation lands on the protected block (trigger range) and
     * the ABM has to climb and meet it in the air.
     */
    private static SceneThreat spawnApproachThreat(GameTestHelper helper, BlockPos radarPos)
    {
        final Vec3 protection = new Vec3(radarPos.getX() + 0.5D, radarPos.getY() + 0.5D, radarPos.getZ() + 0.5D);
        final Vec3 spawn = new Vec3(protection.x + APPROACH_OFFSET_X, protection.y + APPROACH_HEIGHT, protection.z);
        final Vec3 step = protection.subtract(spawn).normalize().scale(APPROACH_SPEED);
        return spawnThreat(helper, spawn, step, step);
    }

    /** One evidence row per scenario; the S7.10 evidence quotes these lines. */
    private static void logScene(String scene, String detail)
    {
        ICBMClassic.LOGGER.info("S7.10 | {} | {}", scene, detail);
    }

    private static int launchCount(UUID targetId)
    {
        final AtomicInteger count = LAUNCHES.get(targetId);
        return count == null ? 0 : count.get();
    }

    private static int interceptCount(UUID targetId)
    {
        final AtomicInteger count = INTERCEPTS.get(targetId);
        return count == null ? 0 : count.get();
    }

    private static int emptyLaunchers(List<TileCruiseLauncher> launchers)
    {
        return (int) launchers.stream().filter(launcher -> launcher.getMissileStack().isEmpty()).count();
    }

    private static boolean tracksThreat(TileRadarStation radar, UUID targetId)
    {
        final RadarTrack track = radar.getTrackTable().get(targetId);
        return track != null && (track.status == TrackStatus.THREAT || track.status == TrackStatus.ENGAGED);
    }

    private static EntityAntiBallisticMissile ownAbm(ServerLevel level, BlockPos near, UUID targetId)
    {
        return level.getEntitiesOfClass(EntityAntiBallisticMissile.class, new AABB(near).inflate(128)).stream()
                .filter(abm -> targetId.equals(abm.getTargetId()))
                .findFirst().orElse(null);
    }

    /**
     * §11 "单目标拦截": radar + one ABM launcher. The incoming missile must be destroyed within the §11 budget,
     * the protected block must survive, the launcher must consume its ABM and the interception must be
     * registered exactly once. (The frozen-threat half of the same message path is covered by
     * {@code LauncherInterceptGameTest#cruiseLauncherRespondsToRadarInterceptOrder}; this scene adds the
     * flying missile, the §11 geometry, the tick budget and the block damage assertion.)
     */
    @GameTest(maxTicks = 600, padding = 32)
    public void singleIncomingMissileIsInterceptedWithoutRadarDamage(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final TileRadarStation radar = placeRadar(helper, RADAR_LOCAL, SINGLE_CHANNEL, 64, 20);
        final TileCruiseLauncher launcher = placeAbmLauncher(helper, LAUNCHER_LOCAL, SINGLE_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(RADAR_LOCAL);

        final SceneThreat threat = spawnApproachThreat(helper, radarPos);
        final UUID threatId = threat.getUUID();
        final long startTick = level.getGameTime();
        final double[] removalY = { Double.NaN };
        final long[] launchTick = { -1 };

        helper.succeedWhen(() -> {
            power(radar);
            powerLauncher(launcher);
            RadarRegistry.add(threat);
            if (launchTick[0] < 0 && launchCount(threatId) > 0) launchTick[0] = level.getGameTime();
            if (!threat.isAlive() && Double.isNaN(removalY[0])) removalY[0] = threat.getY();

            helper.assertTrue(!threat.isAlive(), "the incoming missile must be destroyed (tick "
                    + (level.getGameTime() - startTick) + ", launched at " + launchTick[0] + ")");
            helper.assertTrue(launchCount(threatId) == 1,
                    "the fire control must order exactly one interceptor, ordered " + launchCount(threatId));
            helper.assertTrue(interceptCount(threatId) == 1,
                    "the proximity fuse must resolve exactly once, resolved " + interceptCount(threatId));
            helper.assertTrue(EngagementRegistry.wasIntercepted(level, threatId),
                    "the interception must be recorded in the engagement registry");
            helper.assertTrue(level.getGameTime() - startTick <= INTERCEPT_BUDGET_TICKS,
                    "§11 budgets " + INTERCEPT_BUDGET_TICKS + " ticks for this scene, the missile was destroyed after "
                            + (level.getGameTime() - startTick));
            helper.assertTrue(launcher.getMissileStack().isEmpty(),
                    "the intercept order must consume the loaded ABM");
            helper.assertTrue(removalY[0] > radarPos.getY() + 5.0D,
                    "the missile must be resolved in the air, it was removed at y=" + removalY[0]
                            + " over the protected block at y=" + radarPos.getY());
            helper.assertBlockPresent(ModBlocks.RADAR_STATION, RADAR_LOCAL);
            logScene("single target", "launched at tick " + (launchTick[0] - startTick) + ", destroyed at tick "
                    + (level.getGameTime() - startTick) + ", resolution y=" + removalY[0] + " (protected block y="
                    + radarPos.getY() + "), launches=" + launchCount(threatId) + ", fuse resolutions="
                    + interceptCount(threatId) + ", budget=" + INTERCEPT_BUDGET_TICKS);
            helper.succeed();
        });
    }

    /**
     * §11 "不越权齐射": the same scene with three launchers. {@code abm.max_interceptors_per_target} must keep
     * the salvo at one interceptor, and the fire control must really have had a decision while that
     * interceptor was in flight (otherwise the scene would pass vacuously).
     */
    @GameTest(maxTicks = 600, padding = 32)
    public void threeLaunchersCommitAtMostOneInterceptorPerTarget(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final TileRadarStation radar = placeRadar(helper, RADAR_LOCAL, SALVO_CHANNEL, 64, 20);
        final List<TileCruiseLauncher> launchers = List.of(
                placeAbmLauncher(helper, new BlockPos(2, 1, 5), SALVO_CHANNEL),
                placeAbmLauncher(helper, new BlockPos(4, 1, 5), SALVO_CHANNEL),
                placeAbmLauncher(helper, new BlockPos(5, 1, 7), SALVO_CHANNEL));
        final BlockPos radarPos = helper.absolutePos(RADAR_LOCAL);

        final SceneThreat threat = spawnApproachThreat(helper, radarPos);
        final UUID threatId = threat.getUUID();
        final int[] maxActive = { 0 };
        final int[] decisionsWithInterceptorInFlight = { 0 };

        helper.succeedWhen(() -> {
            power(radar);
            launchers.forEach(AbmScenarioGameTest::powerLauncher);
            RadarRegistry.add(threat);

            final int active = EngagementRegistry.activeCount(level, threatId);
            maxActive[0] = Math.max(maxActive[0], active);
            if (active > 0 && level.getGameTime() % Math.max(1, ConfigAbm.FIRE_CONTROL_INTERVAL_TICKS) == 0)
                decisionsWithInterceptorInFlight[0]++;

            helper.assertTrue(!threat.isAlive(), "the incoming missile must be destroyed");
            helper.assertTrue(launchCount(threatId) == 1, "max_interceptors_per_target="
                    + ConfigAbm.MAX_INTERCEPTORS_PER_TARGET + " must limit the salvo to one interceptor, ordered "
                    + launchCount(threatId));
            helper.assertTrue(maxActive[0] <= ConfigAbm.MAX_INTERCEPTORS_PER_TARGET,
                    "active interceptors per target must stay at or below max_interceptors_per_target="
                            + ConfigAbm.MAX_INTERCEPTORS_PER_TARGET + ", peaked at " + maxActive[0]);
            helper.assertTrue(decisionsWithInterceptorInFlight[0] >= 1,
                    "the fire control must have had a decision tick while the interceptor was in flight, otherwise"
                            + " the salvo limit is not exercised");
            helper.assertTrue(emptyLaunchers(launchers) == 1,
                    "exactly one launcher must fire, fired " + emptyLaunchers(launchers) + " of " + launchers.size());
            helper.assertTrue(interceptCount(threatId) == 1,
                    "the proximity fuse must resolve exactly once, resolved " + interceptCount(threatId));
            logScene("salvo limit", "launchers=" + launchers.size() + ", launches=" + launchCount(threatId)
                    + ", emptied launchers=" + emptyLaunchers(launchers) + ", decisions with an interceptor in flight="
                    + decisionsWithInterceptorInFlight[0] + ", peak active=" + maxActive[0]
                    + ", max_interceptors_per_target=" + ConfigAbm.MAX_INTERCEPTORS_PER_TARGET);
            helper.succeed();
        });
    }

    /**
     * §11 "落点在外不交战": the incoming missile crosses the area sideways, so the extrapolated impact is
     * outside the protection radius. A loaded launcher is standing by on the channel and the fire control is
     * running, so this scene asserts the "no ABM is launched" half that
     * {@code RadarTrackGameTest#sidewaysMissileOutsideProtectionIsDetected} does not cover (it has no launcher).
     */
    @GameTest(maxTicks = 600, padding = 32)
    public void impactOutsideProtectionRangeIsDetectedButNotEngaged(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final TileRadarStation radar = placeRadar(helper, RADAR_LOCAL, OUTSIDE_CHANNEL, 48, 10);
        final TileCruiseLauncher launcher = placeAbmLauncher(helper, LAUNCHER_LOCAL, OUTSIDE_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(RADAR_LOCAL);

        // reported velocity crosses the protected block sideways: extrapolated the missile lands ~40 blocks
        // east of it. The entity itself does not move, which keeps the whole scene inside the entity ticking
        // chunk column (same fixture shape as the radar track test above).
        final SceneThreat threat = spawnThreat(helper, center(helper, RADAR_LOCAL.offset(3, 40, 0)),
                new Vec3(1, -1, 0), Vec3.ZERO);
        final UUID threatId = threat.getUUID();
        final int[] observedScans = { 0 };

        helper.succeedWhen(() -> {
            power(radar);
            powerLauncher(launcher);
            RadarRegistry.add(threat);

            final RadarTrack track = radar.getTrackTable().get(threatId);
            if (track != null)
            {
                observedScans[0]++;
                helper.assertTrue(track.status == TrackStatus.DETECTED,
                        "an impact outside the protection radius must stay DETECTED, the track was " + track.status);
            }
            helper.assertTrue(launchCount(threatId) == 0,
                    "no interceptor may be ordered for an impact outside the protection radius, ordered "
                            + launchCount(threatId));
            helper.assertTrue(level.getEntitiesOfClass(EntityAntiBallisticMissile.class,
                            new AABB(radarPos).inflate(64)).isEmpty(),
                    "no ABM may exist for a track outside the protection radius");
            helper.assertTrue(!launcher.getMissileStack().isEmpty(),
                    "the standing by launcher must keep its loaded ABM");
            helper.assertTrue(observedScans[0] >= NO_ENGAGE_WINDOW_TICKS, "observing the negative scene ("
                    + observedScans[0] + "/" + NO_ENGAGE_WINDOW_TICKS + " scans, last status "
                    + (track == null ? null : track.status) + ")");
            threat.discard();
            logScene("impact outside protection radius", "observed " + observedScans[0] + " scans, track status stayed "
                    + (track == null ? null : track.status) + ", impact=" + (track == null ? null : track.predictedImpact)
                    + ", launches=" + launchCount(threatId) + ", launcher still loaded=" + !launcher.getMissileStack().isEmpty());
            helper.succeed();
        });
    }

    /**
     * §11 "燃料耗尽自毁", fuel path: an ABM in its guidance phase whose {@code abm.fuel_ticks} run out must
     * self-destruct in the air and leave its target alone. The default budget (200 ticks) is far longer than
     * this scene's flight, so the fixture drains the fuel through the public API after spawning - no global
     * config is written (a GameTest batch shares one {@code ConfigAbm}).
     */
    @GameTest(maxTicks = 600, padding = 64)
    public void abmSelfDestructsWhenFuelRunsOut(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final BlockPos floorLocal = new BlockPos(3, 1, 3);
        helper.setBlock(floorLocal, Blocks.STONE);
        final BlockPos floorAbs = helper.absolutePos(floorLocal);

        final SceneThreat target = spawnThreat(helper, center(helper, new BlockPos(3, 41, 3)),
                new Vec3(0, -1, 0), Vec3.ZERO);
        final EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(level);
        abm.setPos(floorAbs.getX() + 0.5D, floorAbs.getY() + 1.0D, floorAbs.getZ() + 0.5D);
        helper.assertTrue(level.addFreshEntity(abm), "the fixture ABM must spawn");
        while (abm.getFuelLeft() > INJECTED_FUEL_TICKS) abm.decrementFuel();
        abm.setTargetId(target.getUUID());
        final int injectedFuel = abm.getFuelLeft();

        final long startTick = level.getGameTime();
        final double[] removalY = { Double.NaN };
        final boolean[] sawGuidance = { false };
        final double[] targetDistanceAtRemoval = { Double.NaN };

        helper.succeedWhen(() -> {
            if (!abm.isRemoved())
            {
                if (abm.getAbmState() == AbmGuidance.AbmState.GUIDE) sawGuidance[0] = true;
            }
            else if (Double.isNaN(removalY[0]))
            {
                removalY[0] = abm.getY();
                targetDistanceAtRemoval[0] = abm.distanceTo(target);
            }

            helper.assertTrue(abm.isRemoved(), "an ABM with " + injectedFuel + " fuel ticks must self-destruct"
                    + " (fuel left=" + abm.getFuelLeft() + ", state=" + abm.getAbmState()
                    + ", tick=" + (level.getGameTime() - startTick) + ")");
            helper.assertTrue(sawGuidance[0], "the fuel must run out during the guidance phase, not during boost");
            final long flownTicks = level.getGameTime() - startTick;
            helper.assertTrue(flownTicks >= injectedFuel && flownTicks <= injectedFuel + FUEL_TICK_SLACK,
                    "the ABM must fly for its fuel budget (" + injectedFuel + " ticks), it flew " + flownTicks);
            helper.assertTrue(targetDistanceAtRemoval[0] > ConfigAbm.PROXIMITY_RADIUS,
                    "the self-destruct must not be a proximity fuse resolution, the target was "
                            + targetDistanceAtRemoval[0] + " blocks away");
            helper.assertTrue(target.isAlive(), "the fuel-exhausted ABM must leave its target alive");
            helper.assertTrue(removalY[0] > floorAbs.getY() + 4.0D,
                    "the ABM must self-destruct in flight, it was removed at y=" + removalY[0]
                            + " over the ground at y=" + floorAbs.getY());
            helper.assertBlockState(floorLocal, state -> state.is(Blocks.STONE),
                    state -> Component.literal("the self-destruct explosion must not damage blocks: " + state));
            logScene("fuel exhaustion", "injected fuel=" + injectedFuel + " (default " + ConfigAbm.FUEL_TICKS
                    + "), flew " + flownTicks + " ticks, gave up at y=" + removalY[0] + " (ground y="
                    + floorAbs.getY() + "), target distance at removal=" + targetDistanceAtRemoval[0]
                    + ", target still alive=" + target.isAlive());
            helper.succeed();
        });
    }

    /**
     * §11 "燃料耗尽自毁", as written in the design: an ABM is launched by the fire control and its target is
     * removed right afterwards. With no other threat inside {@code abm.reacquire_range} the ABM must give up
     * after {@code abm.reacquire_timeout_ticks} and self-destruct instead of flying on or landing.
     * ({@code AbmGuidanceGameTest#noTargetReacquiresThenSelfDestructsWithoutBlockDamage} covers the variant
     * where the ABM never had a target; this scene covers the launch-then-lose transition.)
     */
    @GameTest(maxTicks = 600, padding = 64)
    public void interceptorSelfDestructsWhenItsTargetIsLost(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final TileRadarStation radar = placeRadar(helper, RADAR_LOCAL, LOST_TARGET_CHANNEL, 64, 20);
        final TileCruiseLauncher launcher = placeAbmLauncher(helper, LAUNCHER_LOCAL, LOST_TARGET_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(RADAR_LOCAL);

        final SceneThreat threat = spawnApproachThreat(helper, radarPos);
        final UUID threatId = threat.getUUID();
        final long[] lostTick = { -1 };
        final long[] reacquireTick = { -1 };
        final long[] removalTick = { -1 };
        final double[] removalY = { Double.NaN };
        final EntityAntiBallisticMissile[] interceptor = { null };
        final double[] targetDistanceAtRemoval = { Double.NaN };

        helper.succeedWhen(() -> {
            power(radar);
            powerLauncher(launcher);

            if (threat.isAlive())
            {
                RadarRegistry.add(threat);
                if (launchCount(threatId) > 0)
                {
                    // the design's scene: the target is gone the moment the interceptor is in the air
                    threat.discard();
                    lostTick[0] = level.getGameTime();
                }
                helper.assertTrue(false, "waiting for the fire control to launch an interceptor");
            }

            helper.assertTrue(lostTick[0] > 0,
                    "the target must be removed after an intercept order was issued, but none was issued");

            // the ABM entity is only returned by the level lookup while it is alive, so keep the reference
            if (interceptor[0] == null) interceptor[0] = ownAbm(level, radarPos, threatId);
            helper.assertTrue(interceptor[0] != null,
                    "the launched interceptor must be found while it is still alive");
            if (!interceptor[0].isRemoved())
            {
                if (interceptor[0].getAbmState() == AbmGuidance.AbmState.REACQUIRE && reacquireTick[0] < 0)
                    reacquireTick[0] = level.getGameTime();
            }
            else if (removalTick[0] < 0)
            {
                removalTick[0] = level.getGameTime();
                removalY[0] = interceptor[0].getY();
                targetDistanceAtRemoval[0] = interceptor[0].distanceTo(threat);
            }

            helper.assertTrue(interceptor[0].isRemoved(), "the interceptor must give up after losing its target"
                    + " (state=" + interceptor[0].getAbmState() + ", fuel left=" + interceptor[0].getFuelLeft()
                    + ", tick=" + (level.getGameTime() - lostTick[0]) + " after the target was lost)");
            helper.assertTrue(reacquireTick[0] > 0, "the interceptor must enter REACQUIRE after losing its target");
            helper.assertTrue(removalTick[0] - reacquireTick[0] <= ConfigAbm.REACQUIRE_TIMEOUT_TICKS + 3,
                    "the interceptor must give up after reacquire_timeout_ticks=" + ConfigAbm.REACQUIRE_TIMEOUT_TICKS
                            + ", it took " + (removalTick[0] - reacquireTick[0]) + " ticks of REACQUIRE");
            helper.assertTrue(removalTick[0] - lostTick[0] <= ConfigAbm.BOOST_TICKS
                            + ConfigAbm.REACQUIRE_TIMEOUT_TICKS + REACQUIRE_SLACK_TICKS,
                    "the whole give-up sequence must stay inside boost + reacquire_timeout, it took "
                            + (removalTick[0] - lostTick[0]) + " ticks after the target was lost");
            helper.assertTrue(removalY[0] > radarPos.getY() + 2.0D,
                    "the interceptor must self-destruct in the air instead of landing, it was removed at y="
                            + removalY[0] + " over the launcher at y=" + radarPos.getY());
            helper.assertBlockPresent(ModBlocks.CRUISE_LAUNCHER, LAUNCHER_LOCAL);
            helper.assertBlockPresent(ModBlocks.RADAR_STATION, RADAR_LOCAL);
            logScene("lost target", "launched at tick " + lostTick[0] + ", target removed at the same tick, REACQUIRE at "
                    + reacquireTick[0] + ", gave up at " + removalTick[0] + " (y=" + removalY[0] + ", "
                    + targetDistanceAtRemoval[0] + " blocks from the removed target), reacquire_timeout_ticks="
                    + ConfigAbm.REACQUIRE_TIMEOUT_TICKS);
            helper.succeed();
        });
    }

    /**
     * §11 "跨雷达去重": two radars on <em>different channels</em>, each with its own launcher, both tracking
     * the same target. {@code EngagementRegistry} is level wide, so the second radar must not order a second
     * interceptor. The scene also records that both radars really tracked the threat while an interceptor was
     * already in flight.
     */
    @GameTest(maxTicks = 600, padding = 32)
    public void crossRadarEngagementKeepsOneInterceptorPerTarget(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final TileRadarStation radarA = placeRadar(helper, RADAR_LOCAL, DEDUP_A_CHANNEL, 96, 20);
        final TileRadarStation radarB = placeRadar(helper, RADAR_B_LOCAL, DEDUP_B_CHANNEL, 96, 20);
        final TileCruiseLauncher launcherA = placeAbmLauncher(helper, LAUNCHER_LOCAL, DEDUP_A_CHANNEL);
        final TileCruiseLauncher launcherB = placeAbmLauncher(helper, LAUNCHER_B_LOCAL, DEDUP_B_CHANNEL);
        final BlockPos radarPos = helper.absolutePos(RADAR_LOCAL);

        final SceneThreat threat = spawnApproachThreat(helper, radarPos);
        final UUID threatId = threat.getUUID();
        final int[] maxActive = { 0 };
        final boolean[] bothRadarsTrackedWhileEngaged = { false };

        helper.succeedWhen(() -> {
            power(radarA);
            power(radarB);
            powerLauncher(launcherA);
            powerLauncher(launcherB);
            RadarRegistry.add(threat);

            final int active = EngagementRegistry.activeCount(level, threatId);
            maxActive[0] = Math.max(maxActive[0], active);
            if (active > 0 && tracksThreat(radarA, threatId) && tracksThreat(radarB, threatId))
                bothRadarsTrackedWhileEngaged[0] = true;

            helper.assertTrue(!threat.isAlive(), "the incoming missile must be destroyed");
            helper.assertTrue(launchCount(threatId) == 1, "two radars on different channels share the engagement"
                    + " registry, so only one interceptor may be ordered, ordered " + launchCount(threatId));
            helper.assertTrue(maxActive[0] <= ConfigAbm.MAX_INTERCEPTORS_PER_TARGET,
                    "active interceptors per target must stay at or below max_interceptors_per_target="
                            + ConfigAbm.MAX_INTERCEPTORS_PER_TARGET + ", peaked at " + maxActive[0]);
            helper.assertTrue(bothRadarsTrackedWhileEngaged[0], "both radars must have tracked the threat while an"
                    + " interceptor was in flight, otherwise the deduplication is not exercised");
            helper.assertTrue(emptyLaunchers(List.of(launcherA, launcherB)) == 1,
                    "exactly one launcher must fire, fired " + emptyLaunchers(List.of(launcherA, launcherB)));
            helper.assertTrue(interceptCount(threatId) == 1,
                    "the proximity fuse must resolve exactly once, resolved " + interceptCount(threatId));
            logScene("cross radar dedup", "radars=2 (different channels), launches=" + launchCount(threatId)
                    + ", emptied launchers=" + emptyLaunchers(List.of(launcherA, launcherB)) + ", peak active="
                    + maxActive[0] + ", max_interceptors_per_target=" + ConfigAbm.MAX_INTERCEPTORS_PER_TARGET
                    + ", both radars tracked while engaged=" + bothRadarsTrackedWhileEngaged[0]);
            helper.succeed();
        });
    }
}
