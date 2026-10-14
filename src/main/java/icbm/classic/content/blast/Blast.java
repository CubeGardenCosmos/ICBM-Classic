package icbm.classic.content.blast;

import icbm.classic.ICBMClassic;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.events.BlastBuildEvent;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.explosion.IBlastRestore;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.entity.EntityExplosion;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.actions.WorkTickingActionHandler;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.transform.vector.Location;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Prefab for any Explosion/Blast object created
 * <p>
 * 26.3: {@link Explosion} is an interface (the vanilla implementation is {@link ServerExplosion}), so this
 * class implements it instead of extending it. The fields the 1.12 class inherited from the old
 * {@code Explosion} base class (world, x, y, z, size, exploder) are declared here now.
 */
@Deprecated
public abstract class Blast implements IBlastInit, IBlastRestore, Explosion
{
    // 1.12: inherited from the Explosion base class, which Blast no longer extends in 26.3
    /** Host level of the blast, set by {@link #setBlastWorld(Level)} */
    protected Level world;
    /** Position of the blast, set by {@link #setBlastPosition(double, double, double)} */
    protected double x, y, z;
    /** Radius/size of the blast, set by {@link #setBlastSize(double)} */
    protected float size;
    /** Entity that caused the blast, set by {@link #setBlastSource(Entity)} */
    protected Entity exploder;

    //TODO remove position as we are double storing location data
    public Location location;

    /**
     * Host of the blast
     */
    public Entity controller = null;

    /**
     * Is the blast alive, if false the blast is dead
     */
    private boolean isAlive = true;

    /**
     * The amount of times the explosion has been called
     */
    protected int callCount = 0;

    private boolean hasSetupBlast = false;

    private boolean hasBuilt = false;

    private IExplosiveData explosiveData;
    private IActionSource actionSource;

    /**
     * Only use the default if you plan to init required data
     */
    public Blast()
    {
        // 1.12: called the Explosion super constructor; 26.3 has no super constructor anymore
    }

    public Blast(Level world, double x, double y, double z) {
        this();
        setBlastWorld(world);
        setBlastPosition(x, y, z);
    }

    @Override
    @NonNull
    public IActionSource getSource() {
        return this.actionSource;
    }

    /**
     * Gets data used to create this action
     *
     * @return data
     */
    @Override
    @NonNull
    public IExplosiveData getActionData() {
        return explosiveData;
    }

    @Override
    public IBlastInit setActionSource(IActionSource source) {
        this.actionSource = source;
        return this;
    }

    @NonNull
    @Override
    public IActionStatus doAction()
    {
        try
        {
            if (this.world instanceof ServerLevel serverLevel) //1.12: !world().isRemote
            {
                // 1.12: the Forge explosion start event could cancel the blast here and return
                // ActionResponses.EXPLOSION_CANCELED. ForgeEventFactory no longer exists in the port and the
                // ICBM blasts never used the vanilla path, so the blast always proceeds.

                //Play audio to confirm explosion triggered
                playExplodeSound();

                //Start explosion
                if (this instanceof IBlastTickable)
                {
                    if (!serverLevel.addFreshEntity(new EntityExplosion(serverLevel, this)))
                    {
                        isAlive = false;
                        return ActionResponses.ENTITY_SPAWN_FAILED;
                    }
                    return ActionResponses.COMPLETED;
                }
                else
                {
                    //Do setup tasks
                    if (!this.doFirstSetup())
                    {
                        return BlastStatus.SETUP_ERROR; //TODO specify why we failed during setup
                    }

                    //Call explosive, only complete if true
                    if (this.doExplode(-1))
                    {
                        this.completeBlast();
                    }
                }
            }
            else
            {
                //1.12: world().isRemote, an unset world ends up here too instead of throwing like the legacy NPE did
                clientRunBlast();
            }
            return ActionResponses.COMPLETED;
        }
        catch (Exception e)
        {
            ICBMClassic.LOGGER.error(this + ": Unexpected error running blast", e);
            endBlast();
            return ActionResponses.UNKNOWN_ERROR; //TODO provide dynamic data
        }
    }

    //Handles client only stuff
    protected void clientRunBlast()
    {

    }

