package icbm.classic.test.launcher;

import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.reg.ModBlocks;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Verifies server-side validation for cruise launcher client codexes. */
public class CruiseLauncherCodexGameTest
{
    private static final BlockPos LAUNCHER_POS = new BlockPos(1, 1, 1);

    @GameTest
    public void targetCodexRejectsCoordinatesOutsideWorldBorder(GameTestHelper helper)
    {
        final TileCruiseLauncher launcher = placeLauncher(helper);
        final BlockPos absolute = helper.absolutePos(LAUNCHER_POS);
        final ServerPlayer player = playerAt(helper, absolute);
        final Vec3 original = launcher.getTarget();
        final RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try
        {
            body.writeBlockPos(absolute);
            body.writeDouble(1.0E9);
            body.writeDouble(absolute.getY());
            body.writeDouble(absolute.getZ());
            TileCruiseLauncher.PACKET_TARGET.handleFromClient(player, body);
        }
        finally
        {
            body.release();
        }
        helper.assertTrue(original.equals(launcher.getTarget()), "target outside the world border must be rejected");
        helper.succeed();
    }

    @GameTest
    public void radioCodexRejectsChannelLongerThan32Characters(GameTestHelper helper)
    {
        final TileCruiseLauncher launcher = placeLauncher(helper);
        final BlockPos absolute = helper.absolutePos(LAUNCHER_POS);
        final ServerPlayer player = playerAt(helper, absolute);
        launcher.radio.setChannel("radio-ok");
        final RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try
        {
            body.writeBlockPos(absolute);
            body.writeUtf("a".repeat(33));
            TileCruiseLauncher.PACKET_RADIO_HZ.handleFromClient(player, body);
        }
        finally
        {
            body.release();
        }
        helper.assertTrue("radio-ok".equals(launcher.radio.getChannel()), "radio channel longer than 32 characters must be rejected");
        helper.succeed();
    }

    private static TileCruiseLauncher placeLauncher(GameTestHelper helper)
    {
        helper.setBlock(LAUNCHER_POS, ModBlocks.CRUISE_LAUNCHER.defaultBlockState());
        final TileCruiseLauncher launcher = (TileCruiseLauncher) helper.getLevel().getBlockEntity(helper.absolutePos(LAUNCHER_POS));
        helper.assertTrue(launcher != null, "cruise launcher block entity must be present");
        return launcher;
    }

    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos pos)
    {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return player;
    }
}
