package icbm.classic.prefab.entity;

import icbm.classic.api.data.IWorldPosition;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Base entity class to be shared by most entities, 1.12 {@code icbm.classic.prefab.entity.EntityICBM} ported
 * onto 26.3 entities.
 * <p>
 * Differences to the 1.12 class:
 * <ul>
 *     <li>{@code entityInit} + {@code DataParameter} + {@code EntityDataManager} is
 *     {@link #defineSynchedData(SynchedEntityData.Builder)} with {@link SynchedEntityData#defineId},</li>
 *     <li>{@code attackEntityFrom} is {@link #hurtServer(ServerLevel, DamageSource, float)} (26.3 splits
 *     client from server damage, this class is the server side implementation),</li>
 *     <li>{@code readEntityFromNBT}/{@code writeEntityToNBT} are {@link #readAdditionalSaveData(ValueInput)} /
 *     {@link #addAdditionalSaveData(ValueOutput)} with the S1.03 {@link SaveHandler},</li>
 *     <li>{@code setDead} is {@code discard()}, {@code isEntityInvulnerable} is
 *     {@code isInvulnerableToBase} (protected on {@link Entity}, so it can only be asked of another
 *     {@code EntityICBM}),</li>
 *     <li>1.12's client only {@code setVelocity(double, double, double)} override forwarded to
 *     {@link #setMotionVector(double, double, double)}; 26.3 has no such method on {@link Entity}
 *     ({@code setDeltaMovement} replaced it), so only {@link #setMotionVector(double, double, double)} stayed.</li>
 * </ul>
 * Created by Robin on 1/24/2015.
 */
public abstract class EntityICBM extends Entity implements IWorldPosition
{
    /** Does the entity have HP to take damage. */
    protected boolean hasHealth = false;

    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(EntityICBM.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_HEALTH = SynchedEntityData.defineId(EntityICBM.class, EntityDataSerializers.FLOAT);

    public EntityICBM(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(HEALTH, 1f);
        builder.define(MAX_HEALTH, 1f);
    }

    public float getHealth()
    {
        return this.entityData.get(HEALTH);
    }

    @SuppressWarnings("unchecked")
    public <T> T setHealth(float health)
    {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, this.getMaxHealth()));
        return (T) this;
    }

    public float getMaxHealth()
    {
        return this.entityData.get(MAX_HEALTH);
    }

    @SuppressWarnings("unchecked")
    public <T> T setMaxHealth(float hp)
    {
        this.entityData.set(MAX_HEALTH, hp);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T> T initHealth(float hp)
    {
        this.setMaxHealth(hp);
        this.setHealth(hp);
        return (T) this;
    }

    /**
     * 1.12 {@code attackEntityFrom(DamageSource, float)}, 26.3 {@link Entity#hurtServer}.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        if (hasHealth && damage > 0 && canDamage(this, source))
        {
            this.setHealth(Math.max(getHealth() - damage, 0));
            if (getHealth() <= 0)
            {
                onDestroyedBy(source, damage);
            }
            return true;
        }
        return false;
    }

    /**
     * Can damage be applied at all to the entity
     *
     * @param entity - entity being attacked
     * @param source - damage source being applied
     * @return true if the entity can be damaged
     */
    public boolean canDamage(Entity entity, DamageSource source)
    {
        if (!entity.isAlive())
        {
            return false;
        }
        //1.12: the creative mode flag of the player
        if (entity instanceof Player player && player.isCreative())
        {
            return false;
        }
        //1.12: !entity.isEntityInvulnerable(source). 26.3 exposes the check as a protected member of Entity,
        //so it is only reachable for another EntityICBM (the damage target is always this entity anyway).
        return !(entity instanceof EntityICBM icbm) || !icbm.isInvulnerableToBase(source);
    }


    /**
     * Called when the entity is killed
     */
    protected void onDestroyedBy(DamageSource source, float damage)
    {
        this.destroy();
    }

    /**
     * Called when the entity expires or
     * is destroyed in some way.
     */
    protected void destroy()
    {
        this.discard(); //1.12: setDead()
    }

    /**
     * Sets the position based on the bounding box
     */
    protected void alignToBounds()
    {
        //1.12: posX = (box.minX + box.maxX) / 2, posY = box.minY + getYOffset() - height, posZ = (box.minZ + box.maxZ) / 2.
        //getYOffset() has no 26.3 counterpart on Entity (it was 0 for every ICBM entity) and height is getBbHeight.
        final AABB box = this.getBoundingBox();
        this.setPos((box.minX + box.maxX) / 2.0D, box.minY - this.getBbHeight(), (box.minZ + box.maxZ) / 2.0D);
    }

    /**
     * Gets the predicted position
     *
     * @param t - number of ticks to predicted
     * @return predicted position of the project
     */
    public Vec3 getPredictedPosition(int t)
    {
        //1.12 used posX + motionX * t, 26.3 the equivalent is the position plus the delta movement
        return new Vec3(getX() + getDeltaMovement().x * t, getY() + getDeltaMovement().y * t, getZ() + getDeltaMovement().z * t);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    private static final SaveHandler<EntityICBM> SAVE_LOGIC = new SaveHandler<EntityICBM>()
        .mainRoot()
        /* */.nodeFloat("health", EntityICBM::getHealth, (entity, value) -> entity.setHealth(value))
        /* */.nodeFloat("health_max", EntityICBM::getMaxHealth, (entity, value) -> entity.setMaxHealth(value))
        .base();

    //=========================
    //=== IWorldPosition ======
    //=========================

    @Override
    public Level world()
    {
        return this.level();
    }

    @Override
    public double x()
    {
        return getX();
    }

    @Override
    public double y()
    {
        return getY();
    }

    @Override
    public double z()
    {
        return getZ();
    }

    public Pos getVelocity()
    {
        final Vec3 motion = getDeltaMovement();
        return new Pos(motion.x, motion.y, motion.z); //TODO make wrapper object
    }

    public void setMotionVector(double xx, double yy, double zz)
    {
        //1.12 wrote motionX/motionY/motionZ
        this.setDeltaMovement(xx, yy, zz);
    }
}
