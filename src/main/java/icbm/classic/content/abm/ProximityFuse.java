package icbm.classic.content.abm;

import icbm.classic.content.abm.math.InterceptMath;
import icbm.classic.content.abm.math.Vec3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ProximityFuse
{
    public static boolean check(EntityAntiBallisticMissile abm, Entity target)
    {
        Vec3 previous = abm.getPreviousPositionThisTick();
        Vec3 current = abm.position();
        // Entity.xo/yo/zo are the previous-tick coordinates maintained by Entity.tick().
        Vec3 targetPrevious = new Vec3(target.xo, target.yo, target.zo);
        double distance = InterceptMath.closestApproach(toMath(previous), toMath(current), toMath(targetPrevious), toMath(target.position()));
        if (distance > ConfigAbm.PROXIMITY_RADIUS) return false;
        if (abm.rollProximityFuseHit()) abm.killInterceptedTarget(target);
        else abm.selfDestruct();
        return true;
    }

    private static Vec3d toMath(Vec3 vector) { return new Vec3d(vector.x, vector.y, vector.z); }
    private ProximityFuse() { }
}
