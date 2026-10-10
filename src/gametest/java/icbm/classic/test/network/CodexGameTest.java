package icbm.classic.test.network;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.network.lambda.PacketCodex;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.network.IcbmNetwork;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Tests the codex transport layer (S1.04): the codex builder has to produce a body that decodes back into
 * the fields it was built from, and the server has to reject codexes that are not meant to come from a client.
 */
public class CodexGameTest
{
    /** Target the test codexes read from and write to */
    private static final class TestTarget
    {
        private int number;
        private String text = "";
        private boolean flag;
        private ItemStack stack = ItemStack.EMPTY;
    }

    private static final Identifier PARENT = ICBMConstants.id("test_codex");

    /**
     * Builds a tile codex holding the nodes used by the round trip test. It is a tile codex so the real
     * subclass is exercised; the tests only call {@code encodeBody}/{@code decodeAsSetters}, so the converter
     * and the location prefix are never used.
     */
    private static PacketCodexTile<BlockEntity, TestTarget> fieldsCodex(String name)
    {
        return (PacketCodexTile<BlockEntity, TestTarget>) new PacketCodexTile<BlockEntity, TestTarget>(PARENT, name, TestTarget.class, blockEntity -> new TestTarget())
            .nodeInt(t -> t.number, (t, value) -> t.number = value)
            .nodeString(t -> t.text, (t, value) -> t.text = value)
            .nodeBoolean(t -> t.flag, (t, value) -> t.flag = value);
    }

    private static final PacketCodexTile<BlockEntity, TestTarget> TEST_CODEX = fieldsCodex("fields");

    /** Codex with a registry backed node, item stacks need the registry access of the level */
    private static final PacketCodexTile<BlockEntity, TestTarget> ITEM_CODEX = (PacketCodexTile<BlockEntity, TestTarget>)
        new PacketCodexTile<BlockEntity, TestTarget>(PARENT, "item", TestTarget.class, blockEntity -> new TestTarget())
            .nodeItemStack(t -> t.stack, (t, value) -> t.stack = value);

    /** Codex that the client sends to the server, the server has to accept it */
    private static final PacketCodex<BlockEntity, TestTarget> CLIENT_CODEX = fieldsCodex("from_client").fromClient();

    /** Codex that the server sends to a client, the server has to reject it */
    private static final PacketCodex<BlockEntity, TestTarget> SERVER_CODEX = fieldsCodex("from_server").fromServer();

    private static void assertThat(GameTestHelper helper, String name, Object expected, Object actual)
    {
        if (!Objects.equals(expected, actual))
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void assertFields(GameTestHelper helper, TestTarget target)
    {
        assertThat(helper, "number", 42, target.number);
        assertThat(helper, "text", "icbm", target.text);
        assertThat(helper, "flag", true, target.flag);
    }

    private static RegistryFriendlyByteBuf buffer(byte[] body)
    {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(body), RegistryAccess.EMPTY);
    }

    @GameTest
    public void codexBodyRoundTripsEveryNodeType(GameTestHelper helper)
    {
        // the id used to look the codex up on the other side is parent path + "/" + name path
        assertThat(helper, "codex id", "icbmclassic:test_codex/fields", TEST_CODEX.getCodexId().toString());

        final TestTarget source = new TestTarget();
        source.number = 42;
        source.text = "icbm";
        source.flag = true;

        final byte[] body = TEST_CODEX.encodeBody(source);

        // decodeAsSetters has to read every field before any setter runs, so a broken body can not half apply
        final TestTarget bySetters = new TestTarget();
        final List<Consumer<TestTarget>> setters = TEST_CODEX.decodeAsSetters(buffer(body));
        assertThat(helper, "setter count", 3, setters.size());
        setters.forEach(setter -> setter.accept(bySetters));
        assertFields(helper, bySetters);

        final TestTarget byApply = new TestTarget();
        TEST_CODEX.decodeAndApply(buffer(body), byApply);
        assertFields(helper, byApply);

        helper.succeed();
    }

    @GameTest
    public void serverRejectsCodexesNotAllowedFromClient(GameTestHelper helper)
    {
        assertThat(helper, "fromClient codex", true, IcbmNetwork.canHandleFromClient(CLIENT_CODEX));
        assertThat(helper, "fromServer codex", false, IcbmNetwork.canHandleFromClient(SERVER_CODEX));
        assertThat(helper, "missing codex", false, IcbmNetwork.canHandleFromClient(null));
        helper.succeed();
    }

    @GameTest
    public void codexBodyRoundTripsItemStack(GameTestHelper helper)
    {
        final RegistryAccess registryAccess = helper.getLevel().registryAccess();

        final TestTarget source = new TestTarget();
        source.stack = new ItemStack(Items.DIAMOND, 3);
        final byte[] body = ITEM_CODEX.encodeBody(source, registryAccess);

        final TestTarget loaded = new TestTarget();
        ITEM_CODEX.decodeAndApply(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(body), registryAccess), loaded);

        assertThat(helper, "item stack", true, ItemStack.matches(source.stack, loaded.stack));
        helper.succeed();
    }
}
