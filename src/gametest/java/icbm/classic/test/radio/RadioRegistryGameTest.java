package icbm.classic.test.radio;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.TriggerActionMessage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Tests the radio registry (S1.05): receivers registered by level receive messages sent over their channel,
 * messages with a different or empty channel reach nobody, and clearing a level drops its receivers.
 *
 * <p>Receivers filter on channel themselves (mirroring {@code RadioTile#canReceive}); the registry only
 * decides who is in range, same split as 1.12.</p>
 */
public class RadioRegistryGameTest
{
    /** Minimal radio receiver, counts the messages it accepted */
    private static final class TestReceiver implements IRadioReceiver
    {
        private final Level level;
        private final BlockPos pos;
        private final String channel;

        private int messages;
        private IRadioMessage lastMessage;

        private TestReceiver(Level level, BlockPos pos, String channel)
        {
            this.level = level;
            this.pos = pos;
            this.channel = channel;
        }

        @Override
        public void onMessage(IRadioSender sender, IRadioMessage packet)
        {
            if (sender != (Object) this && Objects.equals(channel, packet.getChannel()))
            {
                messages++;
                lastMessage = packet;
            }
        }

        @Override
        public BlockPos getBlockPos()
        {
            return pos;
        }

        @Override
        public Level getWorld()
        {
            return level;
        }

        @Override
        public IBoundBox<BlockPos> getRange()
        {
            return RadioRegistry.INFINITE;
        }
    }

    /** Minimal radio sender, only used as the source of a message */
    private static final class TestSender implements IRadioSender
    {
        private final Level level;
        private final BlockPos pos;

        private TestSender(Level level, BlockPos pos)
        {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public void onMessageCallback(IRadioReceiver receiver, IRadioMessage response)
        {
            // no response is expected in these tests
        }

        @Override
        public BlockPos getBlockPos()
        {
            return pos;
        }

        @Override
        public Level getWorld()
        {
            return level;
        }

        @Override
        public IBoundBox<BlockPos> getRange()
        {
            return RadioRegistry.INFINITE;
        }
    }

    private static void assertThat(GameTestHelper helper, String name, Object expected, Object actual)
    {
        if (!Objects.equals(expected, actual))
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    @GameTest
    public void sameChannelReceives(GameTestHelper helper)
    {
        final Level level = helper.getLevel();
        final TestReceiver receiver = new TestReceiver(level, new BlockPos(1, 1, 1), "alpha");
        final TestSender sender = new TestSender(level, new BlockPos(2, 1, 1));
        try
        {
            assertThat(helper, "receiver added", true, RadioRegistry.add(receiver));
            final TriggerActionMessage packet = new TriggerActionMessage("alpha");
            RadioRegistry.popMessage(level, sender, packet);
            assertThat(helper, "received count", 1, receiver.messages);
            assertThat(helper, "received channel", "alpha", receiver.lastMessage != null ? receiver.lastMessage.getChannel() : null);
            assertThat(helper, "received message", true, receiver.lastMessage instanceof TriggerActionMessage);
            helper.succeed();
        }
        finally
        {
            RadioRegistry.remove(receiver);
        }
    }

    @GameTest
    public void otherChannelDoesNotReceive(GameTestHelper helper)
    {
        final Level level = helper.getLevel();
        final TestReceiver receiver = new TestReceiver(level, new BlockPos(1, 1, 1), "alpha");
        final TestSender sender = new TestSender(level, new BlockPos(2, 1, 1));
        try
        {
            RadioRegistry.add(receiver);
            RadioRegistry.popMessage(level, sender, new TriggerActionMessage("beta"));
            assertThat(helper, "received count", 0, receiver.messages);
            helper.succeed();
        }
        finally
        {
            RadioRegistry.remove(receiver);
        }
    }

    @GameTest
    public void emptyChannelIsNotSent(GameTestHelper helper)
    {
        final Level level = helper.getLevel();
        // receiver listens on the empty channel, so only the registry's rejection of EMPTY_HZ keeps it quiet
        final TestReceiver receiver = new TestReceiver(level, new BlockPos(1, 1, 1), RadioRegistry.EMPTY_HZ);
        final TestSender sender = new TestSender(level, new BlockPos(2, 1, 1));
        try
        {
            RadioRegistry.add(receiver);
            RadioRegistry.popMessage(level, sender, new TriggerActionMessage(RadioRegistry.EMPTY_HZ));
            assertThat(helper, "received count", 0, receiver.messages);
            helper.succeed();
        }
        finally
        {
            RadioRegistry.remove(receiver);
        }
    }

    @GameTest
    public void removeDropsReceiver(GameTestHelper helper)
    {
        final Level level = helper.getLevel();
        final TestReceiver receiver = new TestReceiver(level, new BlockPos(1, 1, 1), "alpha");
        final TestSender sender = new TestSender(level, new BlockPos(2, 1, 1));
        RadioRegistry.add(receiver);

        RadioRegistry.popMessage(level, sender, new TriggerActionMessage("alpha"));
        assertThat(helper, "received count before remove", 1, receiver.messages);

        RadioRegistry.remove(receiver);
        RadioRegistry.popMessage(level, sender, new TriggerActionMessage("alpha"));
        assertThat(helper, "received count after remove", 1, receiver.messages);
        helper.succeed();
    }
}
