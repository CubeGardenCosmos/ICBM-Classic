package icbm.classic.test.abm;
import icbm.classic.ICBMClassic;

import icbm.classic.content.abm.AbmGuidance;
import icbm.classic.content.abm.EntityAntiBallisticMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

public class AbmGuidanceGameTest
{
    private static final class StationaryTarget extends EntityExplosiveMissile
    {
        private StationaryTarget(Level level) { super(ModEntities.EXPLOSIVE_MISSILE, level); }
        @Override public void tick() { }
    }

    @GameTest(maxTicks = 100, padding = 16)
    public void noTargetReacquiresThenSelfDestructsWithoutBlockDamage(GameTestHelper helper)
    {
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.STONE);
        BlockPos groundAbs = helper.absolutePos(ground);
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        abm.setPos(groundAbs.getX() + 0.5, groundAbs.getY() + 1, groundAbs.getZ() + 0.5);
        helper.getLevel().addFreshEntity(abm);
        helper.runAfterDelay(AbmGuidance.BOOST_TICKS + 2, () -> {
            helper.assertTrue(abm.getAbmState() == AbmGuidance.AbmState.REACQUIRE,
                    "targetless ABM must enter REACQUIRE after boost");
            helper.runAfterDelay(45, () -> {
                helper.assertTrue(abm.isRemoved(), "ABM must self-destruct after reacquire timeout");
                helper.assertBlockState(ground, state -> state.is(Blocks.STONE),
                        state -> net.minecraft.network.chat.Component.literal("NONE explosion damaged ground: " + state));
                helper.succeed();
            });
        });
    }

    @GameTest(maxTicks = 30, padding = 16)
    public void guideTurnsTowardTargetWithinTurnLimit(GameTestHelper helper)
    {
        EntityAntiBallisticMissile abm = new EntityAntiBallisticMissile(ModEntities.MISSILE_SAM, helper.getLevel());
        BlockPos abmPos = helper.absolutePos(new BlockPos(2, 3, 2));
        abm.setPos(abmPos.getX() + 0.5, abmPos.getY() + 0.5, abmPos.getZ() + 0.5);
        abm.setDeltaMovement(4, 0, 0);
        StationaryTarget target = new StationaryTarget(helper.getLevel());
        BlockPos targetPos = helper.absolutePos(new BlockPos(2, 3, 22));
        target.setPos(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        helper.getLevel().addFreshEntity(target);
        abm.setTargetId(target.getUUID());
        abm.setAbmState(AbmGuidance.AbmState.GUIDE);

        double previous = angle(abm.getDeltaMovement(), target.position().subtract(abm.position()));
        Vec3 previousDirection = abm.getDeltaMovement();
        for (int tick = 0; tick < 5; tick++)
        {
            AbmGuidance.tick(abm);
            double currentAngle = angle(abm.getDeltaMovement(), target.position().subtract(abm.position()));
            double turn = angle(abm.getDeltaMovement(), previousDirection);
            ICBMClassic.LOGGER.info("ABM guidance tick {}: angle to target={} turn={} degrees", tick, currentAngle, turn);
            helper.assertTrue(currentAngle < previous + 1.0e-6, "angle to target must decrease each guidance tick");
            helper.assertTrue(turn <= 15.0001, "guidance turn exceeded max_turn_degrees: " + turn);
            previous = currentAngle;
            previousDirection = abm.getDeltaMovement();
        }
        helper.succeed();
    }

    private static double angle(Vec3 a, Vec3 b)
    {
        double denominator = a.length() * b.length();
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, a.dot(b) / denominator))));
    }
}
