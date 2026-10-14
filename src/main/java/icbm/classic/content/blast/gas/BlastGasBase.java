package icbm.classic.content.blast.gas;

import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.content.blast.Blast;
import icbm.classic.content.gas.ProtectiveArmorHandler;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.NBTConstants;
import icbm.classic.network.AirParticlePayload;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Shared logic of the gas explosives. Every {@link #TICKS_BETWEEN_RUNS} the cloud grows one ring outwards
 * through the blocks it can pass (a flood fill), spawns its particles on the new edge blocks and applies its
 * effects to the living entities inside the pathed blocks.
 * <p>
 * 1.12 -> 26.3 API mapping used below (all verified against the decompiled 26.3 sources):
 * <ul>
 *     <li>{@code world.isRemote} -> only the {@link ServerLevel} branch of the ticking/particle code runs.</li>
 *     <li>{@code world.rand} -> {@code Level#getRandom()}.</li>
 *     <li>{@code AxisAlignedBB} -> {@link AABB}, {@code getEntitiesWithinAABB(Class, AABB, Predicate)} ->
 *     {@code Level#getEntitiesOfClass(Class, AABB, Predicate)}.</li>
 *     <li>{@code entity.isEntityAlive()} -> {@link LivingEntity#isAlive()}, the 1.12 player class
 *     {@code isCreative()} -> {@link Player#isCreative()}.</li>
 *     <li>{@code EntityLivingBase} -> {@link LivingEntity}.</li>
 *     <li>{@code BlockPos.MutableBlockPos.setPos(double, double, double)} -> {@code MutableBlockPos#set(double, double, double)};
 *     {@code Vec3i.distanceSq(x, y, z)} -> the same sum of squares computed inline (26.3 {@code Vec3i} only has
 *     {@code distSqr(Vec3i)}).</li>
 *     <li>{@code blockState.getCollisionBoundingBox(world, pos)} -> {@code BlockState#getCollisionShape(BlockGetter, BlockPos)};
 *     a null/empty box (pass-through) is {@code VoxelShape#isEmpty()} and the full-span test uses
 *     {@code VoxelShape#bounds()}.</li>
 *     <li>{@code ICBMSounds.X.play(world, x, y, z, volume, pitch, true)} ->
 *     {@code Level#playSound(null, x, y, z, ModSounds.X, SoundSource.BLOCKS, volume, pitch)}; the 1.12
 *     distance delay flag only affected client side playback and has no 26.3 argument.</li>
 *     <li>Particles: the 1.12 cloud used {@code ICBMClassic.proxy.spawnAirParticle} (a client side
 *     {@code ParticleAirICBM}, a coloured, non-rising normal smoke puff with scale 7 and a life time of the
 *     cloud duration) sent through {@code PacketSpawnAirParticle}. 26.3 sends the same values through
 *     {@link AirParticlePayload} and the client spawns {@code ParticleIcbmSmoke}
 *     ({@code ModParticlesClient#spawnSmoke}, S4.07).</li>
 * </ul>
 */
public abstract class BlastGasBase extends Blast implements IBlastTickable
{
    /** Delay between running calculations */
    private static final int TICKS_BETWEEN_RUNS = 5;

    /** Mutable block pos for pathing */
    private static final BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

    /** Duration of gas effect, also controls particles */
    @Setter @Accessors(chain = true)
    protected int duration;

    @Setter @Accessors(chain = true)
    private boolean playShortSoundFX;

    private int lastRadius = 0;

    /** Blocks pathed */
    private final HashSet<BlockPos> affectedBlocks = new HashSet<>();
    /** Queue of edge blocks to path */
    private final Queue<BlockPos> edgeBlocks = new LinkedList<>();

    /** Entities impacted by gas */
    private final HashMap<LivingEntity, Integer> impactedEntityMap = new HashMap<>();
    //TODO turn into entity capability to prevent damage stacking of several explosives
    //TODO use weak refs to not hold instances

    private double sizePercentageOverTime(int timePassed)
    {
        return Math.min(1, 2f * timePassed / duration + 0.1f);
    }

    @Override
    public boolean doExplode(int callCount)
    {
        //Play start audio
        if (callCount == 0 && !this.playShortSoundFX)
        {
            //TODO look into different sounds per type
            //1.12: ICBMSounds.DEBILITATION.play(world, this.location.x(), this.location.y(), this.location.z(), 4.0F, pitch, true)
            this.world().playSound(null, this.location.x(), this.location.y(), this.location.z(), ModSounds.DEBILITATION, SoundSource.BLOCKS, 4.0F, (1.0F + (world().getRandom().nextFloat() - world().getRandom().nextFloat()) * 0.2F) * 0.7F);
        }

        //Do gas effect
        if (callCount % TICKS_BETWEEN_RUNS == 0)
        {
            setEffectBoundsAndSpawnParticles(this.callCount); // recalculate the affected blocks (where particles spawn, poison is applied, etc.)

            //Trigger effects for user feedback
            generateAudioEffect();

            //Only run potion effect application for the following types
            if (canEffectEntities())
            {
                final double radius = this.getBlastRadius();

                //Max bounds
                final AABB bounds = new AABB(
                        location.x() - radius, location.y() - radius, location.z() - radius,
                        location.x() + radius, location.y() + radius, location.z() + radius);

                //1.12: world().getEntitiesWithinAABB(EntityLivingBase.class, bounds, this::canGasEffect)
                final List<LivingEntity> entityList = world()
                        .getEntitiesOfClass(LivingEntity.class, bounds, this::canGasEffect);

                //Loop all entities
                for (LivingEntity entity : entityList)
                {
                    final float protection = getProtectionRating(entity);
                    if (protection < minGasProtection() || protection < world().getRandom().nextFloat())
                    {
                        //Track entities
                        if (!impactedEntityMap.containsKey(entity))
                        {
                            impactedEntityMap.put(entity, 1);
                        }
                        else
                        {
                            impactedEntityMap.replace(entity, impactedEntityMap.get(entity) + 1);
                        }

                        //Scale damage with hit count
                        final int hitCount = impactedEntityMap.get(entity);

                        //Apply effects
                        applyEffect(entity, hitCount);
                    }
                }
            }

            //End explosion when we hit life timer
            return this.callCount > this.duration;
        }

        return false;
    }

    protected float minGasProtection()
    {
        return 0.5f;
    }

    protected float getProtectionRating(LivingEntity entityLivingBase)
    {
        return ProtectiveArmorHandler.getProtectionRating(entityLivingBase);
    }

    /**
     * Checks if this gas explosion wants to apply effects to entities directly
     *
     * @return true to run effects
     */
    protected abstract boolean canEffectEntities();

    /**
     * Called to apply effects to entities
     *
     * @param entity   to impact
     * @param hitCount level of impact
     */
    protected void applyEffect(final LivingEntity entity, final int hitCount)
    {

    }

    /**
     * Checking that the entity can be harmed or an effect can be applied
     *
     * @param entity to check
     * @return true to effect entity
     */
    protected boolean canGasEffect(LivingEntity entity)
    {
        //Ignore dead things
        if (entity.isAlive())
        {
            //Always ignore non-gameplay characters
            if (entity instanceof Player player && player.isCreative())
            {
                return false;
            }

            //Check that the entity is in range
            return affectedBlocks.contains(checkPos.set(entity.getX(), entity.getY(), entity.getZ()));
        }
        return false;
    }

    private void generateAudioEffect()
    {
        if (this.playShortSoundFX)
        {
            //1.12: ICBMSounds.GAS_LEAK.play(world, location.x() + 0.5D, location.y() + 0.5D, location.z() + 0.5D, 4.0F, pitch, true)
            this.world().playSound(null, location.x() + 0.5D, location.y() + 0.5D, location.z() + 0.5D,
                    ModSounds.GAS_LEAK, SoundSource.BLOCKS, 4.0F,
                    1.0F + (world().getRandom().nextFloat() - world().getRandom().nextFloat()) * 0.2F);
        }
    }

    private void setEffectBoundsAndSpawnParticles(int timePassed) //TODO move to pathfinder object for reuse
    {
        final int maxSize = (int) Math.ceil(this.getBlastRadius());
        //Get and validate radius
        final int radius = (int) Math.floor(maxSize * sizePercentageOverTime(timePassed));
        if (lastRadius == radius)
        {
            return;
        }
        lastRadius = radius;

        //Get radius sq for distance checks
        final int currentDistanceSQ = radius * radius;

        //Init path data
        if (affectedBlocks.isEmpty())
        {
            affectedBlocks.add(this.location.toBlockPos()); //1.12: getPos()
            edgeBlocks.add(this.location.toBlockPos());
        }

        if (edgeBlocks.isEmpty())
        {
            affectedBlocks
                    .stream()
                    .filter((pos) -> Math.random() > 0.5)
                    .forEach(pos -> edgeBlocks.add(pos));
        }

        //Track blocks we pathed but didn't need
        final HashSet<BlockPos> hasPathed = new HashSet<>();
        //Track blocks we need to path next tick
        final Queue<BlockPos> nextSet = new LinkedList<>();

        //Loop edges from last tick
        while (edgeBlocks.peek() != null)
        {
            //Current edge block
            final BlockPos edge = edgeBlocks.poll();

            //Loop all 6 sides of the edge
            for (Direction facing : Direction.values())
            {
                //Move our check pos to current target
                checkPos.set(edge);
                checkPos.move(facing);

                //Don't repath
                if (!hasPathed.contains(checkPos) && !affectedBlocks.contains(checkPos))
                {
                    //Check that it is in range
                    if (isInRange(checkPos, currentDistanceSQ))
                    {
                        //Validate
                        if (isValidPath(checkPos, facing))
                        {
                            final BlockPos pos = checkPos.immutable();
                            affectedBlocks.add(pos);
                            nextSet.add(pos);

                            spawnGasParticles(pos);
                        }
                        //Ignore if invalid
                        else
                        {
                            hasPathed.add(checkPos.immutable());
                        }
                    }
                }
            }
        }

        //Add next set to follow up queue
        edgeBlocks.addAll(nextSet);
    }

    private boolean isValidPath(final BlockPos pos, final Direction direction)
    {
        //1.12: world.getBlockState(pos).getCollisionBoundingBox(world, pos)
        final BlockState blockState = world.getBlockState(pos);
        final VoxelShape shape = blockState.getCollisionShape(world, pos);
        if (shape.isEmpty()) // if there is no bounding box, its pass through, so its a valid path
        {
            return true;
        }

        // whether or not the respective axes are completely stretched (they span form one side of the block to another)
        final AABB aabb = shape.bounds();
        boolean xFull = aabb.minX == 0 && aabb.maxX == 1;
        boolean yFull = aabb.minY == 0 && aabb.maxY == 1;
        boolean zFull = aabb.minZ == 0 && aabb.maxZ == 1;

        boolean isImpassable = false;
        if (direction == Direction.UP || direction == Direction.DOWN)
        {
            isImpassable = xFull && zFull;
        }
        else if (direction == Direction.NORTH || direction == Direction.SOUTH)
        {
            isImpassable = xFull && yFull;
        }
        else if (direction == Direction.WEST || direction == Direction.EAST)
        {
            isImpassable = zFull && yFull;
        }

        return !isImpassable;
    }

    private boolean isInRange(final Vec3i pos, final int radiusSq)
    {
        //1.12: (int) Math.floor(pos.distanceSq(xi(), yi(), zi())) - 26.3 Vec3i has no per axis distanceSq
        final BlockPos origin = this.location.toBlockPos();
        final double dx = pos.getX() - origin.getX();
        final double dy = pos.getY() - origin.getY();
        final double dz = pos.getZ() - origin.getZ();
        return (int) Math.floor(dx * dx + dy * dy + dz * dz) <= radiusSq;
    }

    protected void spawnGasParticles(final Vec3i pos)
    {
        //1.12: ICBMClassic.proxy.spawnAirParticle(world, pos.getX(), pos.getY(), pos.getZ(), vx, vy, vz, red, green, blue, 7.0F, duration)
        if (world() instanceof ServerLevel serverLevel)
        {
            AirParticlePayload.sendToAllClients(serverLevel,
                    pos.getX(), pos.getY(), pos.getZ(),
                    (Math.random() - 0.5) / 2,
                    (Math.random() - 0.5) / 2 - 0.1,
                    (Math.random() - 0.5) / 2,
                    getParticleColorRed(pos), getParticleColorGreen(pos), getParticleColorBlue(pos),
                    7.0F, duration);
        }
    }

    protected float getParticleColorRed(final Vec3i pos)
    {
        return (float) Math.random();
    }

    protected float getParticleColorGreen(final Vec3i pos)
    {
        return (float) Math.random();
    }

    protected float getParticleColorBlue(final Vec3i pos)
    {
        return (float) Math.random();
    }

    @Override
    public void load(ValueInput in)
    {
        super.load(in);
        this.duration = in.getIntOr(NBTConstants.DURATION, 0);
        this.playShortSoundFX = in.getBooleanOr(NBTConstants.PLAY_SHORT_SOUND_FX, false);
    }

    @Override
    public void save(ValueOutput out)
    {
        super.save(out);
        out.putInt(NBTConstants.DURATION, this.duration);
        out.putBoolean(NBTConstants.PLAY_SHORT_SOUND_FX, this.playShortSoundFX);
    }
}
