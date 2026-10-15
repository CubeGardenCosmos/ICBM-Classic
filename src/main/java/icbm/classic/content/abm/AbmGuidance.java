package icbm.classic.content.abm;

import icbm.classic.content.abm.math.Intercept;
import icbm.classic.content.abm.math.InterceptMath;
import icbm.classic.content.abm.math.Rotation;
import icbm.classic.content.abm.math.Vec3d;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class AbmGuidance
{
    public enum AbmState { BOOST, GUIDE, REACQUIRE, SELF_DESTRUCT }

    public static final float SELF_DESTRUCT_POWER = 2.0F;

    private AbmGuidance() { }

    public static void tick(EntityAntiBallisticMissile abm)
    {
        abm.decrementFuel();
        if (abm.getFuelLeft() <= 0) abm.setAbmState(AbmState.SELF_DESTRUCT);
        if (!(abm.level() instanceof ServerLevel level)) return;
        switch (abm.getAbmState())
        {
            case BOOST -> {
                abm.setDeltaMovement(0, ConfigAbm.BOOST_SPEED, 0);
                abm.incrementTicksInState();
                if (abm.getTicksInState() >= ConfigAbm.BOOST_TICKS)
                {
                    abm.setAbmState(AbmState.GUIDE);
                    if (abm.getTargetId() == null) abm.setAbmState(AbmState.REACQUIRE);
                }
            }
            case GUIDE -> guide(abm, level);
            case REACQUIRE -> reacquire(abm, level);
            case SELF_DESTRUCT -> abm.performSelfDestruct();
        }
    }

    private static void guide(EntityAntiBallisticMissile abm, ServerLevel level)
    {
        Entity target = abm.getTargetId() == null ? null : level.getEntity(abm.getTargetId());
        if (target == null || !target.isAlive())
        {
            abm.setAbmState(AbmState.REACQUIRE);
            return;
        }
        Vec3 targetVelocity = target.getDeltaMovement();
        Vec3 acceleration = abm.hasTargetVelocity() ? targetVelocity.subtract(abm.getTargetVelocityLastTick()) : Vec3.ZERO;
        abm.setTargetVelocityLastTick(targetVelocity);
        Vec3 position = abm.position();
        Vec3 aim = target.position();
        Intercept intercept = InterceptMath.solve(toMath(position), ConfigAbm.FLIGHT_SPEED, toMath(aim), toMath(targetVelocity), toMath(acceleration), abm.getFuelLeft());
        if (intercept != null) aim = toMinecraft(intercept.aim());
        Vec3 current = abm.getDeltaMovement();
        Vec3 direction = current.lengthSqr() < 1.0e-12 ? aim.subtract(position).normalize() : current.normalize();
        Vec3 rotated = toMinecraft(Rotation.rotateTowards(toMath(direction), toMath(aim.subtract(position)), Math.toRadians(ConfigAbm.MAX_TURN_DEGREES))).scale(ConfigAbm.FLIGHT_SPEED);
        abm.setDeltaMovement(rotated);
        abm.updateGuidanceRotation(rotated);
        abm.incrementTicksInState();
    }

    private static void reacquire(EntityAntiBallisticMissile abm, ServerLevel level)
    {
        abm.incrementTicksInState();
        if (abm.getTicksInState() % 10 == 0)
        {
            AABB search = abm.getBoundingBox().inflate(ConfigAbm.REACQUIRE_RANGE);
            Entity nearest = level.getEntities(abm, search, TileRadarStation::isThreat).stream()
                    .min(Comparator.comparingDouble(abm::distanceToSqr)).orElse(null);
            if (nearest != null)
            {
                abm.setTargetId(nearest.getUUID());
                abm.setAbmState(AbmState.GUIDE);
                return;
            }
        }
        if (abm.getTicksInState() >= ConfigAbm.REACQUIRE_TIMEOUT_TICKS) abm.setAbmState(AbmState.SELF_DESTRUCT);
    }

    private static Vec3d toMath(Vec3 v) { return new Vec3d(v.x, v.y, v.z); }
    private static Vec3 toMinecraft(Vec3d v) { return new Vec3(v.x(), v.y(), v.z()); }
}
