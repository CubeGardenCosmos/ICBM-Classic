package icbm.classic.test.abm;

import icbm.classic.content.abm.AbmGuidance;
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
    private static final java.util.concurrent.ConcurrentMap<UUID, AtomicInteger> interceptedCounts =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<UUID> cancelledTargets = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final InterceptEvents.Intercepted OBSERVER = (level, abm, target, point) -> {
        AtomicInteger count = interceptedCounts.get(target.getUUID());
        if (count != null) count.incrementAndGet();
        return !cancelledTargets.contains(target.getUUID());
    };
    static { InterceptEvents.INTERCEPTED.register(OBSERVER); }

    private static class TestMissile extends EntityExplosiveMissile
    {
        TestMissile(Level level) { super(ModEntities.EXPLOSIVE_MISSILE, level); }
        @Override public void tick() { xo = getX(); yo = getY(); zo = getZ(); if (getDeltaMovement().lengthSqr() > 0) setPos(position().add(getDeltaMovement())); }
    }

    /**
     * Resolves the fuse roll and warhead policy deterministically. GameTest batches run concurrently in one
     * shared level, so mutating the global {@link icbm.classic.content.abm.ConfigAbm} here would leak into
     * the sibling tests; overriding the per-entity hooks instead keeps every test isolated.
     */
    private static class TestAbm extends EntityAntiBallisticMissile
    {
        private final boolean fuseHit;
        private final boolean detonateWarhead;
        TestAbm(Level level, boolean fuseHit, boolean detonateWarhead)
        {
            super(ModEntities.MISSILE_SAM, level);
            this.fuseHit = fuseHit;
            this.detonateWarhead = detonateWarhead;
        }
        @Override protected boolean rollProximityFuseHit() { return fuseHit; }
        @Override protected boolean shouldDetonateInterceptedWarhead() { return detonateWarhead; }
    }

    @GameTest(maxTicks = 80, padding = 32)
    public void crossingTargetInterceptsOnceWithoutBlockDamage(GameTestHelper helper)
    {
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);
        TestMissile target = spawnTarget(helper, new Vec3(3.5, 6.5, 3.5));
        target.setDeltaMovement(0, 0, 1);
        interceptedCounts.put(target.getUUID(), new AtomicInteger());
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(3.5, 6.5, 5.5), new Vec3(0, 0, -4), true, false);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(target.isRemoved(), "intercepted target must be removed");
            helper.assertTrue(interceptedCounts.get(target.getUUID()).get() == 1, "intercept event must fire exactly once");
            interceptedCounts.remove(target.getUUID());
            helper.assertBlockState(ground, state -> state.is(Blocks.STONE), state -> net.minecraft.network.chat.Component.literal("visual explosion damaged ground"));
            helper.assertTrue(abm.isRemoved(), "ABM must be removed after successful interception");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 32)
    public void listenerCanCancelInterception(GameTestHelper helper)
    {
        TestMissile target = spawnTarget(helper, new Vec3(3.5, 6.5, 3.5));
        cancelledTargets.add(target.getUUID());
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(3.5, 6.5, 5.5), new Vec3(0, 0, -4), true, false);
        helper.runAfterDelay(50, () -> {
            cancelledTargets.remove(target.getUUID());
            helper.assertTrue(!target.isRemoved(), "cancelled intercept must leave target alive");
            helper.assertTrue(abm.isRemoved(), "ABM must self-destruct after cancelled intercept");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 32)
    public void failedHitChanceSelfDestructs(GameTestHelper helper)
    {
        TestMissile target = spawnTarget(helper, new Vec3(3.5, 6.5, 3.5));
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(3.5, 6.5, 5.5), new Vec3(0, 0, -4), false, false);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!target.isRemoved(), "missed target must remain alive");
            helper.assertTrue(abm.isRemoved(), "ABM must self-destruct after a fuse miss");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 32)
    public void highSpeedCrossingResolvesFuse(GameTestHelper helper)
    {
        TestMissile target = spawnTarget(helper, new Vec3(3.5, 2.0, 3.5));
        target.setDeltaMovement(0, 0, 6);
        spawnAbm(helper, target, new Vec3(3.5, 2.0, 5.5), new Vec3(0, 0, -4), true, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(target.isRemoved(), "high-speed crossing must be detected by closest approach");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 80, padding = 32)
    public void targetWarheadCanDamageNearbyBlock(GameTestHelper helper)
    {
        BlockPos stone = new BlockPos(4, 6, 4);
        helper.setBlock(stone, Blocks.STONE);
        TestMissile target = spawnTarget(helper, new Vec3(3.5, 6.5, 3.5));
        EntityAntiBallisticMissile abm = spawnAbm(helper, target, new Vec3(3.5, 6.5, 5.5), new Vec3(0, 0, -4), true, true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(abm.isRemoved(), "ABM must be removed after a warhead-enabled intercept");
            helper.assertTrue(!helper.getBlockState(stone).is(Blocks.STONE), "target warhead should damage nearby block");
            helper.succeed();
        });
    }

    private static TestMissile spawnTarget(GameTestHelper helper, Vec3 position)
    {
        TestMissile target = new TestMissile(helper.getLevel());
        target.setPos(absolute(helper, position));
        // TestMissile overrides tick() without calling super.tick(), so Entity's previous-position fields
        // (xo/yo/zo) are never synced by baseTick. Seed them so the proximity fuse reads a valid previous
        // position even when the fuse runs before this entity's first tick in the same level tick.
        target.xo = target.getX();
        target.yo = target.getY();
        target.zo = target.getZ();
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static EntityAntiBallisticMissile spawnAbm(GameTestHelper helper, TestMissile target, Vec3 position, Vec3 velocity, boolean fuseHit, boolean detonateWarhead)
    {
        EntityAntiBallisticMissile abm = new TestAbm(helper.getLevel(), fuseHit, detonateWarhead);
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
