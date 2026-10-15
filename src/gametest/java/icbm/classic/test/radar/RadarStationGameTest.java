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

        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, helper.getLevel());
        missile.explosive.setStack(new net.minecraft.world.item.ItemStack(
                ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        missile.setPos(radarPos.getX() + 8.5, radarPos.getY() + 1.5, radarPos.getZ() + 0.5);
        missile.setDeltaMovement(new Vec3(-0.4, 0, 0));
        missile.setNoGravity(true);
        helper.getLevel().addFreshEntity(missile);
        missile.launch();

        helper.succeedWhen(() -> {
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
            helper.assertTrue(receiver.received.getMissile() == missile, "broadcast must identify the detected missile");
            RadioRegistry.remove(receiver);
            helper.succeed();
        });
    }
}
