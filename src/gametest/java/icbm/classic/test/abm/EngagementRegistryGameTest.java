package icbm.classic.test.abm;

import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.abm.AbmGuidance;
import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registration lifecycle of ABM engagements (perf-audit C2) and the retention window of the interception
 * history (perf-audit C3).
 * <p>
 * Everything is frozen in place ({@code setFreezeMotion(true)}) so the scenarios are decided by registration
 * state alone and no proximity fuse can resolve mid-test.
 */
public class EngagementRegistryGameTest
{
    /** Stationary threat missile; the reacquire search only accepts entities the radar classifies as threats. */
    private static final class StationaryThreat extends EntityExplosiveMissile
    {
        private StationaryThreat(Level level)
        {
            super(ModEntities.EXPLOSIVE_MISSILE, level);
        }

        @Override
        public void tick()
        {
            // Keep the test trajectory stationary; only the reacquire search looks at these entities
        }
    }

    private static StationaryThreat threat(GameTestHelper helper, BlockPos pos)
    {
        StationaryThreat missile = new StationaryThreat(helper.getLevel());
        missile.explosive.setStack(new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey())));
        missile.setNoGravity(true);
        missile.setFreezeMotion(true);
        missile.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        helper.assertTrue(helper.getLevel().addFreshEntity(missile), "test threat must spawn");
        return missile;
    }

    private static EntityAntiBallisticMissile abm(GameTestHelper helper, BlockPos pos)
    {
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        abm.setFreezeMotion(true);
        abm.setDeltaMovement(Vec3.ZERO);
        abm.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        helper.assertTrue(helper.getLevel().addFreshEntity(abm), "test ABM must spawn");
        return abm;
    }

    /**
     * An ABM that loses its target must release that target's registration, and the target it locks onto
     * afterwards must be registered - without counting a target that is already at the in-flight limit.
     */
    @GameTest(maxTicks = 100, padding = 64)
    public void reacquireMigratesEngagementAndSkipsSaturatedTargets(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final BlockPos hunterLocal = new BlockPos(2, 4, 2);

        // A: the original target, killed as soon as the fire control had registered the engagement
        final StationaryThreat abandoned = threat(helper, helper.absolutePos(hunterLocal.offset(0, 0, 8)));
        // B: the nearest candidate, held at the in-flight limit so it may not be picked
        final StationaryThreat saturated = threat(helper, helper.absolutePos(hunterLocal.offset(0, 0, 18)));
        // C: further away and eligible, the only target the ABM is allowed to lock
        final StationaryThreat eligible = threat(helper, helper.absolutePos(hunterLocal.offset(0, 0, 28)));

        final EntityAntiBallisticMissile hunter = abm(helper, helper.absolutePos(hunterLocal));
        hunter.setTargetId(abandoned.getUUID());
        hunter.setAbmState(AbmGuidance.AbmState.GUIDE);
        // FireControl registers the interceptor when it launches it; this is that single registration
        EngagementRegistry.register(level, abandoned.getUUID(), hunter.getUUID());

        final int limit = Math.max(1, ConfigAbm.MAX_INTERCEPTORS_PER_TARGET);
        final List<EntityAntiBallisticMissile> fillers = new ArrayList<>();
        for (int i = 0; i < limit; i++)
        {
            EntityAntiBallisticMissile filler = abm(helper, helper.absolutePos(hunterLocal.offset(0, 20, 8 + i)));
            filler.setTargetId(saturated.getUUID());
            EngagementRegistry.register(level, saturated.getUUID(), filler.getUUID());
            fillers.add(filler);
        }
        helper.assertTrue(EngagementRegistry.activeCount(level, abandoned.getUUID()) == 1,
                "the hunter must start registered against its original target");
        helper.assertTrue(!EngagementRegistry.canEngage(level, saturated.getUUID()),
                "the nearer candidate must be at the in-flight limit before the ABM searches");
        helper.assertTrue(hunter.distanceToSqr(saturated) < hunter.distanceToSqr(eligible),
                "the saturated candidate must be the nearer one, so only the in-flight limit can rule it out");

        abandoned.discard();

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(hunter.getAbmState() == AbmGuidance.AbmState.REACQUIRE,
                    "losing the target must send the ABM to REACQUIRE, state=" + hunter.getAbmState());
            helper.assertTrue(EngagementRegistry.activeCount(level, abandoned.getUUID()) == 0,
                    "losing the lock must release the registration on the old target");
            helper.assertTrue(EngagementRegistry.canEngage(level, abandoned.getUUID()),
                    "the abandoned target must be engageable again");
        });

        helper.runAfterDelay(25, () -> {
            helper.assertTrue(eligible.getUUID().equals(hunter.getTargetId()),
                    "reacquire must skip the saturated target and lock the eligible one, locked on " + hunter.getTargetId());
            helper.assertTrue(EngagementRegistry.activeCount(level, eligible.getUUID()) == 1,
                    "the new target must be registered for the hunter");
            helper.assertTrue(EngagementRegistry.activeCount(level, abandoned.getUUID()) == 0,
                    "the old target must not keep a residual registration");
            helper.assertTrue(EngagementRegistry.activeCount(level, saturated.getUUID()) == limit,
                    "the saturated target must keep exactly its own interceptors");
            helper.assertTrue(!EngagementRegistry.canEngage(level, eligible.getUUID()),
                    "the hunter must count towards the in-flight limit of the target it switched to");

            hunter.discard();
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(EngagementRegistry.activeCount(level, eligible.getUUID()) == 0,
                        "removal must release the engagement of the target the ABM switched to");
                fillers.forEach(Entity::discard);
                helper.succeed();
            });
        });
    }

    /**
     * The interception history must outlive a single reader (several radars identify the same interception)
     * and must expire after its retention window instead of being kept for the lifetime of the world.
     */
    @GameTest(maxTicks = 300, padding = 8)
    public void interceptionHistoryServesEveryRadarInTheWindowThenExpires(GameTestHelper helper)
    {
        final ServerLevel level = helper.getLevel();
        final UUID targetId = UUID.randomUUID();
        EngagementRegistry.markIntercepted(level, targetId);

        helper.assertTrue(EngagementRegistry.wasIntercepted(level, targetId),
                "the first radar must observe the interception");
        helper.assertTrue(EngagementRegistry.wasIntercepted(level, targetId),
                "a second radar must still observe the same interception");

        helper.runAfterDelay(EngagementRegistry.INTERCEPT_HISTORY_TICKS - 1, () ->
                helper.assertTrue(EngagementRegistry.wasIntercepted(level, targetId),
                        "the interception must stay readable for the whole retention window"));

        helper.runAfterDelay(EngagementRegistry.INTERCEPT_HISTORY_TICKS + 2, () -> {
            helper.assertTrue(!EngagementRegistry.wasIntercepted(level, targetId),
                    "an interception older than the retention window must expire");
            helper.succeed();
        });
    }
}
