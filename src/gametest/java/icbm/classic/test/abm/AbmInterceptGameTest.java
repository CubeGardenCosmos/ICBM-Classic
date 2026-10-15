package icbm.classic.test.abm;

import icbm.classic.content.abm.AbmGuidance;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.abm.InterceptEvents;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AbmInterceptGameTest
{
    private static final AtomicInteger interceptedCount = new AtomicInteger();
    private static volatile UUID observedTarget;
    private static volatile UUID cancelledTarget;
    private static volatile boolean cancel;
    private static final InterceptEvents.Intercepted OBSERVER = (level, abm, target, point) -> {
        if (target.getUUID().equals(observedTarget)) interceptedCount.incrementAndGet();
        return !cancel || !target.getUUID().equals(cancelledTarget);
    };
    static { InterceptEvents.INTERCEPTED.register(OBSERVER); }

    private static class TestMissile extends EntityExplosiveMissile
    {
        TestMissile(Level level) { super(ModEntities.EXPLOSIVE_MISSILE, level); }
        @Override public void tick() { xo = getX(); yo = getY(); zo = getZ(); if (getDeltaMovement().lengthSqr() > 0) setPos(position().add(getDeltaMovement())); }
    }

    @GameTest(maxTicks = 80, padding = 8)
    public void crossingTargetInterceptsOnceWithoutBlockDamage(GameTestHelper helper)
    {
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);
        TestMissile target = spawnTarget(helper, new Vec3(5.5, 6.5, 5.5));
        target.setDeltaMovement(0, 0, 1);
        observedTarget = target.getUUID(); interceptedCount.set(0);
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(5.5, 6.5, 7.5), new Vec3(0, 0, -4));
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(target.isRemoved(), "intercepted target must be removed");
            helper.assertTrue(interceptedCount.get() == 1, "intercept event must fire exactly once");
            helper.assertBlockState(ground, state -> state.is(Blocks.STONE), state -> net.minecraft.network.chat.Component.literal("visual explosion damaged ground"));
            helper.assertTrue(abm.isRemoved(), "ABM must be removed after successful interception");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 8)
    public void listenerCanCancelInterception(GameTestHelper helper)
    {
        TestMissile target = spawnTarget(helper, new Vec3(5.5, 6.5, 5.5));
        cancelledTarget = target.getUUID(); cancel = true;
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(5.5, 6.5, 7.5), new Vec3(0, 0, -4));
        helper.runAfterDelay(50, () -> {
            cancel = false;
            helper.assertTrue(!target.isRemoved(), "cancelled intercept must leave target alive");
            helper.assertTrue(abm.isRemoved(), "ABM must self-destruct after cancelled intercept");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 8)
    public void failedHitChanceSelfDestructs(GameTestHelper helper)
    {
        double oldChance = ConfigAbm.HIT_CHANCE;
        ConfigAbm.HIT_CHANCE = 0;
        TestMissile target = spawnTarget(helper, new Vec3(5.5, 6.5, 5.5));
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(5.5, 6.5, 7.5), new Vec3(0, 0, -4));
        helper.runAfterDelay(50, () -> {
            ConfigAbm.HIT_CHANCE = oldChance;
            helper.assertTrue(!target.isRemoved(), "missed target must remain alive");
            helper.assertTrue(abm.isRemoved(), "ABM must self-destruct after a fuse miss");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 8)
    public void highSpeedCrossingResolvesFuse(GameTestHelper helper)
    {
        TestMissile target = spawnTarget(helper, new Vec3(5.5, 6.5, 3.5));
        target.setDeltaMovement(0, 0, 6);
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(5.5, 6.5, 9.5), new Vec3(0, 0, -4));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(target.isRemoved(), "high-speed crossing must be detected by closest approach");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 8)
    public void targetWarheadCanDamageNearbyBlock(GameTestHelper helper)
    {
        boolean oldWarhead = ConfigAbm.TARGET_WARHEAD_ON_KILL;
        ConfigAbm.TARGET_WARHEAD_ON_KILL = true;
        BlockPos stone = new BlockPos(6, 6, 6);
        helper.setBlock(stone, Blocks.STONE);
        TestMissile target = spawnTarget(helper, new Vec3(5.5, 6.5, 5.5));
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(5.5, 6.5, 7.5), new Vec3(0, 0, -4));
        helper.runAfterDelay(5, () -> {
            ConfigAbm.TARGET_WARHEAD_ON_KILL = oldWarhead;
            helper.assertTrue(abm.isRemoved(), "ABM must be removed after a warhead-enabled intercept");
            helper.assertTrue(!helper.getBlockState(stone).is(Blocks.STONE), "target warhead should damage nearby block");
            helper.succeed();
        });
    }

    private static TestMissile spawnTarget(GameTestHelper helper, Vec3 position)
    {
        TestMissile target = new TestMissile(helper.getLevel());
        target.setPos(absolute(helper, position));
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static EntityAntiBallisticMissile spawnAbm(GameTestHelper helper, TestMissile target, Vec3 position, Vec3 velocity)
    {
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        abm.setPos(absolute(helper, position));
        abm.setTargetId(target.getUUID());
        abm.setAbmState(AbmGuidance.AbmState.GUIDE);
        abm.setDeltaMovement(velocity);
        helper.getLevel().addFreshEntity(abm);
        return abm;
    }

    private static Vec3 absolute(GameTestHelper helper, Vec3 position)
    {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return position.add(origin.getX(), origin.getY(), origin.getZ());
    }
}
