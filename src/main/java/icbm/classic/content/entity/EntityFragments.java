package icbm.classic.content.entity;

import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.world.IProjectileBlockInteraction;
import icbm.classic.lib.world.ProjectileBlockInteraction;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Fragment thrown by shrapnel style blasts (shrapnel, anvil, fragmentation).
 * <p>
 * Registered as {@code icbmclassic:fragment} ({@code ModEntities.FRAGMENT}), see registry-ids.md §4.
 * The entity has three modes that are set by the blast before it is spawned, the 1.12
 * {@code ExplosiveInit} suppliers show all of them:
 * <ul>
 *     <li>arrow: plain flying projectile that damages what it hits,</li>
 *     <li>anvil: same flight but the impact uses the anvil hit sound (renderer draws an anvil, S4.07),</li>
 *     <li>blast: explodes on entity contact, on ground contact and when bumped while young.</li>
 * </ul>
 * Porting notes (porting-guide §2.4):
 * <ul>
 *     <li>{@code IEntityAdditionalSpawnData} is gone. 1.12 sent the two mode flags
 *     ({@code isExplosive}, {@code isAnvil}) with the spawn packet so the client renderer could pick the
 *     model, so both are {@link SynchedEntityData} values now (see {@link #DATA_EXPLOSIVE} and
 *     {@link #DATA_ANVIL}). Everything else the entity needs is either a constructor/setter value that is
 *     only read server side or already part of the vanilla entity spawn packet (position, rotation,
 *     motion).</li>
 *     <li>The 1.12 class kept its own flight code instead of extending {@code EntityProjectile}; this port
 *     copies that code (the sibling {@link icbm.classic.lib.projectile.EntityProjectile} has different
 *     hit/in-ground rules) instead of re-parenting the class, to keep the 1.12 behaviour.</li>
 *     <li>{@code setSize(0.5F, 0.5F)} moved to {@code EntityType.Builder#sized} (see {@link ModEntities}).
 *     {@code Entity#isPickable()} answers false by default in 26.3, which is the 1.12 default of
 *     {@code canBeCollidedWith()}, so no override is needed for that.</li>
 *     <li>{@code setVelocity(...)} is kept as a plain helper instead of an override of
 *     {@link Entity#setDeltaMovement(double, double, double)}: the 26.3 method is called by the engine on
 *     every push/knockback/move, so overriding it would run the 1.12 rotation snap far more often than
 *     1.12 did.</li>
 * </ul>
 *
 * @author Calclavia
 */
@Deprecated //TODO recode to use projectile system and have a unique entity per type
public class EntityFragments extends Entity
{
    /**
     * 1.12 used lombok {@code @Setter} on a plain boolean field that was sent as spawn data. Values that
     * the client has to know at spawn time live in {@link SynchedEntityData} in 26.3 (porting-guide §2.4).
     */
    private static final EntityDataAccessor<Boolean> DATA_EXPLOSIVE =
        SynchedEntityData.defineId(EntityFragments.class, EntityDataSerializers.BOOLEAN);

    /** See {@link #DATA_EXPLOSIVE}, 1.12 {@code isAnvil}. */
    private static final EntityDataAccessor<Boolean> DATA_ANVIL =
        SynchedEntityData.defineId(EntityFragments.class, EntityDataSerializers.BOOLEAN);

    /**
     * Position of the block the fragment is stuck in. 1.12 started this at {@code new BlockPos(0, 0, 0)}
     * (world origin, which is not the position of the entity) and only wrote it on a block impact, the
     * port keeps both the field and that behaviour.
     */
    private BlockPos inTilePosition = new BlockPos(0, 0, 0);

    /**
     * Block the fragment is stuck in. 1.12 {@code inTile} (block state) was null until the first block
     * impact, so the field is nullable here as well. Not saved by 1.12, so not saved here either.
     */
    @Nullable
    private BlockState inTile;

    public LivingEntity shootingEntity;

    //Type settings
    //1.12 TODO: replace both flags with an enum, kept as two flags (see DATA_EXPLOSIVE/DATA_ANVIL)

    //Triggers
    private boolean inGround = false;
    private boolean isExploding = false;

    /** Seems to be some sort of timer for animating an arrow. */
    public int arrowShake = 0;

    /** The owner of this arrow. */
    private int ticksInAir = 0;

    @Setter
    @Accessors(chain = true)
    public int damage = 11;

    public boolean flatDamage = false;

    /** Is this arrow a critical hit? (Controls particles and damage) */
    @Setter
    @Accessors(chain = true)
    public boolean arrowCritical = false;

    /** Explosion radius used when {@link #isExplosive()} is set, 1.12 {@code explosionSize} */
    public float explosionSize = 1.5F;

    public EntityFragments(EntityType<EntityFragments> type, Level level)
    {
        super(type, level);
        // 1.12 called setSize(0.5F, 0.5F) here; 26.3 takes the size from EntityType.Builder#sized when the
        // entity type is registered (ModEntities)
    }

    /** 1.12 constructor {@code EntityFragments(World)}, kept so blast content can spawn fragments. */
    public EntityFragments(Level level)
    {
        this(ModEntities.FRAGMENT, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add.
        // 1.12 entityInit() was empty as both flags travelled as spawn data instead of data manager values
        builder.define(DATA_EXPLOSIVE, false);
        builder.define(DATA_ANVIL, false);
    }

    /** 1.12 field {@code isExplosive}: the fragment explodes on impact instead of just hurting. */
    public boolean isExplosive()
    {
        return this.entityData.get(DATA_EXPLOSIVE);
    }

    /** 1.12 lombok generated setter, chained like the other fragment settings. */
    public EntityFragments setExplosive(boolean isExplosive)
    {
        this.entityData.set(DATA_EXPLOSIVE, isExplosive);
        return this;
    }

    /** 1.12 field {@code isAnvil}: the fragment behaves like an anvil (hit sound + anvil renderer). */
    public boolean isAnvil()
    {
        return this.entityData.get(DATA_ANVIL);
    }

    /** 1.12 lombok generated setter, chained like the other fragment settings. */
    public EntityFragments setAnvil(boolean isAnvil)
    {
        this.entityData.set(DATA_ANVIL, isAnvil);
        return this;
    }

    /** 1.12 returned the raw string "Fragments"; 26.3 entity names are components shown by commands. */
    @Override
    public Component getName()
    {
        return Component.translatable("entity.icbmclassic.fragment");
    }

    /**
     * Uses the provided coordinates as a heading and determines the velocity from it with the set
     * force and random variance. Args: x, y, z, force, forceVariation
     */
    public void setArrowHeading(double vecX, double vecY, double vecZ, float scale, float random)
    {
        //Normalize vector
        float mag = Mth.sqrt((float) (vecX * vecX + vecY * vecY + vecZ * vecZ));
        vecX /= mag;
        vecY /= mag;
        vecZ /= mag;

        //Add random
        vecX += this.getRandom().nextGaussian() * 0.007499999832361937D * random;
        vecY += this.getRandom().nextGaussian() * 0.007499999832361937D * random;
        vecZ += this.getRandom().nextGaussian() * 0.007499999832361937D * random;

        //Scale to power
        vecX *= scale;
        vecY *= scale;
        vecZ *= scale;

        //Set motion
        this.setDeltaMovement(vecX, vecY, vecZ);

        //Update rotation
        float flatMag = Mth.sqrt((float) (vecX * vecX + vecZ * vecZ));
        final float yaw = (float) (Math.atan2(vecX, vecZ) * 180.0D / Math.PI);
        final float pitch = (float) (Math.atan2(vecY, (double) flatMag) * 180.0D / Math.PI);
        this.yRotO = yaw;
        this.setYRot(yaw);
        this.xRotO = pitch;
        this.setXRot(pitch);
    }

    /**
     * Sets the velocity to the args. Args: x, y, z
     * <p>
     * 1.12 overrode {@code Entity#setVelocity}; 26.3 replaced that method with
     * {@link Entity#setDeltaMovement(double, double, double)} which the engine calls on every push, knock
     * back and move. Overriding it would apply the rotation snap of this method far more often than 1.12
     * did, so the 1.12 body is kept as a helper with the 1.12 name.
     */
    public void setVelocity(double motionX, double motionY, double motionZ)
    {
        this.setDeltaMovement(motionX, motionY, motionZ);

        if (this.xRotO == 0.0F && this.yRotO == 0.0F)
        {
            float flatMag = Mth.sqrt((float) (motionX * motionX + motionZ * motionZ));
            final float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
            final float pitch = (float) (Math.atan2(motionY, (double) flatMag) * 180.0D / Math.PI);
            this.yRotO = yaw;
            this.setYRot(yaw);
            this.xRotO = pitch;
            this.setXRot(pitch);
            // 1.12 Entity#setLocationAndAngles -> 26.3 Entity#snapTo keeps the same position and rotation
            this.snapTo(this.getX(), this.getY(), this.getZ(), yaw, pitch);
        }
    }

    private void explode()
    {
        // 1.12: if (!this.isExploding && !this.world.isRemote)
        if (!this.isExploding && !this.level().isClientSide())
        {
            this.isExploding = true;
            // 1.12 world.createExplosion(this, posX, posY, posZ, explosionSize, true). The trailing true
            // was "isSmoking" which is the block breaking explosion, 26.3 spells that TNT
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionSize, Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }

    /** Called to update the entity's position/logic. */
    @Override
    public void tick()
    {
        super.tick();

        if (this.xRotO == 0.0F && this.yRotO == 0.0F)
        {
            final Vec3 motion = this.getDeltaMovement();
            float flatMag = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
            final float yaw = (float) (Math.atan2(motion.x, motion.z) * (180D / Math.PI));
            final float pitch = (float) (Math.atan2(motion.y, (double) flatMag) * (180D / Math.PI));
            this.setYRot(yaw);
            this.setXRot(pitch);
            this.yRotO = yaw;
            this.xRotO = pitch;
        }

        final BlockState blockState = this.level().getBlockState(this.inTilePosition);

        // 1.12 checked the block Material, 26.3 answers air as a flag on the state
        if (!blockState.isAir())
        {
            // 1.12 used Block#getCollisionBoundingBox (null when the block has no shape); 26.3 answers an
            // empty VoxelShape instead
            final VoxelShape shape = blockState.getCollisionShape(this.level(), this.inTilePosition);

            if (!shape.isEmpty() && shape.bounds().move(this.inTilePosition).contains(this.position()))
            {
                this.inGround = true;
            }
        }

        if (this.arrowShake > 0)
        {
            --this.arrowShake;
        }

        if (this.inGround)
        {
            // 1.12 world.collidesWithAnyBlock(box) -> !level.noCollision(box)
            if (blockState != this.inTile && !this.level().noCollision(this.getBoundingBox().inflate(0.05D)))
            {
                this.inGround = false;
                final Vec3 motion = this.getDeltaMovement();
                this.setDeltaMovement(
                    motion.x * (double) (this.getRandom().nextFloat() * 0.2F),
                    motion.y * (double) (this.getRandom().nextFloat() * 0.2F),
                    motion.z * (double) (this.getRandom().nextFloat() * 0.2F));
                this.ticksInAir = 0;
            }
            else
            {
                onImpactGround();
            }
        }
        else
        {
            ++this.ticksInAir;

            //Check for block collision
            final Vec3 start = new Vec3(this.getX(), this.getY(), this.getZ());
            final Vec3 motion = this.getDeltaMovement();

            // 1.12 world.rayTraceBlocks(start, motion * 2, false, true, false): ignore blocks without a
            // collision shape (Block.COLLIDER) and do not stop on fluids (Fluid.NONE)
            final Vec3 rayEnd = new Vec3(this.getX() + motion.x * 2, this.getY() + motion.y * 2, this.getZ() + motion.z * 2);
            final BlockHitResult blockHit = this.level().clip(new ClipContext(start, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            // 26.3 answers with a HitResult.Type.MISS instead of null when nothing was hit
            final boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;

            //Reset start end
            Vec3 end = new Vec3(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

            //IF hit, change end to last block hit
            if (hitBlock)
            {
                end = blockHit.getLocation();
            }

            //Check for entities
            final Entity entity = this.findEntityOnPath(start, end);

            //Wrapper entity hit, 1.12 replaced the block hit with the entity hit
            final HitResult hitResult = entity != null ? new EntityHitResult(entity) : (hitBlock ? blockHit : null);

            //Handle entity hit. 1.12 asked Forge's projectile impact event here to let other mods cancel
            //the impact; Fabric has no server side projectile impact event, so the hook is dropped
            if (hitResult != null)
            {
                this.onHit(hitResult);
            }

            //Update position
            final Vec3 updatedMotion = this.getDeltaMovement();
            this.setPos(this.getX() + updatedMotion.x, this.getY() + updatedMotion.y, this.getZ() + updatedMotion.z);

            //Update rotation
            float flatMag = Mth.sqrt((float) (updatedMotion.x * updatedMotion.x + updatedMotion.z * updatedMotion.z));
            float yaw = (float) (Math.atan2(updatedMotion.x, updatedMotion.z) * (180D / Math.PI));
            float pitch = (float) (Math.atan2(updatedMotion.y, (double) flatMag) * (180D / Math.PI));

            // 1.12 unwound prevRotationPitch/prevRotationYaw before the lerp; 26.3 tracks the previous
            // rotation in xRotO/yRotO, the 1.12 unwinding is applied to those
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

            //Reduce delta in rotation to provide for a smoother animation
            this.setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
            this.setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);

            float motionModifier = 0.99F;

            //Handle water
            if (this.isInWater())
            {
                float motionMultiplier = 0.25F;

                for (int i = 0; i < 4; ++i)
                {
                    // 1.12 world.spawnParticle(WATER_BUBBLE, x, y, z, xSpeed, ySpeed, zSpeed), the 26.3
                    // Level#addParticle signature is the same (only the client level spawns anything)
                    this.level().addParticle(ParticleTypes.BUBBLE,
                        this.getX() - updatedMotion.x * motionMultiplier,
                        this.getY() - updatedMotion.y * motionMultiplier,
                        this.getZ() - updatedMotion.z * motionMultiplier,
                        updatedMotion.x, updatedMotion.y, updatedMotion.z);
                }

                motionModifier = 0.6F;
            }

            // 1.12 Entity#isWet() and Entity#extinguish()
            if (this.isInWaterOrRain())
            {
                this.extinguishFire();
            }

            //Air friction
            final Vec3 slowedMotion = this.getDeltaMovement().scale(motionModifier);
            double motionY = slowedMotion.y;

            //Gravity
            if (!this.isNoGravity())
            {
                motionY -= 0.05000000074505806D;
            }
            this.setDeltaMovement(slowedMotion.x, motionY, slowedMotion.z);

            this.setPos(this.getX(), this.getY(), this.getZ());

            // 1.12 Entity#doBlockCollisions -> 26.3 Entity#applyEffectsFromBlocks for the movement done
            // above (the NOOP free scan of 1.12 is gone, 26.3 routes it through the movement)
            this.applyEffectsFromBlocks(start, this.position());
        }
    }

    protected double getDamage(float speed)
    {
        if (flatDamage)
        {
            return damage;
        }
        return (double) speed * this.damage;
    }

    protected boolean canAttack(Entity entity)
    {
        return entity != null && entity.isAlive();
    }

    /**
     * Called when the arrow hits a block or an entity
     */
    protected void onHit(HitResult hitResult)
    {
        if (hitResult instanceof EntityHitResult entityHit)
        {
            final Entity entity = entityHit.getEntity();

            if (!canAttack(entity))
            {
                this.discard();
                return;
            }

            final Vec3 motion = this.getDeltaMovement();
            final float speed = Mth.sqrt((float) (motion.x * motion.x + motion.y * motion.y + motion.z * motion.z));
            final int damageScaled = Mth.ceil(getDamage(speed));

            //TODO change damage source to match fragment
            // 1.12 built new EntityDamageSourceIndirect("arrow", this, shootingEntity).setProjectile().
            // 26.3 damage sources come from data pack damage types and DamageSources#arrow requires an
            // AbstractArrow, so the vanilla arrow type is resolved from its holder with this entity as the
            // direct and the shooting entity as the causing entity (the 1.12 indirect layout). The 1.12 TODO
            // above is kept: this is not a fragment specific damage type
            final DamageSource damageSource = new DamageSource(ModDamageTypes.holder(this.level(), DamageTypes.ARROW), this, this.shootingEntity);

            //Notify that we hit an entity
            this.onImpactEntity(entity);

            if (this.isOnFire() && !(entity instanceof Enderman))
            {
                // 1.12 Entity#setFire(5) counted seconds
                entity.igniteForSeconds(5);
            }

            // 1.12 Entity#attackEntityFrom -> 26.3 Entity#hurtServer, which needs a ServerLevel. The 1.12
            // client copy never dealt the damage (the server owns the entity), so only the server attacks
            final boolean damaged = this.level() instanceof ServerLevel serverLevel
                && entity.hurtServer(serverLevel, damageSource, (float) damageScaled);

            if (damaged)
            {
                if (!(entity instanceof Enderman))
                {
                    this.discard();
                }
            }
            else
            {
                final Vec3 reflectedMotion = this.getDeltaMovement().scale(-0.10000000149011612D);
                this.setDeltaMovement(reflectedMotion);
                this.setYRot(this.getYRot() + 180.0F);
                this.yRotO += 180.0F;
                this.ticksInAir = 0;

                if (!this.level().isClientSide() && reflectedMotion.lengthSqr() < 0.0010000000474974513D)
                {
                    this.discard();
                }
            }
        }
        else if (hitResult instanceof BlockHitResult blockHit)
        {
            this.inTilePosition = blockHit.getBlockPos();
            this.inTile = this.level().getBlockState(this.inTilePosition);

            final IProjectileBlockInteraction.EnumHitReactions reaction =
                ProjectileBlockInteraction.handleSpecialInteraction(this.level(), this.inTilePosition, blockHit.getLocation(), blockHit.getDirection(), this.inTile, this);
            if (reaction.stop)
            {
                return;
            }

            // 1.12 wrote the hit offset into motion with the float casts below, kept as is
            this.setDeltaMovement(
                (float) (blockHit.getLocation().x - this.getX()),
                (float) (blockHit.getLocation().y - this.getY()),
                (float) (blockHit.getLocation().z - this.getZ()));

            final Vec3 motion = this.getDeltaMovement();
            final float f2 = Mth.sqrt((float) (motion.x * motion.x + motion.y * motion.y + motion.z * motion.z));
            this.setPos(
                this.getX() - motion.x / (double) f2 * 0.05000000074505806D,
                this.getY() - motion.y / (double) f2 * 0.05000000074505806D,
                this.getZ() - motion.z / (double) f2 * 0.05000000074505806D);

            this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.getRandom().nextFloat() * 0.2F + 0.9F));

            this.inGround = true;
            this.arrowShake = 7;

            if (!this.inTile.isAir())
            {
                // 1.12 Block#onEntityCollidedWithBlock, 26.3 routes the touch through BlockState#entityInside
                // (NOOP keeps the "no effect" behaviour of the plain 1.12 call)
                this.inTile.entityInside(this.level(), this.inTilePosition, this, InsideBlockEffectApplier.NOOP, false);
            }
        }
    }

    protected void onImpactEntity(Entity entity)
    {
        if (this.isExplosive())
        {
            explode();
        }
        else
        {
            playImpactAudio();
        }
    }

    protected void onImpactGround()
    {
        if (this.isExplosive())
        {
            explode();
        }
        else
        {
            playImpactAudio();
            this.discard();
        }
    }

    protected void playImpactAudio()
    {
        if (this.getRandom().nextFloat() > 0.5f)
        {
            if (this.isAnvil())
            {
                // 1.12 world.playSound(x, y, z, BLOCK_ANVIL_HIT, BLOCKS, volume, pitch, distanceDelay);
                // 26.3 has the same method as Level#playLocalSound
                this.level().playLocalSound(this.getX(), (int) this.getY(), (int) this.getZ(),
                    SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 1, 1, true);
            }
            else
            {
                this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.getRandom().nextFloat() * 0.2F + 0.9F));
            }
        }
    }

    @Nullable
    protected Entity findEntityOnPath(Vec3 start, Vec3 end)
    {
        // 1.12 looped over world.getEntitiesInAABBexcluding(box expanded by motion * 1.2 grow 1.0) and
        // clipped every candidate bounding box grown by 0.3, keeping the nearest hit. ProjectileUtil
        // #getEntityHitResult is exactly that loop in 26.3
        if (this.ticksInAir < 5) // 1.12 checked this per candidate, so nothing can be hit before 5 ticks
        {
            return null;
        }

        final Vec3 motion = this.getDeltaMovement();
        final EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            start,
            end,
            this.getBoundingBox().expandTowards(motion.x * 1.2, motion.y * 1.2, motion.z * 1.2).inflate(1.0D),
            this::canBeHitOnPath,
            0.30000001192092896F); // 1.12 grew every candidate box by this much

        return hit != null ? hit.getEntity() : null;
    }

    /** Filter behind {@link #findEntityOnPath}, 1.12 required a collidable entity that is not a fragment. */
    private boolean canBeHitOnPath(Entity entity)
    {
        return entity.isPickable() && !(entity instanceof EntityFragments);
    }

    /** (abstract) Protected helper method to write subclass entity data to save. */
    @Override
    public void addAdditionalSaveData(ValueOutput out)
    {
        // Entity#addAdditionalSaveData is abstract in 26.3 and has no base implementation to call
        SAVE_LOGIC.save(this, out);
    }

    /** (abstract) Protected helper method to read subclass entity data from save. */
    @Override
    public void readAdditionalSaveData(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    /**
     * 1.12 {@code writeEntityToNBT}/{@code readEntityFromNBT} wrote the shake timer as a byte and the
     * explosive mode flag, on the entity root and with the same {@link NBTConstants} keys. The other
     * fragment settings (anvil, damage, critical, explosion size) were runtime only in 1.12 and are not
     * saved here either.
     */
    private static final SaveHandler<EntityFragments> SAVE_LOGIC = new SaveHandler<EntityFragments>()
        .mainRoot()
        /* */.nodeByte(NBTConstants.SHAKE,
            fragments -> (byte) fragments.arrowShake,
            (fragments, shake) -> fragments.arrowShake = shake & 255)
        /* */.nodeBoolean(NBTConstants.IS_EXPLOSIVE, EntityFragments::isExplosive, EntityFragments::setExplosive)
        .base();

    /**
     * 1.12 inherited {@code Entity#attackEntityFrom}, which answered false for everything that is not a
     * living entity. 26.3 requires the override as {@link Entity#hurtServer} is abstract.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    /** Called by a player entity when they collide with an entity */
    @Override
    public void push(Entity entity)
    {
        // 1.12 Entity#applyEntityCollision, 26.3 resolves entity collisions through Entity#push
        super.push(entity);

        // 1.12 used ticksExisted, 26.3 counts the same ticks in tickCount
        if (this.isExplosive() && this.tickCount < 20 * 2)
        {
            this.explode();
        }
    }
}
