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
    private static final String CHANNEL = "fire-control-test";

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

    private static final class TestReceiver implements IRadioReceiver
    {
        private final Level level;
        private final BlockPos pos;
        private final BlockPos launcherPos;
        private final Vec3 launchPoint;
        private final UUID launchedAbmId;
        private int queryCount;
        private int orderCount;

        private TestReceiver(Level level, BlockPos pos, BlockPos launcherPos, Vec3 launchPoint, UUID launchedAbmId)
        {
            this.level = level;
            this.pos = pos;
            this.launcherPos = launcherPos;
            this.launchPoint = launchPoint;
            this.launchedAbmId = launchedAbmId;
        }

        @Override
        public void onMessage(IRadioSender sender, IRadioMessage packet)
        {
            if (!CHANNEL.equals(packet.getChannel())) return;
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

    private Fixture setup(GameTestHelper helper)
    {
        BlockPos localPos = new BlockPos(2, 1, 2);
        helper.setBlock(localPos, ModBlocks.RADAR_STATION);
        BlockPos pos = helper.absolutePos(localPos);
        TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(radar != null, "radar station must be present");
        radar.setDetectionRange(100);
        radar.setTriggerRange(10);
        radar.getRadio().setChannel(CHANNEL);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());

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

    private static TestReceiver receiver(GameTestHelper helper, BlockPos radarPos, BlockPos launcherPos, UUID launchedAbmId)
    {
        Vec3 launchPoint = launcherPos == null ? null
                : new Vec3(launcherPos.getX() + 0.5, launcherPos.getY() + 1, launcherPos.getZ() + 0.5);
        return new TestReceiver(helper.getLevel(), launcherPos == null ? radarPos : launcherPos,
                launcherPos, launchPoint, launchedAbmId);
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void selectsNearestOffer(GameTestHelper helper)
    {
        Fixture fixture = setup(helper);
        BlockPos farPos = fixture.pos().offset(0, 0, 10);
        BlockPos nearPos = fixture.pos().offset(0, 0, 2);
        TestReceiver far = receiver(helper, fixture.pos(), farPos, null);
        TestReceiver near = receiver(helper, fixture.pos(), nearPos, null);
        RadioRegistry.add(far);
        RadioRegistry.add(near);
        helper.succeedWhen(() -> {
            RadarTrack track = refreshTrack(helper, fixture);
            if (track == null || track.status != TrackStatus.THREAT || near.orderCount == 0) return;
            helper.assertTrue(far.orderCount == 0, "far launcher must not receive the selected order");
            helper.assertTrue(near.orderCount == 1, "nearest launcher must receive the selected order");
            RadioRegistry.remove(far);
            RadioRegistry.remove(near);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 120, padding = 16)
    public void launchedOrderEngagesTrackAndHonorsReengageDelay(GameTestHelper helper)
    {
        Fixture fixture = setup(helper);
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(helper.getLevel());
        abm.setTargetId(fixture.missile().getUUID());
        helper.assertTrue(helper.getLevel().addFreshEntity(abm), "test ABM must spawn");
        BlockPos launcherPos = fixture.pos().offset(0, 0, 2);
        TestReceiver receiver = receiver(helper, fixture.pos(), launcherPos, abm.getUUID());
        RadioRegistry.add(receiver);
        helper.succeedWhen(() -> {
            RadarTrack track = refreshTrack(helper, fixture);
            if (track == null || track.status != TrackStatus.ENGAGED || receiver.orderCount == 0) return;
            ServerLevel level = (ServerLevel) helper.getLevel();
            helper.assertTrue(EngagementRegistry.activeCount(level, fixture.missile().getUUID()) == 1,
                    "launched ABM must be registered as active");
            int initialQueries = receiver.queryCount;
            track.setStatus(TrackStatus.THREAT, level.getGameTime());
            helper.runAfterDelay(Math.max(1, ConfigAbm.REENGAGE_DELAY_TICKS - 1), () -> {
                RadarTrack current = refreshTrack(helper, fixture);
                helper.assertTrue(current != null, "real missile track must still exist during reengage delay");
                helper.assertTrue(receiver.queryCount == initialQueries,
                        "reengage delay must suppress a repeated query for the THREAT track");
                RadioRegistry.remove(receiver);
                abm.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void noOfferDoesNotSendOrder(GameTestHelper helper)
    {
        Fixture fixture = setup(helper);
        TestReceiver receiver = receiver(helper, fixture.pos(), null, null);
        RadioRegistry.add(receiver);
        helper.succeedWhen(() -> {
            RadarTrack track = refreshTrack(helper, fixture);
            if (track == null || track.status != TrackStatus.THREAT || receiver.queryCount == 0) return;
            helper.assertTrue(receiver.orderCount == 0, "no offer must not produce an order");
            RadioRegistry.remove(receiver);
            helper.succeed();
        });
    }
}
