package icbm.classic.test.prefab;

import icbm.classic.content.reg.ModBlockEntities;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.lib.tile.ITick;
import icbm.classic.lib.tile.TickAction;
import icbm.classic.lib.tile.TickDoOnce;
import icbm.classic.prefab.tile.TileMachine;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;

/**
 * Covers {@link TileMachine} (S5.01): the 1.12 {@code ITickable#update()} tick action list driven from the
 * 26.3 server/client tick entry points, {@link TickDoOnce} running exactly once and the 1.12 description
 * packet data ({@code getUpdateTag}).
 */
public class TileMachineGameTest
{
    /** Test machine exposing the protected 1.12 fields and saving a value for the update tag check */
    private static final class TestMachine extends TileMachine
    {
        private int value = 0;

        TestMachine(BlockPos pos)
        {
            //26.3 BlockEntity#<init> validates the block state against the block entity type, so the machine
            //is built on a real explosive block (the type has to exist to build a block entity in a test)
            super(ModBlockEntities.EXPLOSIVE, pos, ModBlocks.EXPLOSIVE_BLOCKS.values().iterator().next().defaultBlockState());
        }

        void addAction(ITick action)
        {
            tickActions.add(action);
        }

        int getTicks()
        {
            return ticks;
        }

        @Override
        protected void saveAdditional(ValueOutput out)
        {
            out.putInt("test_value", value);
            super.saveAdditional(out);
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
    public void tickDoOnceRunsExactlyOnce(GameTestHelper helper)
    {
        final int[] calls = {0};
        final TickDoOnce action = new TickDoOnce(tick -> calls[0]++);

        //1.12: the action list runs every tick, the do once action only fires after doNext()
        action.update(0, true);
        assertThat(helper, "no call before doNext", 0, calls[0]);

        action.doNext();
        action.update(1, true);
        assertThat(helper, "called once after doNext", 1, calls[0]);

        action.update(2, true);
        action.update(3, true);
        assertThat(helper, "not called again", 1, calls[0]);

        action.doNext();
        action.update(4, true);
        assertThat(helper, "called again after the second doNext", 2, calls[0]);

        helper.succeed();
    }

    @GameTest
    public void tileMachineDrivesTickActionsOnServerAndClient(GameTestHelper helper)
    {
        final TestMachine machine = new TestMachine(new BlockPos(1, 1, 1));
        machine.setLevel(helper.getLevel());

        final int[] everyTick = {0};
        final int[] everyFiveTicks = {0};
        final int[] clientOnly = {0};
        machine.addAction(new TickAction(1, true, () -> everyTick[0]++));
        machine.addAction(new TickAction(5, true, () -> everyFiveTicks[0]++));
        machine.addAction(new TickAction(1, false, () -> clientOnly[0]++));

        final ServerLevel serverLevel = helper.getLevel();
        for (int i = 0; i < 10; i++)
        {
            machine.serverTick(serverLevel);
        }

        //1.12 update() increased the tick first, the counter starts at -1 so the first tick is 0
        assertThat(helper, "tick counter after 10 server ticks", 9, machine.getTicks());
        assertThat(helper, "server action ran every tick", 10, everyTick[0]);
        assertThat(helper, "modulo action ran on ticks 0 and 5", 2, everyFiveTicks[0]);
        assertThat(helper, "client only action did not run on the server", 0, clientOnly[0]);
        assertThat(helper, "isServer", true, machine.isServer());
        assertThat(helper, "isClient", false, machine.isClient());

        //the client half of the 1.12 update() keeps working for the client only actions
        machine.clientTick();
        assertThat(helper, "client only action ran on the client tick", 1, clientOnly[0]);
        assertThat(helper, "server action did not run on the client tick", 10, everyTick[0]);

        helper.succeed();
    }

    @GameTest
    public void tileMachineSendsItsDataInTheUpdateTag(GameTestHelper helper)
    {
        final TestMachine machine = new TestMachine(new BlockPos(1, 1, 1));
        machine.setLevel(helper.getLevel());
        machine.value = 7;

        //1.12 getUpdatePacket/getUpdateTag wrote the block entity data for the client
        assertThat(helper, "update packet", true, machine.getUpdatePacket() != null);

        final CompoundTag tag = machine.getUpdateTag(helper.getLevel().registryAccess());
        assertThat(helper, "update tag value", 7, tag.getIntOr("test_value", -1));

        helper.succeed();
    }
}
