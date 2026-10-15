package icbm.classic.test.radar;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.IncomingMissileMessage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class RadarStationGameTest
{
    private static final class TestReceiver implements IRadioReceiver
    {
        private final Level level;
        private final BlockPos pos;
        private final String channel;
        private IncomingMissileMessage received;

        private TestReceiver(Level level, BlockPos pos, String channel)
        {
            this.level = level;
            this.pos = pos;
            this.channel = channel;
        }

        @Override
        public void onMessage(IRadioSender sender, IRadioMessage message)
        {
            if (Objects.equals(channel, message.getChannel()) && message instanceof IncomingMissileMessage incoming)
                received = incoming;
        }

        @Override public BlockPos getBlockPos() { return pos; }
        @Override public Level getWorld() { return level; }
        @Override public IBoundBox<BlockPos> getRange() { return RadioRegistry.INFINITE; }
    }
    /**
     * A radar target whose position is controlled by the fixture; the regular projectile physics can collide with
     * the shared game-test structures while the radar only needs its flight state and prediction velocity.
     */
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
            // Keep the radar contact stationary; getDeltaMovement still describes its predicted inbound path.
        }
    }

    @GameTest(maxTicks = 10, padding = 16)
    public void radarDetectsAndBroadcastsIncomingCondensedMissile(GameTestHelper helper)
    {
        final BlockPos localRadar = new BlockPos(2, 1, 2);
        helper.setBlock(localRadar, ModBlocks.RADAR_STATION);
        helper.setBlock(localRadar.east(), Blocks.REDSTONE_LAMP);
        final BlockPos radarPos = helper.absolutePos(localRadar);
        final TileRadarStation radar = (TileRadarStation) helper.getLevel().getBlockEntity(radarPos);
        helper.assertTrue(radar != null, "radar station block entity must be present");
        radar.setDetectionRange(32);
        radar.setTriggerRange(16);
        radar.energyStorage.setStored(radar.energyStorage.getCapacity());
        radar.getRadio().setChannel("radar-test");
        final TestReceiver receiver = new TestReceiver(helper.getLevel(), radarPos.south(2), "radar-test");
        RadioRegistry.add(receiver);

        final TestMissile missile = new TestMissile(helper.getLevel());
        missile.explosive.setStack(new net.minecraft.world.item.ItemStack(
                ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        // Keep the target above the structure roof and inside the radar's x/z query; physics is disabled by the
        // fixture subclass, not by altering radar's incoming classification or its velocity prediction.
        // The contact has to describe a descent (the same downward velocity LauncherInterceptGameTest uses),
        // otherwise TrajectoryPredictor never reaches the impact floor within ConfigAbm.PREDICTION_MAX_TICKS, the
        // track stays DETECTED and the station never reports an incoming missile. The descent also has to take no
        // longer than ConfigAbm.EMP_FALLBACK_TICKS, because the station only broadcasts the IncomingMissileMessage
        // for a threat it can reach in time: 50.5 blocks at 2 blocks a tick predicts an impact after 26 ticks.
        final int chunkMinX = radarPos.getX() & ~15;
        final boolean fromPositiveX = radarPos.getX() <= chunkMinX + 7;
        final double missileX = fromPositiveX ? chunkMinX + 11.5 : chunkMinX + 4.5;
        missile.setPos(missileX, radarPos.getY() + 50.5, radarPos.getZ() + 0.5);
        final BlockPos missileBlockPos = missile.blockPosition();
        helper.assertTrue(helper.getLevel().getBlockState(missileBlockPos).isAir(),
                "missile must start in an empty block");
        missile.setDeltaMovement(new Vec3(fromPositiveX ? -0.1 : 0.1, -2, 0));
        missile.setNoGravity(true);
        missile.setFreezeMotion(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(missile), "incoming missile must spawn");
        icbm.classic.lib.radar.RadarRegistry.add(missile);

        helper.succeedWhen(() -> {
            // Other GameTests can unload a shared chunk and remove its contacts from the dimension registry.
            // Re-register only this target before waiting for the station's next scan.
            icbm.classic.lib.radar.RadarRegistry.add(missile);
            helper.assertTrue(radar.hasIncomingMissiles(),
                    "radar must classify the approaching missile as incoming (missileTicksInAir="
                            + missile.getTicksInAir() + ", missilePos=" + missile.position()
                            + ", missileVelocity=" + missile.getDeltaMovement() + ", incomingCount="
                            + radar.getIncomingThreats().size() + ", tracked="
                            + icbm.classic.lib.radar.RadarRegistry.getAllLivingObjectsWithin(
                            helper.getLevel(), radarPos.getX() + 0.5, radarPos.getY() + 0.5, radarPos.getZ() + 0.5, 32)
                            .contains(missile) + ", power=" + radar.energyStorage.getStored()
                            + ", requiresPower=" + icbm.classic.config.ConfigMain.REQUIRES_POWER + ")");
            helper.assertTrue(helper.getLevel().getBlockState(radarPos.east()).getValue(RedstoneLampBlock.LIT),
                    "redstone lamp beside radar must be lit");
            helper.assertTrue(receiver.received != null, "radio receiver must receive IncomingMissileMessage");
            helper.assertTrue(receiver.received.getMissile() == missile,
                    "broadcast must identify the detected missile");
            // Cleanup only after every assertion passed: a failing attempt is retried on the next tick and must still
            // find the receiver and the missile registered.
            RadioRegistry.remove(receiver);
            missile.discard();
            helper.succeed();
        });
    }
}

