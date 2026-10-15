package icbm.classic.content.abm.radar;

import icbm.classic.content.abm.math.Vec3d;
import net.minecraft.world.phys.Vec3;

public final class Vectors
{
    private Vectors()
    {
    }

    public static Vec3d from(Vec3 vector)
    {
        return new Vec3d(vector.x, vector.y, vector.z);
    }

    public static Vec3 to(Vec3d vector)
    {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }
}