    /**
     * Called by ticking explosives
     *
     * @param ticksExisted
     * @return
     */
    public boolean onBlastTick(int ticksExisted)
    {
        if (!this.world.isClientSide()) //1.12: !world.isRemote
        {
            //Do setup work
            if (!doFirstSetup())
            {
                return false;
            }

            //Do ticks
            if (isAlive)
            {
                if (this.isCompleted() || this.doExplode(callCount++))
                {
                    completeBlast();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called to start the blast and run setup code
     *
     * @return true if the blast should continue to run, false otherwhise
     */
    @Deprecated // TODO inline with runBlast to provide proper feedback using IActionStatus
    public final boolean doFirstSetup()
    {
        if (isAlive && !hasSetupBlast)
        {
            hasSetupBlast = true;
            WorkTickingActionHandler.add(this);
            return this.setupBlast();
        }

        return true;
    }

    /**
     * @return true if the blast should continue to run, false otherwhise
     */
    protected boolean setupBlast()
    {
        return true;
    }

    /**
     * Called each tick of the blast
     *
     * @param callCount - call count or -1 to indicate this is not a ticking explosive
     * @return true to finish, false to continue/indicate the blast is doing something external like a thread
     */
    protected boolean doExplode(int callCount)
    {
        return false;
    }

    /**
     * Called to kill the blast and run last min code
     */
    public final void completeBlast()
    {
        if (isAlive)
        {
            this.endBlast();

            //Run post code
            this.onBlastCompleted();
        }
    }

    /**
     * Internal call for running post blast code
     * Do not se the entity or blast dead. This is completed
     * in the {@link #completeBlast()} method.
     */
    protected void onBlastCompleted()
    {
        clearBlast();
    }

    /**
     * Called each tick the blast has moved
     *
     * @param posX
     * @param posY
     * @param posZ
     */
    public void onPositionUpdate(double posX, double posY, double posZ)
    {
        setPosition(posX, posY, posZ);
    }

    /**
     * Called to set the position of the blast. Only call this for initialization, anything
     * that happens during world tick should call {@link #onPositionUpdate(double, double, double)}
     *
     * @param posX
     * @param posY
     * @param posZ
     */
    public Blast setPosition(double posX, double posY, double posZ)
    {
        this.x = posX;
        this.y = posY;
        this.z = posZ;
        location = new Location(world, posX, posY, posZ);
        //TODO super contains a vec3 also called position, we need to set that value instead of overriding the return
        return this;
    }

    @Override
    public Vec3 getPosition()
    {
        return this.location.toVec3();
    }

    protected void playExplodeSound()
    {
        this.world.playSound(null,
                this.x, this.y, this.z,
                SoundEvents.GENERIC_EXPLODE, //1.12: SoundEvents.ENTITY_GENERIC_EXPLODE, which is a Holder in 26.3
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (this.world.getRandom().nextFloat() - this.world.getRandom().nextFloat()) * 0.2F) * 0.7F);
    }

    /**
     * Radius of the blast
     *
     * @return
     */
    public float getBlastRadius()
    {
        return Math.max(3, this.size);
    }

    protected boolean doDamageEntities(float radius, float power)
    {
        return this.doDamageEntities(radius, power, true);
    }

    protected List<Entity> getEntities(double radius) {
        Location minCoord = location.add(-radius - 1); //TODO drop need for location
        Location maxCoord = location.add(radius + 1);
        //1.12: world().getEntitiesWithinAABB(Entity.class, AxisAlignedBB)
        return world().getEntitiesOfClass(Entity.class, new AABB(minCoord.xi(), minCoord.yi(), minCoord.zi(), maxCoord.xi(), maxCoord.yi(), maxCoord.zi()));
    }

    /**
     * @return true if the method ran successfully, false if it was interrupted
     */
    protected boolean doDamageEntities(float radius, float power, boolean destroyItem)
    {
        final List<Entity> allEntities = getEntities(radius * 2);
        return doDamageEntities(allEntities, radius, power, destroyItem);
    }

    protected boolean  doDamageEntities(List<Entity> entities, float radius, float power, boolean destroyItem) {
        // 1.12 entity damage ran on the server only. 26.3 Entity#hurtServer needs the server level, so a
        // client/degenerate call is a no-op like the legacy code was for a client world.
        if (!(this.world instanceof ServerLevel serverLevel))
        {
            return true;
        }

        final Vec3 center = new Vec3(location.x(), location.y(), location.z());
        for (Entity entity : entities) {
            if (this.onDamageEntity(entity)) {
                continue;
            }

            if (entity instanceof ItemEntity && !destroyItem) { //1.12: EntityItem
                continue;
            }

            //1.12: entity.getDistance(location.x(), location.y(), location.z())
            double distance = entity.position().distanceTo(new Vec3(location.x(), location.y(), location.z())) / radius;

            if (distance <= 1.0D) {
                double xDifference = entity.getX() - location.x();
                double yDifference = entity.getY() - location.y();
                double zDifference = entity.getZ() - location.z();

                //1.12: MathHelper.sqrt(double) is (float) Math.sqrt(value)
                double mag = (float) Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference); //TODO switch to sq for better speed

                xDifference /= mag;
                yDifference /= mag;
                zDifference /= mag;

                //1.12: world().getBlockDensity(center, entity.getEntityBoundingBox())
                double blockDensity = ServerExplosion.getSeenPercent(center, entity);
                double damageScale = (1.0D - distance) * blockDensity;
                int damage = (int) ((damageScale * damageScale + damageScale) / 2.0D * 8.0D * power + 1.0D); //TODO magic numbers

                entity.hurtServer(serverLevel, getDamageSource(), damage);

                //1.12: entity.motionX += ...
                entity.setDeltaMovement(entity.getDeltaMovement().add(xDifference * damageScale, yDifference * damageScale, zDifference * damageScale));
            }
        }

        return true;
    }

    protected DamageSource getDamageSource() {
        // 1.12: DamageSource.causeExplosionDamage(this) -> EntityDamageSource("explosion.player", explosion source entity).
        // 26.3 DamageSources#explosion(Entity direct, Entity indirect) is that same factory (DamageSources#explosion(Explosion)
        // only forwards Explosion#getDirectSourceEntity/#getIndirectSourceEntity into it), so the values are passed in directly.
        return world().damageSources().explosion(getDirectSourceEntity(), getIndirectSourceEntity());
    }

    /**
     * Called by doDamageEntity on each entity being damaged. This function should be inherited if
     * something special is to happen to a specific entity.
     *
     * @return True if something special happens to this specific entity.
     */
    protected boolean onDamageEntity(Entity entity)
    {
        return false;
    }

    @Override
    public void load(ValueInput in)
    {
        this.callCount = in.getIntOr(NBTConstants.CALL_COUNT, 0); //1.12: nbt.getInteger
        this.size = in.getFloatOr(NBTConstants.EXPLOSION_SIZE, 0); //1.12: nbt.getFloat

        //don't load the exploder if it hasn't been saved
        if (world instanceof ServerLevel serverLevel)
        {
            in.read(NBTConstants.BLAST_EXPLODER_ENT_ID, UUIDUtil.CODEC).ifPresent(uuid -> this.exploder = serverLevel.getEntity(uuid));
        }
    }

    @Override
    public void save(ValueOutput out)
    {
        out.putInt(NBTConstants.CALL_COUNT, this.callCount); //1.12: nbt.setInteger
        out.putFloat(NBTConstants.EXPLOSION_SIZE, this.size); //1.12: nbt.setFloat

        if (world instanceof ServerLevel && exploder != null) //don't save the exploder if there is none to save. TODO: do we even need to save it at all?
        {
            out.store(NBTConstants.BLAST_EXPLODER_ENT_ID, UUIDUtil.CODEC, this.exploder.getUUID());
        }
    }

    public boolean isMovable()
    {
        return false;
    }

    @Override
    public Level world()
    {
        return world;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public double x()
    {
        return this.location.x();
    }

    @Override
    public double y()
    {
        return this.location.y();
    }

    @Override
    public double z()
    {
        return this.location.z();
    }

    @Override
    public Entity getBlastSource()
    {
        // 1.12: this method called itself (infinite recursion). Kept verbatim, do not fix.
        return getBlastSource();
    }

    //================================================
    //=====          Explosion hooks            ======
    //================================================

    /**
     * {@link Explosion#level()} -> the level the blast runs in, which is the legacy {@code world} field.
     * ICBM blasts only ever run server side, so a non-server level means the blast was used outside of its
     * lifecycle (a hard error instead of a silent vanilla fallback).
     */
    @Override
    public ServerLevel level()
    {
        if (this.world instanceof ServerLevel serverLevel)
        {
            return serverLevel;
        }
        throw new IllegalStateException("Blast requires a server level to run: " + this);
    }

    /** {@link Explosion#center()} -> the legacy blast position */
    @Override
    public Vec3 center()
    {
        return getPosition();
    }

    /** {@link Explosion#radius()} -> {@link #getBlastRadius()} */
    @Override
    public float radius()
    {
        return getBlastRadius();
    }

    /** {@link Explosion#getDirectSourceEntity()} -> the legacy {@code exploder} field */
    @Override
    public Entity getDirectSourceEntity()
    {
        return this.exploder;
    }

    /** {@link Explosion#getIndirectSourceEntity()} -> {@link Explosion#getIndirectSourceEntity(Entity)} on the legacy exploder field */
    @Override
    public LivingEntity getIndirectSourceEntity()
    {
        return Explosion.getIndirectSourceEntity(this.exploder);
    }

    /** {@link Explosion#getBlockInteraction()} -> ICBM blasts handle their own block edits, vanilla never runs on them */
    @Override
    public Explosion.BlockInteraction getBlockInteraction()
    {
        return Explosion.BlockInteraction.DESTROY;
    }

    /** {@link Explosion#canTriggerBlocks()} -> ICBM blasts handle their own blocks */
    @Override
    public boolean canTriggerBlocks()
    {
        return false;
    }

    /** {@link Explosion#shouldAffectBlocklikeEntities()} -> ICBM blasts handle their own blocks */
    @Override
    public boolean shouldAffectBlocklikeEntities()
    {
        return false;
    }

    //================================================
    //=====             Properties              ======
    //================================================

    @Override
    public IBlastInit setBlastSource(Entity entity)
    {
        checkBuilt();
        this.exploder = entity;
        return this;
    }

    @Override
    public Blast setBlastSize(double power)
    {
        checkBuilt();
        this.size = (float) power;
        return this;
    }

    @Override
    public Blast scaleBlast(double scale)
    {
        checkBuilt();
        this.size *= scale;
        return this;
    }

    @Override
    public Blast setBlastWorld(Level world)
    {
        checkBuilt();
        this.world = world;
        return this;
    }

    @Override
    public Blast setBlastPosition(double posX, double posY, double posZ)
    {
        checkBuilt();
        setPosition(posX, posY, posZ);
        return this;
    }

    @Override
    public IBlastInit setExplosiveData(IExplosiveData data)
    {
        checkBuilt();
        this.explosiveData = data;
        return this;
    }

    @Override
    public IBlastInit setCustomData(@NonNull CompoundTag customData)
    {
        return this;
    }

    @Override
    public boolean isCompleted()
    {
        return !isAlive;
    }

    @Override
    public IBlastInit setEntityController(Entity entityController)
    {
        this.controller = entityController;
        return this;
    }

    @Nullable
    @Override
    public Entity getEntity()
    {
        return controller;
    }

    private final void checkBuilt()
    {
        if (hasBuilt)
        {
            throw new RuntimeException("Can not init properties of a blast after it has built");
        }
    }

    @Override
    public Blast buildBlast()
    {
        if (hasBuilt)
        {
            throw new RuntimeException("Blast has already been built");
        }
        BlastBuildEvent.EVENT.invoker().onBlastBuilt(new BlastBuildEvent(this));
        hasBuilt = true;
        return this;
    }

    @Override
    public void clearBlast()
    {
       endBlast();
    }

    protected void endBlast() {
        isAlive = false;
        WorkTickingActionHandler.remove(this);

        // 1.12 killed the blast's ThreadExplosion here; threaded blasts are rebuilt on lib/thread in S3.06
        if (controller != null)
        {
            controller.discard(); //1.12: controller.setDead()
        }
    }
}
