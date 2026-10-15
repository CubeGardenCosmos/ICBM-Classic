package icbm.classic.content.abm.math;

import org.jspecify.annotations.Nullable;

public final class TrajectoryPredictor
{
    private TrajectoryPredictor()
    {
    }

    public static @Nullable Prediction predictImpact(Vec3d p, Vec3d v, Vec3d a, double floorY, int maxTicks)
    {
        double px = p.x(), py = p.y(), pz = p.z();
        if (py <= floorY)
        {
            return new Prediction(p, 0);
        }

        double vx = v.x(), vy = v.y(), vz = v.z();
        final double ax = a.x(), ay = a.y(), az = a.z();

        // The integration runs on plain doubles: same operations in the same order as the previous
        // per-tick "p = p.add(v); v = v.add(a)" form, so every result is bit for bit identical, but no
        // longer allocates two Vec3d per simulated tick (up to 48 KB per call at 600 ticks).
        for (int ticks = 1; ticks <= maxTicks; ticks++)
        {
            final double previousX = px, previousY = py, previousZ = pz;
            px += vx;
            py += vy;
            pz += vz;
            vx += ax;
            vy += ay;
            vz += az;
            if (py <= floorY)
            {
                final double fraction = (previousY - floorY) / (previousY - py);
                return new Prediction(new Vec3d(
                        previousX + (px - previousX) * fraction,
                        previousY + (py - previousY) * fraction,
                        previousZ + (pz - previousZ) * fraction), ticks);
            }
        }
        return null;
    }
}
