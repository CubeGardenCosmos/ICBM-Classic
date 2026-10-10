package icbm.classic.test.lib;

import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.test.util.SaveRoundTrip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;
import java.util.UUID;

public class SaveHandlerGameTest
{
    private static final String MOD_ID = "icbmclassic";

    /** Test mode exercising the enum node, stored and reloaded by name */
    private enum TestMode
    {
        ALPHA,
        BETA
    }

    /**
     * Nested {@link ISavable} with its own handler, used by the sub node of {@link SaveHandlerTestObject}
     */
    private static final class NestedData implements ISavable
    {
        private static final SaveHandler<NestedData> SAVE_LOGIC = new SaveHandler<NestedData>()
            .mainRoot()
                .nodeInteger("counter", data -> data.counter, (data, value) -> data.counter = value)
                .nodeString("label", data -> data.label, (data, value) -> data.label = value)
            .base();

        private int counter = 7;
        private String label = "nested";

        @Override
        public void save(ValueOutput out)
        {
            SAVE_LOGIC.save(this, out);
        }

        @Override
        public void load(ValueInput in)
        {
            SAVE_LOGIC.load(this, in);
        }
    }

    /**
     * POJO declaring every node type of {@link SaveHandler}: primitives, codec backed values, a nested
     * root, a nested main root entry and an {@link ISavable}.
     */
    private static final class SaveHandlerTestObject
    {
        private static final SaveHandler<SaveHandlerTestObject> SAVE_LOGIC = new SaveHandler<SaveHandlerTestObject>()
            .mainRoot()
                .nodeInteger("number", obj -> obj.number, (obj, value) -> obj.number = value)
                .nodeBoolean("flag", obj -> obj.flag, (obj, value) -> obj.flag = value)
                .nodeString("text", obj -> obj.text, (obj, value) -> obj.text = value)
                .nodeEnumString("mode", obj -> obj.mode, (obj, value) -> obj.mode = value, TestMode::valueOf)
                .nodeBlockPos("pos", obj -> obj.pos, (obj, value) -> obj.pos = value)
                .nodeVec3("vector", obj -> obj.vector, (obj, value) -> obj.vector = value)
                .nodeUUID("uuid", obj -> obj.uuid, (obj, value) -> obj.uuid = value)
                .nodeIdentifier("key", obj -> obj.key, (obj, value) -> obj.key = value)
                .nodeDouble("ratio", obj -> obj.ratio, (obj, value) -> obj.ratio = value)
                .nodeFloat("speed", obj -> obj.speed, (obj, value) -> obj.speed = value)
                .nodeByte("mask", obj -> obj.mask, (obj, value) -> obj.mask = value)
            .base()
            .addRoot("group")
                .nodeInteger("depth", obj -> obj.depth, (obj, value) -> obj.depth = value)
            .base()
            .mainRoot()
                .nodeSavable("sub", obj -> obj.sub)
            .base();

        private int number = 1;
        private boolean flag = false;
        private String text = "default";
        private TestMode mode = TestMode.ALPHA;
        private BlockPos pos = new BlockPos(0, 0, 0);
        private Vec3 vector = Vec3.ZERO;
        private UUID uuid = new UUID(0, 0);
        private Identifier key = Identifier.fromNamespaceAndPath(MOD_ID, "default");
        private double ratio = 0;
        private float speed = 0;
        private byte mask = 0;
        private int depth = 0;
        private final NestedData sub = new NestedData();

