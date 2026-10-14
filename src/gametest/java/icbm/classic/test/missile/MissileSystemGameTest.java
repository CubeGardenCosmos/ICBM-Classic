package icbm.classic.test.missile;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.content.missile.logic.flight.ArcFlightLogic;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.missile.logic.flight.DirectFlightLogic;
import icbm.classic.content.missile.logic.flight.FollowTargetLogic;
import icbm.classic.content.missile.logic.flight.WarmupFlightLogic;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.flight.move.MoveByFacingLogic;
import icbm.classic.content.missile.logic.flight.move.MoveByVec3Logic;
import icbm.classic.content.missile.logic.flight.move.MoveForTicksLogic;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.missile.logic.source.cause.RedstoneCause;
import icbm.classic.content.missile.logic.targeting.BallisticTargetingData;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Covers the S4.01 wiring that the pure logic tests can not reach:
 *
 * <ul>
 *     <li>both missile builder registries are built by {@code ICBMClassic#onInitialize} and locked,</li>
 *     <li>every flight logic / target data of registry-ids.md is resolvable by its 1.12 registry name
 *     (including the {@code ballistic} name that must stay gone, ADR 0002),</li>
 *     <li>the missile causes are registered with the action cause registry (ActionSystem),</li>
 *     <li>a flight logic survives a registry save/load round trip.</li>
 * </ul>
 */
public class MissileSystemGameTest
{
    @GameTest(maxTicks = 20)
    public void missileRegistriesAreBuiltAndLocked(GameTestHelper helper)
    {
        final IBuilderRegistry<IMissileFlightLogic> flightRegistry = ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY;
        final IBuilderRegistry<IMissileTarget> targetRegistry = ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY;

        helper.assertTrue(flightRegistry != null, "ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY was not assigned");
        helper.assertTrue(targetRegistry != null, "ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY was not assigned");
        helper.assertTrue(flightRegistry.isLocked(), "missile flight logic registry was not locked");
        helper.assertTrue(targetRegistry.isLocked(), "missile target data registry was not locked");

        final Identifier[] flightLogic = {
            DirectFlightLogic.REG_NAME,
            ArcFlightLogic.REG_NAME,
            WarmupFlightLogic.REG_NAME,
            MoveByFacingLogic.REG_NAME,
            MoveByVec3Logic.REG_NAME,
            MoveForTicksLogic.REG_NAME,
            DeadFlightLogic.REG_NAME,
            FollowTargetLogic.REG_NAME,
        };
        for (Identifier id : flightLogic)
        {
            helper.assertTrue(flightRegistry.getOrBuild(id) != null, "flight logic " + id + " is missing from the registry");
        }

        // BallisticFlightLogicOld (1.12 "ballistic") is dropped with the legacy save support, ADR 0002
        helper.assertTrue(flightRegistry.getOrBuild(ICBMConstants.id("ballistic")) == null,
                "the dropped BallisticFlightLogicOld was registered again");

        helper.assertTrue(targetRegistry.getOrBuild(BasicTargetData.REG_NAME) instanceof BasicTargetData,
                "basic target data is missing from the registry");
        helper.assertTrue(targetRegistry.getOrBuild(BallisticTargetingData.REG_NAME) instanceof BallisticTargetingData,
                "ballistic target data is missing from the registry");
        // 1.12 reserved the SAM name with a builder answering null, SAMTargetData is ported in S4.04
        helper.assertTrue(targetRegistry.getOrBuild(ICBMConstants.id("anti.missile")) == null,
                "the reserved anti.missile target data name returned an instance");

        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void missileCausesAreRegistered(GameTestHelper helper)
    {
        final IBuilderRegistry<IActionCause> registry = ICBMClassicAPI.ACTION_CAUSE_REGISTRY;
        helper.assertTrue(registry != null, "ICBMClassicAPI.ACTION_CAUSE_REGISTRY was not assigned");
        helper.assertTrue(registry.isLocked(), "action cause registry was not locked");

        helper.assertTrue(registry.getOrBuild(EntityCause.REG_NAME) instanceof EntityCause,
                "EntityCause is missing from the action cause registry");
        helper.assertTrue(registry.getOrBuild(CausedByBlock.REG_NAME) instanceof CausedByBlock,
                "CausedByBlock is missing from the action cause registry");
        helper.assertTrue(registry.getOrBuild(RedstoneCause.REG_NAME) instanceof RedstoneCause,
                "RedstoneCause is missing from the action cause registry");

        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void flightLogicSurvivesRegistryRoundTrip(GameTestHelper helper)
    {
        final IBuilderRegistry<IMissileFlightLogic> registry = ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY;
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();

        final MoveForTicksLogic original = new MoveForTicksLogic();
        original.setTicks(42);
        original.setDirection(Direction.UP);
        original.setAcceleration(0.25);

        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        registry.save(original, out);
        final CompoundTag tag = out.buildResult();

        final IMissileFlightLogic loaded = registry.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
        if (!(loaded instanceof MoveForTicksLogic))
        {
            helper.fail("flight logic did not load back as MoveForTicksLogic, saved as " + tag + " and loaded " + loaded);
            return;
        }

        final MoveForTicksLogic copy = (MoveForTicksLogic) loaded;
        helper.assertTrue(copy.getTicks() == 42, "ticks did not survive the round trip, got " + copy.getTicks());
        helper.assertTrue(copy.getDirection() == Direction.UP, "direction did not survive the round trip, got " + copy.getDirection());
        helper.assertTrue(copy.getAcceleration() == 0.25, "acceleration did not survive the round trip, got " + copy.getAcceleration());
        // the chained next step is part of the parent FlightLogic save
        helper.assertTrue(copy.getNextStep() == null, "next step should stay null, got " + copy.getNextStep());

        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void arcFlightLogicFliesUpAndTowardsTheTarget(GameTestHelper helper)
    {
        final BlockPos origin = helper.absolutePos(new BlockPos(1, 4, 1));
        final TestMissile missile = new TestMissile(helper.getLevel());
        missile.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        // 8 blocks away on x -> flatDistance 8, arcHeightMax 8, flightTime 16, acceleration 0.21875
        missile.setTargetData(new BasicTargetData(origin.getX() + 8.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D));
        final ArcFlightLogic logic = new ArcFlightLogic();
        missile.setFlightLogic(logic);
        missile.launch();

        // one tick is enough and it keeps the missile inside the game test structure: the flight logic locks
        // in the arc on its first tick and the projectile moves by the launch motion in the same tick
        missile.tick();

        final Vec3 motion = missile.getDeltaMovement();
        // launch motion = (deltaPathX / flightTime, acceleration * flightTime / 2, deltaPathZ / flightTime)
        if (Math.abs(motion.x - 0.5D) > 1e-6D || Math.abs(motion.y - 1.75D) > 1e-6D || motion.z != 0.0D)
        {
            // dump the arc state (start/end, flight time, acceleration) through its own save handler so a
            // failure reports why the path was not calculated as expected
            final HolderLookup.Provider provider = helper.getLevel().registryAccess();
            final TagValueOutput state = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
            logic.save(state);
            helper.fail("arc did not set the launch motion, motion=" + motion + " pos=" + missile.position()
                    + " target=" + missile.getTargetData().getX() + "," + missile.getTargetData().getY() + ","
                    + missile.getTargetData().getZ() + " removed=" + missile.isRemoved() + " state=" + state.buildResult());
            return;
        }

        helper.assertTrue(missile.getX() > origin.getX() + 0.5D, "missile never moved, pos=" + missile.position());
        helper.assertTrue(!missile.impacted(), "missile impacted while flying in open air");
        helper.assertTrue(!missile.isRemoved(), "missile was removed while flying in open air");

        missile.discard();
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void missileImpactOnWallRunsTheImpactCallback(GameTestHelper helper)
    {
        // wall 5 blocks down +x of the launch point
        for (int y = 4; y < 6; y++)
        {
            for (int z = 1; z < 4; z++)
            {
                helper.setBlock(5, y, z, Blocks.STONE);
            }
        }

        final BlockPos origin = helper.absolutePos(new BlockPos(1, 4, 1));
        final TestMissile missile = new TestMissile(helper.getLevel());
        missile.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        // straight flight (2 blocks per tick, no gravity while it has fuel) into the wall
        missile.setTargetData(new BasicTargetData(origin.getX() + 20.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D));
        missile.setFlightLogic(new DirectFlightLogic(200));
        missile.launch();

        for (int i = 0; i < 6 && !missile.isRemoved(); i++)
        {
            missile.tick();
        }

        helper.assertTrue(missile.impacted(), "missile flew through/next to the wall without an impact");
        helper.assertTrue(missile.isRemoved(), "the impact callback should have destroyed the missile");

        helper.succeed();
    }

    /**
     * Minimal concrete missile for the runtime tests. The real entity types are registered in S4.02
     * ({@code ModEntities}), the test builds the entity directly so it does not need a registered type.
     * The vanilla entity type is only a placeholder, nothing reads it.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class TestMissile extends EntityMissile<TestMissile>
    {
        TestMissile(Level level)
        {
            super((EntityType) EntityTypes.MARKER, level);
        }

        @Override
        public ItemStack toStack()
        {
            return ItemStack.EMPTY;
        }

        boolean impacted()
        {
            return hasImpacted;
        }
    }
}
