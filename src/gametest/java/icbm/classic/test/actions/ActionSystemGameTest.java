package icbm.classic.test.actions;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.ActionDataBase;
import icbm.classic.lib.actions.PotentialAction;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.buildable.BuildableObjectRegistry;
import icbm.classic.lib.saving.SaveHandler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Tests the action framework (S1.08): a test action runs and edits the world through the listener handler,
 * a blocking condition prevents the action from running, a passing condition allows it, and action data
 * survives a {@link SaveHandler} save/load round trip.
 *
 * <p>The production action registry is locked once {@code ICBMClassic.init()} has run, so the test action
 * is registered into a test owned {@link BuildableObjectRegistry} instead of touching production logic.</p>
 */
public class ActionSystemGameTest
{
    private static final Identifier TEST_ACTION_ID = Identifier.fromNamespaceAndPath("icbmclassic-test", "place_stone");

    /** Test owned registry, production registries are already locked when gametests run */
    private static final BuildableObjectRegistry<IActionData> TEST_REGISTRY = new BuildableObjectRegistry<>("TEST_ACTIONS", "test.actions");

    /** Wraps the data as an {@link icbm.classic.lib.saving.ISavable} so its own save/load is exercised */
    private static final SaveHandler<PlaceStoneData> ROUND_TRIP = new SaveHandler<PlaceStoneData>()
            .mainRoot()
            .nodeSavable("data", data -> data)
            .base();

    static
    {
        TEST_REGISTRY.register(TEST_ACTION_ID, PlaceStoneData::new);
    }

    private static void assertThat(GameTestHelper helper, String name, Object expected, Object actual)
    {
        if (!Objects.equals(expected, actual))
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static Vec3 center(BlockPos pos)
    {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @GameTest
    public void actionRunsThroughListener(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.AIR);
        final BlockPos abs = helper.absolutePos(pos);

        final PlaceStoneData data = new PlaceStoneData();
        final IActionSource source = new ActionSource(helper.getLevel(), center(abs), null);
        final IAction action = data.create(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, source, null);

        final IActionStatus status = ICBMClassicAPI.ACTION_LISTENER.runAction(action);
        if (status.isBlocking())
        {
            helper.fail("action was blocked by the listener: " + status);
        }
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    @GameTest
    public void passingConditionRunsAction(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.AIR);
        final BlockPos abs = helper.absolutePos(pos);

        final PotentialAction potential = new PotentialAction();
        potential.setActionData(new PlaceStoneData());
        potential.withCondition(new TestCondition(true));

        final IActionStatus status = potential.doAction(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
        if (status.isBlocking())
        {
            helper.fail("passing condition should not block the action: " + status);
        }
        helper.assertBlockPresent(Blocks.STONE, pos);
        helper.succeed();
    }

    @GameTest
    public void blockingConditionPreventsAction(GameTestHelper helper)
    {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.AIR);
        final BlockPos abs = helper.absolutePos(pos);

        final PotentialAction potential = new PotentialAction();
        potential.setActionData(new PlaceStoneData());
        potential.withCondition(new TestCondition(false));

        final IActionStatus status = potential.doAction(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5, null);
        if (!status.isBlocking())
        {
            helper.fail("blocking condition should have stopped the action, got " + status);
        }
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.succeed();
    }

    @GameTest
    public void actionDataRoundTrips(GameTestHelper helper)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();

        final PlaceStoneData data = new PlaceStoneData();
        data.setValue(ActionFields.AREA_SIZE, 7F);
        data.setValue(ActionFields.IMPACTED, true);
        data.setTargetState(Blocks.GOLD_BLOCK.defaultBlockState());

        final CompoundTag tag = ROUND_TRIP.saveToTag(data, provider);

        final PlaceStoneData loaded = new PlaceStoneData();
        ROUND_TRIP.loadFromTag(loaded, tag, provider);

        assertThat(helper, "field map value AREA_SIZE", data.getValue(ActionFields.AREA_SIZE), loaded.getValue(ActionFields.AREA_SIZE));
        assertThat(helper, "field map value IMPACTED", data.getValue(ActionFields.IMPACTED), loaded.getValue(ActionFields.IMPACTED));
        assertThat(helper, "save handler value target_state", data.getTargetState(), loaded.getTargetState());
        helper.succeed();
    }

    /** Minimal condition used to drive the accept/reject paths */
    private static final class TestCondition implements ICondition
    {
        private final boolean pass;

        private TestCondition(boolean pass)
        {
            this.pass = pass;
        }

        @NonNull
        @Override
        public Identifier getRegistryKey()
        {
            return Identifier.fromNamespaceAndPath("icbmclassic-test", "always");
        }

        @Override
        public IActionStatus getCondition()
        {
            return pass ? ActionResponses.READY : ActionResponses.WAITING;
        }
    }

    /** Test action data; declares its extra field through a save handler and supports framework fields */
    public static final class PlaceStoneData extends ActionDataBase
    {
        private static final SaveHandler<PlaceStoneData> EXTRA_SAVE = new SaveHandler<PlaceStoneData>()
                .mainRoot()
                .nodeBlockState("target_state", PlaceStoneData::getTargetState, PlaceStoneData::setTargetState)
                .base();

        private BlockState targetState = Blocks.STONE.defaultBlockState();

        public BlockState getTargetState()
        {
            return targetState;
        }

        public PlaceStoneData setTargetState(BlockState targetState)
        {
            this.targetState = targetState;
            return this;
        }

        @Override
        public Collection<ActionField<?>> getFields()
        {
            return List.of(ActionFields.AREA_SIZE, ActionFields.IMPACTED);
        }

        @NonNull
        @Override
        public IAction create(Level world, double x, double y, double z, @NonNull IActionSource source, @Nullable IActionFieldProvider fieldAccessor)
        {
            return new PlaceStoneAction(world, new Vec3(x, y, z), source, this);
        }

        @NonNull
        @Override
        public Identifier getRegistryKey()
        {
            return TEST_ACTION_ID;
        }

        @NonNull
        @Override
        public IBuilderRegistry<IActionData> getRegistry()
        {
            return TEST_REGISTRY;
        }

        @Override
        public void save(ValueOutput out)
        {
            super.save(out);
            EXTRA_SAVE.save(this, out);
        }

        @Override
        public void load(ValueInput in)
        {
            super.load(in);
            EXTRA_SAVE.load(this, in);
        }
    }

    /** Test action that places the data's target block at the action position */
    private static final class PlaceStoneAction extends ActionBase
    {
        private PlaceStoneAction(Level world, Vec3 position, IActionSource source, IActionData actionData)
        {
            super(world, position, source, actionData);
        }

        @NonNull
        @Override
        public IActionStatus doAction()
        {
            final BlockState state = getActionData() instanceof PlaceStoneData data ? data.getTargetState() : Blocks.STONE.defaultBlockState();
            getWorld().setBlock(getBlockPos(), state, 3);
            return ActionResponses.COMPLETED;
        }
    }
}