        private void mutate()
        {
            number = 42;
            flag = true;
            text = "icbm";
            mode = TestMode.BETA;
            pos = new BlockPos(-12, 64, 300);
            vector = new Vec3(1.5, -2.25, 3.75);
            uuid = new UUID(0x0123456789ABCDEFL, 0xFEDCBA9876543210L);
            key = Identifier.fromNamespaceAndPath("minecraft", "diamond");
            ratio = 0.125;
            speed = 4.5F;
            mask = (byte) 0x5A;
            depth = 9;
            sub.counter = 99;
            sub.label = "loaded";
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
    public void saveHandlerRoundTripsEveryNodeType(GameTestHelper helper)
    {
        final SaveHandlerTestObject source = new SaveHandlerTestObject();
        source.mutate();

        final CompoundTag tag = source.SAVE_LOGIC.saveToTag(source, helper.getLevel().registryAccess());

        final SaveHandlerTestObject loaded = new SaveHandlerTestObject();
        loaded.SAVE_LOGIC.loadFromTag(loaded, tag, helper.getLevel().registryAccess());

        assertThat(helper, "number", source.number, loaded.number);
        assertThat(helper, "flag", source.flag, loaded.flag);
        assertThat(helper, "text", source.text, loaded.text);
        assertThat(helper, "mode", source.mode, loaded.mode);
        assertThat(helper, "pos", source.pos, loaded.pos);
        assertThat(helper, "vector", source.vector, loaded.vector);
        assertThat(helper, "uuid", source.uuid, loaded.uuid);
        assertThat(helper, "key", source.key, loaded.key);
        assertThat(helper, "ratio", source.ratio, loaded.ratio);
        assertThat(helper, "speed", source.speed, loaded.speed);
        assertThat(helper, "mask", source.mask, loaded.mask);
        assertThat(helper, "depth", source.depth, loaded.depth);
        assertThat(helper, "sub.counter", source.sub.counter, loaded.sub.counter);
        assertThat(helper, "sub.label", source.sub.label, loaded.sub.label);

        // the nested root has to nest under its key and the main root entries stay at the top
        assertThat(helper, "group root", true, tag.getCompoundOrEmpty("group").contains("depth"));
        assertThat(helper, "main root", true, tag.contains("number"));
        assertThat(helper, "savable child", true, tag.getCompoundOrEmpty("sub").contains("counter"));

        helper.succeed();
    }

    @GameTest
    public void emptySaveLeavesConstructorDefaults(GameTestHelper helper)
    {
        final SaveHandlerTestObject loaded = new SaveHandlerTestObject();
        loaded.SAVE_LOGIC.loadFromTag(loaded, new CompoundTag(), helper.getLevel().registryAccess());

        assertThat(helper, "number", 1, loaded.number);
        assertThat(helper, "flag", false, loaded.flag);
        assertThat(helper, "text", "default", loaded.text);
        assertThat(helper, "mode", TestMode.ALPHA, loaded.mode);
        assertThat(helper, "pos", new BlockPos(0, 0, 0), loaded.pos);
        assertThat(helper, "vector", Vec3.ZERO, loaded.vector);
        assertThat(helper, "key", Identifier.fromNamespaceAndPath(MOD_ID, "default"), loaded.key);
        assertThat(helper, "depth", 0, loaded.depth);
        assertThat(helper, "sub.counter", 7, loaded.sub.counter);
        assertThat(helper, "sub.label", "nested", loaded.sub.label);

        helper.succeed();
    }

    @GameTest
    public void unknownEnumKeepsDefault(GameTestHelper helper)
    {
        final SaveHandlerTestObject source = new SaveHandlerTestObject();
        final CompoundTag tag = source.SAVE_LOGIC.saveToTag(source, helper.getLevel().registryAccess());
        tag.putString("mode", "NOT_A_MODE");

        final SaveHandlerTestObject loaded = new SaveHandlerTestObject();
        loaded.SAVE_LOGIC.loadFromTag(loaded, tag, helper.getLevel().registryAccess());

        assertThat(helper, "mode", TestMode.ALPHA, loaded.mode);

        helper.succeed();
    }

    @GameTest
    public void blockEntityRoundTripThroughSaveRoundTrip(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final BlockState state = Blocks.COMPARATOR.defaultBlockState();
        final ComparatorBlockEntity be = new ComparatorBlockEntity(pos, state);
        be.setLevel(helper.getLevel());

        SaveRoundTrip.assertRoundTrip(helper, be,
            blockEntity -> ((ComparatorBlockEntity) blockEntity).setOutputSignal(7),
            (source, loaded) -> ((ComparatorBlockEntity) source).getOutputSignal() == ((ComparatorBlockEntity) loaded).getOutputSignal());

        helper.succeed();
    }
}
