package icbm.classic.lib.transform.vector;

import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.jlib.data.vector.ITransform;
import com.builtbroken.jlib.data.vector.Pos3D;
import icbm.classic.ICBMClassic;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.transform.rotation.EulerAngle;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;
import java.util.Optional;

/**
 * Abstract version of Pos3D for interaction with the minecraft world
 * Created by Robin on 1/13/2015.
 */
public abstract class AbstractPos<R extends AbstractPos> extends Pos3D<R> implements Position
{
    public AbstractPos()
    {
        this(0, 0, 0);
    }

    public AbstractPos(double a)
    {
        this(a, a, a);
    }

    public AbstractPos(double x, double y, double z)
    {
        super(x, y, z);
    }

    public AbstractPos(double yaw, double pitch)
    {
        this(-Math.sin(Math.toRadians(yaw)), Math.sin(Math.toRadians(pitch)), -Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
    }

    public AbstractPos(BlockEntity tile)
    {
        this(tile.getBlockPos());
    }

    public AbstractPos(Entity entity)
    {
        this(entity.getX(), entity.getY(), entity.getZ());
    }

    public AbstractPos(IPos3D vec)
    {
        this(vec.x(), vec.y(), vec.z());
    }

    public AbstractPos(ValueInput in)
    {
        this(in.getDoubleOr(NBTConstants.X, 0), in.getDoubleOr(NBTConstants.Y, 0), in.getDoubleOr(NBTConstants.Z, 0));
    }

    public AbstractPos(ByteBuf data)
    {
        this(data.readDouble(), data.readDouble(), data.readDouble());
    }

    public AbstractPos(BlockPos par1)
    {
        this(par1.getX(), par1.getY(), par1.getZ());
    }

    public AbstractPos(Direction dir)
    {
        this(dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }

    public AbstractPos(Vec3 vec)
    {
        this(vec.x, vec.y, vec.z);
    }

    public double angle(IPos3D other)
    {
        return Math.acos((this.cross(other)).magnitude() / (new Pos(other).magnitude() * magnitude()));
    }

    public double anglePreNorm(IPos3D other)
    {
        return Math.acos(this.cross(other).magnitude());
    }

    //=========================
    //========Converters=======
    //=========================

    public Vec3 toVec3()
    {
        return new Vec3(x(), y(), z());
    }

    public Point toVector2()
    {
        return new Point(x(), z());
    }

    public Direction toDirection()
    {
        //TODO maybe add a way to convert convert any vector into a direction from origin
        for (Direction dir : Direction.values())
        {
            if (xi() == dir.getStepX() && yi() == dir.getStepY() && zi() == dir.getStepZ())
            {
                return dir;
            }
        }
        return null;
    }

    @Deprecated
    public EulerAngle toEulerAngle(IPos3D target)
    {
        return sub(target).toEulerAngle();
    }

    public EulerAngle toEulerAngle(Vec3 target)
    {
        return sub(target).toEulerAngle();
    }

    public EulerAngle toEulerAngle()
    {
        return new EulerAngle(Math.toDegrees(Math.atan2(x(), z())), Math.toDegrees(-Math.atan2(y(), Math.hypot(z(), x()))));
    }

    public IPos3D transform(ITransform transformer)
    {
        if (this instanceof IPos3D)
        {
            return transformer.transform((IPos3D) this);
        }
        return null;
    }

    /**
     * Calls {@link Math#abs(double)} on each term of the pos data
     *
     * @return abs
     */
    public R absolute()
    {
        return newPos(Math.abs(x()), Math.abs(y()), Math.abs(z()));
    }

    //=========================
    //======Math Operators=====
    //=========================

    public R add(BlockPos other)
    {
        return add(other.getX(), other.getY(), other.getZ());
    }

    public R add(Direction face)
    {
        return add(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    public R add(Vec3 vec)
    {
        return add(vec.x, vec.y, vec.z);
    }

    public R sub(Direction face)
    {
        return sub(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    public R sub(Vec3 vec)
    {
        return sub(vec.x, vec.y, vec.z);
    }

    public double distance(Vec3i vec)
    {
        return distance(vec.getX() + 0.5, vec.getY() + 0.5, vec.getZ() + 0.5);
    }

    public double distance(Vec3 vec)
    {
        return distance(vec.x, vec.y, vec.z);
    }

    public double distance(Entity entity)
    {
        return distance(entity.getX(), entity.getY(), entity.getZ());
    }

    public R multiply(Direction face)
    {
        return multiply(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    public R multiply(Vec3 vec)
    {
        return multiply(vec.x, vec.y, vec.z);
    }

    public R divide(Direction face)
    {
        return divide(face.getStepX(), face.getStepY(), face.getStepZ());
    }

    public R divide(Vec3 vec)
    {
        return divide(vec.x, vec.y, vec.z);
    }

    @Override
    public R floor()
    {
        return newPos(Math.floor(x()), Math.floor(y()), Math.floor(z()));
    }

    //=========================
    //========NBT==============
    //=========================

    public void save(ValueOutput out)
    {
        out.putDouble(NBTConstants.X, x());
        out.putDouble(NBTConstants.Y, y());
        out.putDouble(NBTConstants.Z, z());
    }

    public void saveInt(ValueOutput out)
    {
        out.putInt(NBTConstants.X, xi());
        out.putInt(NBTConstants.Y, yi());
        out.putInt(NBTConstants.Z, zi());
    }

    public ByteBuf writeByteBuf(ByteBuf data)
    {
        data.writeDouble(x());
        data.writeDouble(y());
        data.writeDouble(z());
        return data;
    }

    public HitResult rayTrace(Level world, IPos3D dir, double dist)
    {
        return rayTrace(world, new Pos(x() + dir.x() * dist, y() + dir.y() * dist, z() + dir.z() * dist));
    }


    public HitResult rayTrace(Level world, IPos3D end)
    {
        return rayTrace(world, end, false, false, false);
    }

    public HitResult rayTrace(Level world, IPos3D end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        HitResult block = rayTraceBlocks(world, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
        HitResult entity = rayTraceEntities(world, end);

        if (block == null)
        {
            return entity;
        }
        if (entity == null)
        {
            return block;
        }

        if (distance(new Pos(block.getLocation())) < distance(new Pos(entity.getLocation())))
        {
            return block;
        }

        return entity;
    }


    public BlockHitResult rayTraceBlocks(Level world, IPos3D end)
    {
        return rayTraceBlocks(world, end, false, false, false);
    }

    public BlockHitResult rayTraceBlocks(Level world, IPos3D end, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock)
    {
        //TODO 26.3 removed the "returnLastUncollidableBlock" behaviour, callers must handle a null/miss result
        return world.clip(new ClipContext(
            toVec3(),
            new Vec3(end.x(), end.y(), end.z()),
            ignoreBlockWithoutBoundingBox ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE,
            stopOnLiquid ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
            CollisionContext.empty()));
    }

    public HitResult rayTraceEntities(Level world, IPos3D end)
    {
        HitResult closestEntityMOP = null;
        double closetDistance = 0D;

        double checkDistance = distance(end);
        final Vec3 start = toVec3();
        final Vec3 endVec = new Vec3(end.x(), end.y(), end.z());
        AABB scanRegion = AABB.ofSize(start, checkDistance * 2, checkDistance * 2, checkDistance * 2);

        List<Entity> checkEntities = world.getEntitiesOfClass(Entity.class, scanRegion);

        for (Entity entity : checkEntities)
        {
            if (entity != null && entity.isPickable() && entity.getBoundingBox() != null)
            {
                float border = entity.getPickRadius();
                AABB bounds = entity.getBoundingBox().inflate(border, border, border);
                Optional<Vec3> hit = bounds.clip(start, endVec);

                if (hit.isPresent())
                {
                    if (bounds.contains(start))
                    {
                        if (0 < closetDistance || closetDistance == 0)
                        {
                            closestEntityMOP = new EntityHitResult(entity, hit.get());

                            closetDistance = 0;
                        }
                    }
                    else
                    {
                        double dist = distance(new Pos(hit.get()));

                        if (dist < closetDistance || closetDistance == 0)
                        {
                            closestEntityMOP = new EntityHitResult(entity, hit.get());

                            closetDistance = dist;
                        }
                    }
                }
            }
        }

        return closestEntityMOP;
    }

    //===================
    //===World Setters===
    //===================
    public boolean setBlock(Level world, Block block)
    {
        return setBlock(world, block.defaultBlockState());
    }

    public boolean setBlock(Level world, BlockState state)
    {
        return setBlock(world, state, 3);
    }

    public boolean setBlock(Level world, BlockState block, int notify)
    {
        if (world != null && block != null)
        {
            return world.setBlock(toBlockPos(), block, notify);
        }
        else
        {
            return false;
        }
    }

    public boolean setBlockToAir(Level world)
    {
        return world.removeBlock(toBlockPos(), false);
    }

    public BlockPos toBlockPos()
    {
        return new BlockPos(xi(), yi(), zi());
    }

    //===================
    //==World Accessors==
    //===================
    public boolean isAirBlock(Level world)
    {
        return world.isEmptyBlock(toBlockPos());
    }

    @Deprecated
    public boolean isBlockFreezable(Level world)
    {
        return false;
    }

    /**
     * Checks if the block is replaceable
     *
     * @return true if it can be replaced
     */
    public boolean isReplaceable(Level world)
    {
        BlockPos pos = toBlockPos();
        BlockState block = world.getBlockState(pos);
        return block == null || block.isAir() || block.canBeReplaced();
    }

    /**
     * Checks to see if the tile can see the sky
     *
     * @return true if it can see sky, false if not or world is null
     */
    public boolean canSeeSky(Level world)
    {
        return world.canSeeSky(toBlockPos());
    }

    public boolean isBlockEqual(Level world, Block block)
    {
        Block b = getBlock(world);
        return b != null && b == block;
    }

    public Block getBlock(BlockGetter world)
    {
        BlockState state = getBlockState(world);
        if (world != null && state != null) //TODO check if chunk is loaded
        {
            return state.getBlock();
        }
        else
        {
            return null;
        }
    }

    public BlockState getBlockState(BlockGetter world)
    {
        if (world != null) //TODO check if chunk is loaded
        {
            return world.getBlockState(toBlockPos());
        }
        else
        {
            return null;
        }
    }

    public BlockEntity getBlockEntity(BlockGetter world)
    {
        if (world != null) //TODO check if chunk is loaded
        {
            return world.getBlockEntity(toBlockPos());
        }
        return null;
    }

    public float getHardness(Level world)
    {
        BlockState state = getBlockState(world);
        if (state != null && !state.isAir())
        {
            return state.getDestroySpeed(world, toBlockPos());
        }
        else
        {
            return 0;
        }
    }

    /**
     * Gets the resistance of a block using block.getExplosionResistance method
     *
     * @param cause - entity that triggered/is the explosion
     */
    public float getResistance(Entity cause)
    {
        return getResistance(cause.level(), cause, x(), y(), z());
    }

    /**
     * Gets the resistance of a block using block.getExplosionResistance method
     *
     * @param cause - entity that triggered/is the explosion
     */
    public float getResistanceToEntity(Entity cause)
    {
        return getBlock(cause.level()).getExplosionResistance();
    }

    /**
     * Gets the resistance of a block using block.getExplosionResistance method
     *
     * @param cause - entity that triggered/is the explosion
     */
    public float getResistanceToEntity(Level world, Entity cause)
    {
        return getBlock(world).getExplosionResistance();
    }

    /**
     * Gets the resistance of a block using block.getExplosionResistance method
     *
     * @param world - world to check in
     * @param cause - entity that triggered/is the explosion
     */
    public float getResistance(Level world, Entity cause)
    {
        return getResistance(world, cause, cause.getX(), cause.getY(), cause.getZ());
    }

    /**
     * Gets the resistance of a block using block.getExplosionResistance method
     *
     * @param world - world to check in
     * @param cause - entity that triggered/is the explosion
     * @param xx    - xPos location of the explosion
     * @param yy    - xPos location of the explosion
     * @param zz    - xPos location of the explosion
     */
    public float getResistance(Level world, Entity cause, double xx, double yy, double zz)
    {
        //TODO 26.3 的爆炸抗性不再接收位置/来源（Block#getExplosionResistance() 无参）
        return getBlock(world).getExplosionResistance();
    }

    public boolean isAboveBedrock()
    {
        return y() > 0;
    }

    public boolean isInsideMap()
    {
        return isAboveBedrock() && y() < ICBMClassic.MAP_HEIGHT;
    }

    /**
     * Marks a block for update
     *
     * @param world - world to update the location in
     */
    public void markForUpdate(Level world)
    {
        BlockPos pos = toBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state != null && !state.isAir())
        {
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }

    //===================
    //==Position Accessors==
    //===================
    public double getX()
    {
        return x();
    }

    public double getY()
    {
        return y();
    }

    public double getZ()
    {
        return z();
    }
}
