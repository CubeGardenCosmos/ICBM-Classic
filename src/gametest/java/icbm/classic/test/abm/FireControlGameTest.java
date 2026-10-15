package icbm.classic.test.abm;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.radio.InterceptOffer;
import icbm.classic.content.abm.radio.InterceptOrderMessage;
import icbm.classic.content.abm.radio.InterceptQueryMessage;
import icbm.classic.content.abm.radar.RadarTrack;
import icbm.classic.content.abm.radar.TrackStatus;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.radio.RadioRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class FireControlGameTest
{
    private static final String SELECTS_NEAREST_CHANNEL = "fire-control-selects-nearest";
    private static final String LAUNCHED_ORDER_CHANNEL = "fire-control-launched-order";
    private static final String NO_OFFER_CHANNEL = "fire-control-no-offer";
    private static final String RECOVERY_CHANNEL = "fire-control-engaged-recovery";

    private static final class TestMissile extends EntityExplosiveMissile
    {
        private TestMissile(Level level)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
        }

        @Override
        public int getTicksInAir() { return 2; }

        @Override
        public void tick()
        {
            // Keep the test trajectory stationary; radar reads the configured velocity.
        }
    }

    private static final class FrozenAbm extends EntityAntiBallisticMissile
    {
        private FrozenAbm(Level level)
        {
            super(level);
        }

        @Override
        public void tick()
        {
            // Keep the interceptor in place so the test decides exactly when the engagement ends.
        }
    }

    private static final class TestReceiver implements IRadioReceiver
    {
        private final Level level;
        private final BlockPos pos;
        private final BlockPos launcherPos;
        private final Vec3 launchPoint;
        private final UUID launchedAbmId;
        private final String channel;
        private int queryCount;
        private int orderCount;

        private TestReceiver(Level level, BlockPos pos, BlockPos launcherPos, Vec3 launchPoint,
                             UUID launchedAbmId, String channel)
        {
            this.level = level;
            this.pos = pos;
            this.launcherPos = launcherPos;
            this.launchPoint = launchPoint;
            this.launchedAbmId = launchedAbmId;
            this.channel = channel;
        }

        @Override
        public void onMessage(IRadioSender sender, IRadioMessage packet)
        {
            if (!channel.equals(packet.getChannel())) return;
            if (packet instanceof InterceptQueryMessage query)
            {
                queryCount++;
                if (launcherPos != null)
                    query.offer(new InterceptOffer(launcherPos, launchPoint, 4.0, 0));
            }
            else if (packet instanceof InterceptOrderMessage order && launcherPos != null
                    && launcherPos.equals(order.launcherPos()))
            {
                orderCount++;
                if (launchedAbmId != null) order.markLaunched(launchedAbmId);
            }
        }

        @Override public BlockPos getBlockPos() { return pos; }
        @Override public Level getWorld() { return level; }
        @Override public IBoundBox<BlockPos> getRange() { return RadioRegistry.INFINITE; }
    }

    private record Fixture(TileRadarStation radar, BlockPos pos, TestMissile missile) { }

    private Fixture setup(GameTestHelper helper, String channel)
    {
        BlockPos localPos = new BlockPos(2, 1, 2);
        helper.setBlock(localPos, ModBlocks.RADAR_STATION);
        BlockPos pos = helper.absolutePos(localPos);
        TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(radar != null, "radar station must be present");
        radar.setDetectionRange(48);
        radar.setTriggerRange(10);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
        radar.getRadio().setChannel(channel);
        helper.onEachTick(() -> radar.energyStorage.setStored(radar.energyStorage.getCapacity()));

        TestMissile missile = new TestMissile(helper.getLevel());
        missile.explosive.setStack(new net.minecraft.world.item.ItemStack(
                ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        missile.setPos(pos.getX() + 0.5, pos.getY() + 40.5, pos.getZ() + 0.5);
        missile.setDeltaMovement(new Vec3(0, -1, 0));
        missile.setNoGravity(true);
        missile.setFreezeMotion(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(missile), "test threat missile must spawn");
        RadarRegistry.add(missile);
        return new Fixture(radar, pos, missile);
    }

    private static RadarTrack refreshTrack(GameTestHelper helper, Fixture fixture)
    {
        RadarRegistry.add(fixture.missile());
        return fixture.radar().getTrackTable().get(fixture.missile().getUUID());
    }

    private static TestReceiver receiver(GameTestHelper helper, BlockPos radarPos, BlockPos launcherPos,
                                         UUID launchedAbmId, String channel)
    {
        Vec3 launchPoint = launcherPos == null ? null
                : new Vec3(launcherPos.getX() + 0.5, launcherPos.getY() + 1, launcherPos.getZ() + 0.5);
        return new TestReceiver(helper.getLevel(), launcherPos == null ? radarPos : launcherPos,
                launcherPos, launchPoint, launchedAbmId, channel);
    }

    @GameTest(maxTicks = 100, padding = 128)
    public void selectsNearestOffer(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, SELECTS_NEAREST_CHANNEL);
        BlockPos farPos = fixture.pos().offset(0, 0, 10);
        BlockPos nearPos = fixture.pos().offset(0, 0, 2);
        TestReceiver far = receiver(helper, fixture.pos(), farPos, null, SELECTS_NEAREST_CHANNEL);
        TestReceiver near = receiver(helper, fixture.pos(), nearPos, null, SELECTS_NEAREST_CHANNEL);
        RadioRegistry.add(far);
        RadioRegistry.add(near);
        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            RadarTrack track = refreshTrack(helper, fixture);
            helper.assertTrue(track != null && track.status == TrackStatus.THREAT && near.orderCount > 0,
                    "radar must create the expected threat track and nearest launcher must receive an order");
            helper.assertTrue(far.orderCount == 0, "far launcher must not receive the selected order");
            helper.assertTrue(near.orderCount == 1, "nearest launcher must receive the selected order");
            RadioRegistry.remove(far);
            RadioRegistry.remove(near);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 120, padding = 128)
    public void launchedOrderEngagesTrackAndHonorsReengageDelay(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, LAUNCHED_ORDER_CHANNEL);
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(helper.getLevel());
        abm.setTargetId(fixture.missile().getUUID());
        helper.assertTrue(helper.getLevel().addFreshEntity(abm), "test ABM must spawn");
        BlockPos launcherPos = fixture.pos().offset(0, 0, 2);
        TestReceiver receiver = receiver(helper, fixture.pos(), launcherPos, abm.getUUID(), LAUNCHED_ORDER_CHANNEL);
        RadioRegistry.add(receiver);
        final boolean[] reengageCheckScheduled = { false };
        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            if (reengageCheckScheduled[0]) {
                helper.assertTrue(false, "waiting for reengage-delay checks");
            }
            RadarTrack track = refreshTrack(helper, fixture);
            helper.assertTrue(track != null && track.status == TrackStatus.ENGAGED && receiver.orderCount > 0,
                    "radar must create the expected engaged track and launcher must receive an order");
            ServerLevel level = (ServerLevel) helper.getLevel();
            helper.assertTrue(EngagementRegistry.activeCount(level, fixture.missile().getUUID()) == 1,
                    "launched ABM must be registered as active");
            int initialQueries = receiver.queryCount;
            track.setStatus(TrackStatus.THREAT, level.getGameTime());
            reengageCheckScheduled[0] = true;
            helper.runAfterDelay(Math.max(1, ConfigAbm.REENGAGE_DELAY_TICKS - 1), () -> {
                RadarTrack current = refreshTrack(helper, fixture);
                helper.assertTrue(current != null, "real missile track must still exist during reengage delay");
                helper.assertTrue(receiver.queryCount == initialQueries,
                        "reengage delay must suppress a repeated query for the THREAT track");
                RadioRegistry.remove(receiver);
                abm.discard();
                helper.succeed();
            });
            helper.assertTrue(false, "waiting for reengage-delay checks");
        });
    }

    @GameTest(maxTicks = 100, padding = 128)
    public void noOfferDoesNotSendOrder(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, NO_OFFER_CHANNEL);
        TestReceiver receiver = receiver(helper, fixture.pos(), null, null, NO_OFFER_CHANNEL);
        RadioRegistry.add(receiver);
        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            RadarTrack track = refreshTrack(helper, fixture);
            helper.assertTrue(track != null && track.status == TrackStatus.THREAT && receiver.queryCount > 0,
                    "radar must create the expected threat track and query the receiver");
            helper.assertTrue(receiver.orderCount == 0, "no offer must not produce an order");
            RadioRegistry.remove(receiver);
            helper.succeed();
        });
    }

    /**
     * C1: an ENGAGED track must not stay engaged forever once its interceptor is gone. The scan has to drop it back
     * to THREAT and the fire control must be able to order a replacement interceptor while the threat keeps flying.
     * Before the fix the track stayed ENGAGED, {@code TrackTable#threats()} stayed empty and no second order was
     * ever sent, which made {@code ConfigAbm.REENGAGE_DELAY_TICKS} meaningless after a failed interception.
     */
    @GameTest(maxTicks = 200, padding = 128)
    public void engagedTrackRecoversWhenInterceptorIsLost(GameTestHelper helper)
    {
        Fixture fixture = setup(helper, RECOVERY_CHANNEL);
        final ServerLevel level = (ServerLevel) helper.getLevel();
        FrozenAbm firstAbm = new FrozenAbm(level);
        firstAbm.setTargetId(fixture.missile().getUUID());
        helper.assertTrue(level.addFreshEntity(firstAbm), "test interceptor must spawn");
        BlockPos launcherPos = fixture.pos().offset(0, 0, 2);
        TestReceiver receiver = receiver(helper, fixture.pos(), launcherPos, firstAbm.getUUID(), RECOVERY_CHANNEL);
        RadioRegistry.add(receiver);
        final int[] phase = { 0 };
        final int[] ordersWhenLost = { 0 };
        helper.succeedWhen(() -> {
            fixture.radar().energyStorage.setStored(fixture.radar().energyStorage.getCapacity());
            RadarTrack track = refreshTrack(helper, fixture);
            helper.assertTrue(track != null, "radar must track the incoming missile");

            if (phase[0] == 0)
            {
                helper.assertTrue(track.status == TrackStatus.ENGAGED && receiver.orderCount > 0,
                        "radar must engage the track while the interceptor is in flight");
                helper.assertTrue(EngagementRegistry.activeCount(level, fixture.missile().getUUID()) == 1,
                        "the interceptor must count as active while its entity exists");
                ordersWhenLost[0] = receiver.orderCount;
                firstAbm.discard(); // interception fails: the interceptor is gone, the threat keeps flying
                phase[0] = 1;
                helper.assertTrue(false, "waiting for the scan to notice the lost interceptor");
            }

            if (phase[0] == 1)
            {
                helper.assertTrue(EngagementRegistry.activeCount(level, fixture.missile().getUUID()) == 0,
                        "a discarded interceptor must no longer count as active");
                helper.assertTrue(track.status == TrackStatus.THREAT,
                        "a track whose interceptor is gone must fall back to THREAT, was " + track.status);
                phase[0] = 2;
                helper.assertTrue(false, "waiting for the replacement interceptor order");
            }

            helper.assertTrue(receiver.orderCount >= ordersWhenLost[0] + 1,
                    "a failed interception must allow a replacement order, ordered " + receiver.orderCount
                            + " of " + ordersWhenLost[0]);
            helper.assertTrue(EngagementRegistry.activeCount(level, fixture.missile().getUUID()) == 0,
                    "the replacement order must have been possible because no interceptor was left");
            RadioRegistry.remove(receiver);
            firstAbm.discard();
            helper.succeed();
        });
    }
}
