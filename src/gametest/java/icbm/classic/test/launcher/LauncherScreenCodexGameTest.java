package icbm.classic.test.launcher;

import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.reg.ModBlocks;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Server-side validation tests for launcher screen client codexes. */
public class LauncherScreenCodexGameTest
{
    private static final BlockPos SCREEN_POS = new BlockPos(1, 1, 1);

    @GameTest
    public void targetCodexUpdatesTileFromNearbyPlayer(GameTestHelper helper)
    {
        final TileLauncherScreen screen = placeScreen(helper);
        final BlockPos absolute = helper.absolutePos(SCREEN_POS);
        final ServerPlayer player = playerAt(helper, absolute);
        final Vec3 target = new Vec3(absolute.getX() + 20.5, absolute.getY() + 10.5, absolute.getZ() - 6.5);
        handleTarget(screen, player, absolute, target);
        helper.assertTrue(target.equals(screen.getTarget()), "valid in-border target codex must update the screen target");
        helper.succeed();
    }

    @GameTest
    public void targetCodexRejectsPlayerBeyond64Blocks(GameTestHelper helper)
    {
        final TileLauncherScreen screen = placeScreen(helper);
        final BlockPos absolute = helper.absolutePos(SCREEN_POS);
        final ServerPlayer player = playerAt(helper, absolute.offset(65, 0, 0));
        final Vec3 original = screen.getTarget();
        handleTarget(screen, player, absolute, new Vec3(absolute.getX() + 2.5, absolute.getY() + 3.5, absolute.getZ() + 4.5));
        helper.assertTrue(original.equals(screen.getTarget()), "target codex from more than 64 blocks away must be rejected");
        helper.succeed();
    }

    @GameTest
    public void targetCodexRejectsCoordinatesOutsideWorldBorder(GameTestHelper helper)
    {
        final TileLauncherScreen screen = placeScreen(helper);
        final BlockPos absolute = helper.absolutePos(SCREEN_POS);
        final ServerPlayer player = playerAt(helper, absolute);
        final Vec3 original = screen.getTarget();
        handleTarget(screen, player, absolute, new Vec3(1.0E9, absolute.getY(), absolute.getZ()));
        helper.assertTrue(original.equals(screen.getTarget()), "target outside the world border must be rejected");
        helper.succeed();
    }

    @GameTest
    public void radioCodexRejectsChannelLongerThan32Characters(GameTestHelper helper)
    {
        final TileLauncherScreen screen = placeScreen(helper);
        final BlockPos absolute = helper.absolutePos(SCREEN_POS);
        final ServerPlayer player = playerAt(helper, absolute);
        screen.radioCap.setChannel("radio-ok");
        final RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try
        {
            body.writeBlockPos(absolute);
            body.writeUtf("a".repeat(33));
            TileLauncherScreen.PACKET_RADIO_HZ.handleFromClient(player, body);
        }
        finally
        {
            body.release();
        }
        helper.assertTrue("radio-ok".equals(screen.radioCap.getChannel()), "radio channel longer than 32 characters must be rejected");
        helper.succeed();
    }

    private static TileLauncherScreen placeScreen(GameTestHelper helper)
    {
        helper.setBlock(SCREEN_POS, ModBlocks.LAUNCHER_SCREEN.defaultBlockState());
        final TileLauncherScreen screen = (TileLauncherScreen) helper.getLevel().getBlockEntity(helper.absolutePos(SCREEN_POS));
        helper.assertTrue(screen != null, "launcher screen block entity must be present");
        return screen;
    }

    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos pos)
    {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return player;
    }

    private static void handleTarget(TileLauncherScreen screen, ServerPlayer player, BlockPos pos, Vec3 target)
    {
        final RegistryFriendlyByteBuf body = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.level().registryAccess());
        try
        {
            body.writeBlockPos(pos);
            body.writeDouble(target.x);
            body.writeDouble(target.y);
            body.writeDouble(target.z);
            TileLauncherScreen.PACKET_TARGET.handleFromClient(player, body);
        }
        finally
        {
            body.release();
        }
    }
}
