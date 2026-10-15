package icbm.classic.content.abm.math;

public final class Rotation
{
    private static final Vec3d UP = new Vec3d(0, 1, 0);
    private static final Vec3d X_AXIS = new Vec3d(1, 0, 0);

    private Rotation()
    {
    }

    public static Vec3d rotateTowards(Vec3d current, Vec3d desired, double maxRadians)
    {
        Vec3d a = current.normalize();
        Vec3d b = desired.normalize();
        double theta = Math.acos(clamp(a.dot(b), -1, 1));
        if (theta <= maxRadians)
        {
            // the function contract is "returns a direction": every other branch answers with a unit
            // vector too, so the caller may scale the result by a speed (AbmGuidance does exactly that).
            // Returning `desired` as-is made a non-unit desired input blow the caller's speed up.
            return b;
        }
        if (theta > Math.PI - 1e-6)
        {
            Vec3d axis = a.cross(UP);
            if (axis.length() < 1e-6)
            {
                axis = a.cross(X_AXIS);
            }
            axis = axis.normalize();
            double cos = Math.cos(maxRadians);
            double sin = Math.sin(maxRadians);
            return a.scale(cos).add(axis.cross(a).scale(sin)).add(axis.scale(axis.dot(a) * (1 - cos)));
        }

        double fraction = maxRadians / theta;
        double sinTheta = Math.sin(theta);
        double firstWeight = Math.sin((1 - fraction) * theta) / sinTheta;
        double secondWeight = Math.sin(fraction * theta) / sinTheta;
        return a.scale(firstWeight).add(b.scale(secondWeight));
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
