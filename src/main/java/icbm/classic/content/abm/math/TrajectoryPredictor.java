package icbm.classic.content.abm.math;

import org.jspecify.annotations.Nullable;

public final class TrajectoryPredictor
{
    private TrajectoryPredictor()
    {
    }

    public static @Nullable Prediction predictImpact(Vec3d p, Vec3d v, Vec3d a, double floorY, int maxTicks)
    {
        if (p.y() <= floorY)
        {
            return new Prediction(p, 0);
        }

        for (int ticks = 1; ticks <= maxTicks; ticks++)
        {
            Vec3d previous = p;
            p = p.add(v);
            v = v.add(a);
            if (p.y() <= floorY)
            {
                double fraction = (previous.y() - floorY) / (previous.y() - p.y());
                Vec3d impact = previous.add(p.sub(previous).scale(fraction));
                return new Prediction(impact, ticks);
            }
        }
        return null;
    }
}
