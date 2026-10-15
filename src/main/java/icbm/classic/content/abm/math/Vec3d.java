package icbm.classic.content.abm.math;

/** A minimal immutable vector for ABM calculations without game dependencies. */
public record Vec3d(double x, double y, double z)
{
    public Vec3d add(Vec3d other)
    {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    public Vec3d sub(Vec3d other)
    {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d scale(double factor)
    {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public double dot(Vec3d other)
    {
        return x * other.x + y * other.y + z * other.z;
    }

    public double length()
    {
        return Math.sqrt(lengthSqr());
    }

    public double lengthSqr()
    {
        return dot(this);
    }

    public Vec3d normalize()
    {
        double length = length();
        return length == 0 ? new Vec3d(0, 0, 0) : scale(1 / length);
    }

    public Vec3d cross(Vec3d other)
    {
        return new Vec3d(y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x);
    }
}
