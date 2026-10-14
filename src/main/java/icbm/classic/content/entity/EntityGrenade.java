package icbm.classic.content.entity;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.capability.ex.CapabilityExplosiveEntity;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Thrown grenade. Flies with a simple drag/gravity motion, ticks the fuse of its explosive through
 * {@link ICBMClassicAPI#EX_GRENADE_REGISTRY} and runs the blast when the fuse runs out.
 * <p>
 * Registered as {@code icbmclassic:grenade} ({@code ModEntities.GRENADE}), see registry-ids.md §4.
 * <p>
 * Porting notes (porting-guide §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone: 1.12 sent the explosive item stack as spawn data,
 *     26.3 syncs the same stack through {@link SynchedEntityData} ({@link #DATA_RENDER_STACK}) so the client
 *     can label/render the entity. The stack is also the payload of the
 *     {@link CapabilityExplosiveEntity} (customizations), which stays server side like in
 *     {@link EntityExplosive}.</li>
 *     <li>Forge capabilities are gone, so this entity implements {@link IExplosive} itself and forwards to
 *     {@link #explosive}, the counterpart of the 1.12 {@code EXPLOSIVE_CAPABILITY} (see
 *     {@link icbm.classic.lib.lookup.IcbmLookups}).</li>
 *     <li>{@code motionX/Y/Z} → {@link #getDeltaMovement()}/{@link #setDeltaMovement(Vec3)};
 *     {@code rotationYaw/Pitch} → {@link #getYRot()}/{@link #getXRot()} and the previous frame values
 *     {@code yRotO}/{@code xRotO}.</li>
 *     <li>{@code setLocationAndAngles} → {@link #snapTo(double, double, double, float, float)},
 *     {@code setSize(0.25F, 0.25F)} moved to {@code EntityType.Builder#sized} (see {@code ModEntities}),
 *     {@code canBeCollidedWith()}/{@code canBePushed()} → {@link #isPickable()}/{@link #isPushable()},
 *     {@code ticksExisted} → {@link #tickCount}.</li>
 *     <li>{@code handleWaterMovement()} has no 26.3 counterpart (water movement is handled inside
 *     {@code Entity#move}) and {@code preventEntitySpawning} does not exist; both were dropped.</li>
 * </ul>
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class EntityGrenade extends Entity implements IExplosive
{
    /**
     * Stack the grenade was thrown with. 1.12 sent it as the spawn data so the client could render the item;
     * it also carries the explosive (flat items, ADR 0002).
     */
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK = SynchedEntityData.defineId(EntityGrenade.class, EntityDataSerializers.ITEM_STACK);

    /** Entity that created the grenade and set it into motion */
    private LivingEntity thrower;

    /** Explosive capability, 1.12 {@code explosive} (holds the stack and the customizations) */
    public final CapabilityExplosiveEntity explosive = new CapabilityExplosiveEntity(this);

    public EntityGrenade(EntityType<EntityGrenade> type, Level level)
    {
        super(type, level);
        // 1.12 setSize(0.25F, 0.25F), which is EntityType.Builder#sized in 26.3 (see ModEntities)
    }

    /**
     * Spawn helper used by {@code ItemGrenade}, 1.12 {@code new EntityGrenade(world)}.
     */
    public EntityGrenade(Level level)
    {
        this(ModEntities.GRENADE, level);
    }

    /**
     * Sets the explosive stack
     *
     * @param stack - explosive stack
     * @return this
     */
    public EntityGrenade setItemStack(ItemStack stack)
    {
        explosive.setStack(stack);
        // 1.12 sent the stack as spawn data; 26.3 syncs it (see DATA_RENDER_STACK)
        this.entityData.set(DATA_RENDER_STACK, stack.copyWithCount(1));
        return this;
    }

    /**
     * Gets the itemStack meant to represent the render
     *
     * @return stack to render
     */
    public ItemStack renderItemStack()
    {
        // 1.12 cached explosive.toStack() to prevent bleeding memory; the synced value is a stable reference
        return this.entityData.get(DATA_RENDER_STACK);
    }

    /**
     * Sets the throwing entity
     *
     * @param thrower - entity that threw the grenade
     * @return this
     */
    public EntityGrenade setThrower(LivingEntity thrower)
    {
        this.thrower = thrower;
        return this;
    }

    public LivingEntity getThrower()
    {
        return thrower;
    }

    /**
     * Sets the aim and position based on the throwing entity
     *
     * @return this
     */
    public EntityGrenade aimFromThrower() //TODO figure out which hand threw the grenade so we can spawn over shoulder
    {
        // 1.12 setLocationAndAngles(x, y, z, yaw, pitch) -> 26.3 Entity#snapTo(x, y, z, yRot, xRot)
        this.snapTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), thrower.getYRot(), thrower.getXRot());

        //Set position
        final float horizontalOffset = 0.16F;
        final double x = this.getX() - Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * horizontalOffset;
        final double y = this.getY() - 0.10000000149011612D;
        final double z = this.getZ() - Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * horizontalOffset;
        this.setPos(x, y, z);

        return this;
    }

    /**
     * Spawns the grenade into the game world
     *
     * @return this
     */
    public EntityGrenade spawn()
    {
        level().addFreshEntity(this); // 1.12 world.spawnEntity(this)
        return this;
    }

    /**
     * Sets the motion of the grenade
     *
     * @param energy - energy to scale the motion
     * @return this
     */
    public EntityGrenade setThrowMotion(float energy)
    {
        //Set velocity
        final float powerScale = 0.4F;
        final double vx = -Mth.sin(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI) * powerScale;
        final double vz = Mth.cos(this.getYRot() / 180.0F * (float) Math.PI) * Mth.cos(this.getXRot() / 180.0F * (float) Math.PI) * powerScale;
        final double vy = -Mth.sin((this.getXRot()) / 180.0F * (float) Math.PI) * powerScale;
        this.setThrowableHeading(vx, vy, vz, 1.8f * energy, 1.0F); //TODO see what this 1.8 is and change to be 1 * energy
        return this;
    }

    @Override
    public Component getName()
    {
        // 1.12 answered the raw string "icbm.grenade.<key>"; 26.3 entity names are components (debug label)
        return Component.literal("icbm.grenade." + getExplosiveData().getRegistryKey());
    }

    /**
     * Sets the velocity of the grenade
     *
     * @param vx     - x component velocity vector
     * @param vy     - y component velocity vector
     * @param vz     - z component velocity vector
     * @param scale  - amount to scale the vector by
     * @param random - amount to randomize the vector
     */
    public void setThrowableHeading(double vx, double vy, double vz, float scale, float random)
    {
        //normalize
        float power = Mth.sqrt((float) (vx * vx + vy * vy + vz * vz));
        vx /= power;
        vy /= power;
        vz /= power;

        //Randomize
        vx += this.random.nextGaussian() * 0.007499999832361937D * random;
        vy += this.random.nextGaussian() * 0.007499999832361937D * random;
        vz += this.random.nextGaussian() * 0.007499999832361937D * random;

        //Scale
        vx *= scale;
        vy *= scale;
        vz *= scale;

        //Apply
        setVelocity(vx, vy, vz);
    }

    /**
     * Sets the velocity to the args. Args: x, y, z
     * <p>
     * 1.12 overrode {@code Entity#setVelocity} to also orient the grenade when it had no previous rotation;
     * 26.3 builds the delta movement directly, so the same logic is kept here.
     */
    private void setVelocity(double vx, double vy, double vz)
    {
        this.setDeltaMovement(vx, vy, vz);

        if (this.xRotO == 0.0F && this.yRotO == 0.0F)
        {
            float horizontal = Mth.sqrt((float) (vx * vx + vz * vz));
            this.setYRot((float) (Mth.atan2(vx, vz) * 180.0D / Math.PI));
            this.yRotO = this.getYRot();
            this.setXRot((float) (Mth.atan2(vy, horizontal) * 180.0D / Math.PI));
            this.xRotO = this.getXRot();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // 1.12 entityInit() was empty; 26.3 requires the synced data to be declared here
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
    }

    /** Called to update the entity's position/logic. */
    @Override
    public void tick()
    {
        // 1.12 copied posX/Y/Z into prevPosX/Y/Z at the top of onUpdate; 26.3 runs Entity#commonTick before
        // this method, which copies the position into xo/yo/zo and the rotation into yRotO/xRotO
        super.tick();

        this.move(MoverType.SELF, this.getDeltaMovement());

        Vec3 motion = this.getDeltaMovement();
        final float horizontalMag = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        final float yaw = (float) (Mth.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        final float pitch = (float) (Mth.atan2(motion.y, horizontalMag) * 180.0D / Math.PI);

        // 1.12 wrapped the rotation into the same half turn as the previous frame before interpolating
        while (pitch - this.xRotO < -180.0F)
        {
            this.xRotO -= 360.0F;
        }
        while (pitch - this.xRotO >= 180.0F)
        {
            this.xRotO += 360.0F;
        }
        while (yaw - this.yRotO < -180.0F)
        {
            this.yRotO -= 360.0F;
        }
        while (yaw - this.yRotO >= 180.0F)
        {
            this.yRotO += 360.0F;
        }

        this.setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
        this.setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);

        float motionModifier = 0.98F;
        final float gravity = 0.03F;

        if (this.isInWater())
        {
            for (int i = 0; i < 4; ++i)
            {
                final float scale = 0.25F;
                // 1.12 world.spawnParticle(WATER_BUBBLE, ...); only the client level spawns anything
                this.level().addParticle(ParticleTypes.BUBBLE,
                        this.getX() - motion.x * scale,
                        this.getY() - motion.y * scale,
                        this.getZ() - motion.z * scale,
                        motion.x, motion.y, motion.z);
            }

            motionModifier = 0.8F;
        }

        motion = motion.scale(motionModifier);

        if (this.onGround())
        {
            motion = motion.multiply(0.5D, 0.5D, 0.5D);
        }
        else
        {
            motion = motion.subtract(0.0D, gravity, 0.0D);
        }

        this.setDeltaMovement(motion);

        tickFuse();
    }

    /** Ticks the fuse */
    protected void tickFuse()
    {
        if (this.tickCount > ICBMClassicAPI.EX_GRENADE_REGISTRY.getFuseTime(this, getExplosiveData()))
        {
            triggerExplosion();
        }
        else
        {
            ICBMClassicAPI.EX_GRENADE_REGISTRY.tickFuse(this, getExplosiveData(), this.tickCount);
            final Vec3 motion = this.getDeltaMovement();
            this.level().addParticle(ParticleTypes.SMOKE,
                    this.getX() - motion.x, this.getY() - motion.y + 0.4, this.getZ() - motion.z,
                    motion.x, motion.y, motion.z);
        }
    }

    /** Triggers the explosion of the grenade */
    protected void triggerExplosion()
    {
        // TODO(S4.01) build the cause from EntityCause(this) once content/missile/logic/source/cause is ported,
        //  same as EntityExplosive#explode
        explosive.doExplosion(this.getX(), this.getY() + 0.3f, this.getZ(),
                new ActionSource(level(), new Vec3(this.getX(), this.getY(), this.getZ()), null));

        this.discard(); // 1.12 setDead()
    }

    /**
     * 1.12 {@code canBeCollidedWith()} returned true.
     * <p>
     * 26.3 {@link Entity#isPickable()} answers false by default, so the value is spelled out here.
     */
    @Override
    public boolean isPickable()
    {
        return true;
    }

    /**
     * 1.12 {@code canBePushed()} returned true.
     * <p>
     * 26.3 {@link Entity#isPushable()} answers false by default, so the value is spelled out here.
     */
    @Override
    public boolean isPushable()
    {
        return true;
    }

    /**
     * 1.12 {@code attackEntityFrom}: a grenade has no health and ignores damage. 26.3 requires the override
     * as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    /**
     * 1.12 {@code readEntityFromNBT}: the explosive capability (item stack + customizations).
     */
    @Override
    protected void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    /**
     * 1.12 {@code writeEntityToNBT}: the explosive capability (item stack + customizations).
     */
    @Override
    protected void addAdditionalSaveData(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    /**
     * 1.12 stored the capability under {@code NBTConstants.EXPLOSIVE}; the port keeps the key and delegates to
     * {@link CapabilityExplosiveEntity#save(ValueOutput)}, which names its own item stack node for the 26.3
     * item stack codec (same nesting as {@link EntityExplosive}).
     */
    private static final SaveHandler<EntityGrenade> SAVE_LOGIC = new SaveHandler<EntityGrenade>()
        .mainRoot()
        /* */.nodeSavable(NBTConstants.EXPLOSIVE, EntityGrenade::getExplosiveCap)
        .base();

    /** The explosive capability of this entity, 1.12 {@code explosive}. */
    public CapabilityExplosiveEntity getExplosiveCap()
    {
        return explosive;
    }

    /**
     * Explosive this grenade runs, resolved from the synced stack so it answers the same on both sides.
     * 1.12 resolved it off the explosive capability ({@code explosive.getExplosiveData()}).
     */
    @NonNull
    @Override
    public IExplosiveData getExplosiveData()
    {
        final IExplosive data = ICBMClassicHelpers.getExplosive(renderItemStack());
        return data != null ? data.getExplosiveData() : ICBMExplosives.CONDENSED;
    }

    @Override
    public void applyCustomizations(IBlast blast)
    {
        explosive.applyCustomizations(blast);
    }

    @Override
    public void addCustomization(IExplosiveCustomization customization)
    {
        explosive.addCustomization(customization);
    }

    @Nullable
    @Override
    public ItemStack toStack()
    {
        return explosive.toStack();
    }

    @Override
    public void onDefuse()
    {
        explosive.onDefuse();
    }
}
