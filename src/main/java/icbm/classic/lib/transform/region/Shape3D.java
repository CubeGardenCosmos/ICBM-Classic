package icbm.classic.lib.transform.region;

import com.builtbroken.jlib.data.vector.IPos3D;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.transform.rotation.EulerAngle;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Created by Robin on 1/12/2015.
 */
public abstract class Shape3D
{
    EulerAngle angle;
    Pos center;

    public Shape3D(Pos center)
    {
        this.center = center;
    }

    public Shape3D(ValueInput in)
    {
        this(new Pos(in.childOrEmpty(NBTConstants.CENTER)));
        angle = new EulerAngle(in.getDoubleOr(NBTConstants.YAW, 0), in.getDoubleOr(NBTConstants.PITCH, 0), in.getDoubleOr(NBTConstants.ROLL, 0));
    }

    /**
     * Is the vector(x, y, z) inside the shape
     */
    abstract boolean isWithin(double x, double y, double z);

    /**
     * Volume of the 3D shape
     */
    abstract double getVolume();

    /**
     * Surface area of the shape
     */
    abstract double getArea();

    /**
     * Distance the shape takes in the Xaxis
     */
    abstract double getSizeX();

    /**
     * Distance the shape takes in the Y axis
     */
    abstract double getSizeY();

    /**
     * Distance the shape takes in the Z axis
     */
    abstract double getSizeZ();

    /**
     * gets the max distance a corner of the shape will reach from the center
     */
    double getSize()
    {
        double r = getSizeX();
        if (getSizeY() > r)
            r = getSizeY();
        if (getSizeZ() > r)
            r = getSizeZ();
        return r;
    }

    /**
     * Center of the 3D shape
     */
    public IPos3D getCenter()
    {
        return center;
    }

    public double distance(IPos3D pos)
    {
        return center.distance(pos);
    }

    /**
     * Is the vector(x, y, z) inside the shape
     */
    public boolean isWithin(IPos3D vec)
    {
        return isWithin(vec.x(), vec.y(), vec.z());
    }

    public boolean isWithin(BlockPos vec)
    {
        return isWithin(vec.getX(), vec.getY(), vec.getZ());
    }
}
