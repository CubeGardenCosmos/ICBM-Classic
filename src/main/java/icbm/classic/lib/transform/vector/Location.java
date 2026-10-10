package icbm.classic.lib.transform.vector;

import com.builtbroken.jlib.data.vector.IPos3D;
import icbm.classic.api.data.IWorldPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class Location extends AbstractLocation<Location> implements IWorldPosition, IPos3D, Comparable<IWorldPosition>
{
    public static final Location NULL = new Location(null, 0, 0, 0);

    public Location(Level world, double x, double y, double z)
    {
        super(world, x, y, z);
    }

    public Location(Entity entity)
    {
        this(entity.level(), entity.getX(), entity.getY(), entity.getZ());
    }

    public Location(BlockEntity tile)
    {
        super(tile);
    }

    public Location(IWorldPosition vec)
    {
        this(vec.world(), vec.x(), vec.y(), vec.z());
    }

    public Location(Level world, IPos3D vector)
    {
        this(world, vector.x(), vector.y(), vector.z());
    }

    public Location(Level world, BlockPos pos)
    {
        this(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public Location(Level world, Vec3 vec)
    {
        this(world, vec.x, vec.y, vec.z);
    }

    public Location(Level world, HitResult target)
    {
        this(world, target.getLocation());
    }

    @Override
    public Location newPos(double x, double y, double z)
    {
        return new Location(world, x, y, z);
    }

    public void playSound(SoundEvent soundIn, SoundSource category, float volume, float pitch, boolean distanceDelay)
    {
        world().playLocalSound(x(), y(), z(), soundIn, category, volume, pitch, distanceDelay);
    }

    public boolean isSideSolid(Direction side)
    {
        BlockState state = getBlockState();
        if (state != null && !state.isAir())
        {
            return state.isFaceSturdy(world, toBlockPos(), side);
        }
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof IWorldPosition && this.world == ((IWorldPosition) o).world() && ((IWorldPosition) o).x() == x() && ((IWorldPosition) o).y() == y() && ((IWorldPosition) o).z() == z();
    }

    @Override
    public int compareTo(IWorldPosition that)
    {
        final String dimension = dimensionName();
        final String thatDimension = that.world() != null ? that.world().dimension().identifier().toString() : "";

        if (dimension.compareTo(thatDimension) < 0 || x() < that.x() || y() < that.y() || z() < that.z())
        {
            return -1;
        }

        if (dimension.compareTo(thatDimension) > 0 || x() > that.x() || y() > that.y() || z() > that.z())
        {
            return 1;
        }

        return 0;
    }

    private String dimensionName()
    {
        return world() != null ? world().dimension().identifier().toString() : "";
    }
}
