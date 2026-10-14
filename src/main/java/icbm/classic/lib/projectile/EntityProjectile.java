package icbm.classic.lib.projectile;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.data.D3Consumer;
import icbm.classic.api.data.IWorldPosition;
import icbm.classic.api.missiles.IMissileAiming;
import icbm.classic.api.missiles.projectile.IProjectileThrowable;
import icbm.classic.content.entity.EntityPlayerSeat;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.transform.vector.Pos;
import icbm.classic.lib.world.IProjectileBlockInteraction;
import icbm.classic.lib.world.ProjectileBlockInteraction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Modified version of the arrow projectile to be more abstract
 *
 * @author Darkguardsman
 */
public abstract class EntityProjectile<PROJECTILE extends EntityProjectile<PROJECTILE>> extends Entity implements IMissileAiming, IProjectileThrowable<PROJECTILE>, IWorldPosition {

    /**
     * Effectively just render distance but scales with entity size
     */
    public static final double RENDER_DISTANCE_SCALE = 256; //TODO add config

    /**
     * 1.12 held the generic "icbmclassic:projectile" damage source here. 26.3 builds damage sources from
     * data pack damage types, so the type now lives in {@code icbmclassic:projectile}
     * ({@link icbm.classic.content.reg.ModDamageTypes#PROJECTILE}) and
     * {@link #getImpactDamageSource(Entity, float, HitResult)} resolves it per level.
     */
    public static final float DEFAULT_GRAVITY = 0.05F;

    /**
     * 26.3 requires every synced field to be declared in {@link #defineSynchedData(SynchedEntityData.Builder)}.
     * 1.12 sent {@code freezeMotion} with {@code IEntityAdditionalSpawnData#writeSpawnData}.
     */
    private static final EntityDataAccessor<Boolean> FREEZE_MOTION = SynchedEntityData.defineId(EntityProjectile.class, EntityDataSerializers.BOOLEAN);

    /**
     * The entity who shot this projectile and can be used for damage calculations
     * As well useful for causing argo on the shooter
     */
    public Entity shootingEntity;
    /**
     * Used to track shooting entity after being loaded from a save
     */
    public UUID shootingEntityUUID; //TODO abstract as a shooter object so we can track player vs entity vs tile vs admin command
    /**
     * Location the projectile was fired from, use this over the shooting entity
     * to force argo on the source of the projectile. This way things like
     * zombies don't auto go to players. Instead, they can move towards the firing
     * location. Think of it like moving towards the sound of the weapon.
     */
    public Pos sourceOfProjectile;

    //Settings
    @Setter
    @Getter
    @Accessors(chain = true)
    protected int inGroundKillTime = ICBMConstants.TICKS_MIN;
    @Setter
    @Getter
    @Accessors(chain = true)
    protected int inAirKillTime = ICBMConstants.TICKS_MIN;

    //In ground data
    @Setter(value = AccessLevel.PROTECTED)
    @Getter
    private InGroundData inGroundData;
    /**
     * Entity is being teleported, causes it to ignore block impacts
     */
    public boolean changingDimensions = false;

    //Timers
    public int ticksInGround;
    public int ticksInAir;

    // Debug
    // 1.12 had this as a plain public field synced by IEntityAdditionalSpawnData, see FREEZE_MOTION

    /**
     * 1.12 declared {@code canBeDestroyed} against {@code EntityICBM#hasHealth}. That class is ported with the
     * other prefab content (S5.01, see port-ledger), so the flag is kept here for the projectile side.
     */
    protected boolean hasHealth = false;

    public EntityProjectile(EntityType<? extends EntityProjectile> type, Level level) {
        super(type, level);
        // 1.12 called setSize(0.5F, 0.5F) here; 26.3 takes the size from EntityType.Builder#sized when the
        // content registers its entity type (registry-ids.md §4)
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Entity#defineSynchedData is abstract in 26.3, there is no base data to add
        builder.define(FREEZE_MOTION, false);
    }

    public boolean isFreezeMotion() {
        return this.entityData.get(FREEZE_MOTION);
    }

    public PROJECTILE setFreezeMotion(boolean freezeMotion) {
        this.entityData.set(FREEZE_MOTION, freezeMotion);
        return (PROJECTILE) this;
    }

    /**
     * Initialized the projectile to spawn from the shooter and aim at the target
     *
     * @param shooter    - spawn point, used for aiming and position offsets
     * @param target     - aim target
     * @param multiplier - power multiplier, mostly changes speed
     * @param random     - random multiplier
     * @return this
     */
    @Deprecated
    public PROJECTILE init(LivingEntity shooter, LivingEntity target, float multiplier, float random) {
        this.shootingEntity = shooter;
        this.sourceOfProjectile = new Pos(shooter);

        final double posY = shooter.getEyeY() - 0.10000000149011612D;
        double deltaX = target.getX() - shooter.getX();
        double deltaY = target.getBoundingBox().minY + (double) (target.getBbHeight() / 3.0F) - posY; //TODO why div3
        double deltaZ = target.getZ() - shooter.getZ();
        double deltaMag = Mth.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));

        if (deltaMag >= 1.0E-7D) //TODO why the small num? rounding likely but maybe a better solution
        {
            float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
            float pitch = (float) (-(Math.atan2(deltaY, deltaMag) * 180.0D / Math.PI));

            double subX = deltaX / deltaMag;
            double subZ = deltaZ / deltaMag;
            float subY = (float) deltaMag * 0.2F; //TODO why the 0.2F

            this.snapTo(shooter.getX() + subX, posY, shooter.getZ() + subZ, yaw, pitch);
            this.shoot(deltaX, deltaY + (double) subY, deltaZ, multiplier, random);
        }
        return (PROJECTILE) this;
    }

    @Deprecated
    public PROJECTILE init(double x, double y, double z, float yaw, float pitch, float multiplier, float distanceScale) {
        this.sourceOfProjectile = new Pos(x, y, z);
        this.snapTo(x, y, z, yaw, pitch);

        //TODO figure out why we are updating position by rotation after spawning
        final double posX = x - (double) (Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F * distanceScale);
        final double posY = y - 0.10000000149011612D; //TODO magic number - likely half arrow height
        final double posZ = z - (double) (Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F * distanceScale);
        this.setPos(posX, posY, posZ);

        final double motionX = (double) (-Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI));
        final double motionZ = (double) (Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI));
        final double motionY = (double) (-Mth.sin(pitch / 180.0F * (float) Math.PI));
        this.shoot(motionX, motionY, motionZ, multiplier, 0);

        return (PROJECTILE) this;
    }

    @Override
    public void initAimingPosition(double x, double y, double z, float yaw, float pitch, float offsetMultiplier, float forceMultiplier) {
        init(x, y, z, yaw, pitch, forceMultiplier, offsetMultiplier);
    }

    @Override
    public void initAimingPosition(Entity shooter, float offsetMultiplier, float forceMultiplier) {
        this.shootingEntity = shooter;
        initAimingPosition(
            shooter.getX(), shooter.getY() + (double) shooter.getEyeHeight(), shooter.getZ(),
            shooter.getYRot(), shooter.getXRot(),
            offsetMultiplier, forceMultiplier
        );
    }

    @Override
    public boolean throwProjectile(@NonNull PROJECTILE entity, @Nullable IActionSource source, double x, double y, double z, float yaw, float pitch, float velocity, float random) {
        initAimingPosition(
            x, y, z,
            yaw, pitch,
            1, velocity
        );
        // TODO implement random
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        //ICBMClassic.logger().info(String.format("Projectile: - Pos(%.5f,%.5f,%.5f), VEL(%.5f,%.5f,%.5f)", posX, posY, posZ, motionX, motionY, motionZ));

        this.checkInGround();

        // Handle ground logic
        if (this.inGroundData != null) {

            // Kill projectile if in ground for a while
            ++this.ticksInGround;

            if (shouldExpire()) {
                this.destroy();
            }
        }
        // Handle in air logic
        else {

            //Kills the projectile if it moves forever into space
            ++this.ticksInAir;
            if (shouldExpire()) {
                this.destroy();
                return;
            }

            final Vec3 motion = this.getDeltaMovement();
            double rayEndVecX = motion.x;
            double rayEndVecY = motion.y;
            double rayEndVecZ = motion.z;

            // Ensure we always raytrace 1 block ahead to allow early detection of collisions
            double velocity = motion.length();

            rayEndVecX /= velocity;
            rayEndVecY /= velocity;
            rayEndVecZ /= velocity;

            rayEndVecX *= (velocity + 1);
            rayEndVecY *= (velocity + 1);
            rayEndVecZ *= (velocity + 1);

            //Do raytrace TODO move to prefab entity for reuse
            // 1.12 world.rayTraceBlocks(start, end, false, true, false) -> 26.3 ClipContext with
            // Block.COLLIDER (ignore blocks without a collision shape) and Fluid.NONE (stopOnLiquid false)
            final Vec3 rayStart = new Vec3(this.getX(), this.getY(), this.getZ());
            final Vec3 rayEnd = new Vec3(this.getX() + rayEndVecX, this.getY() + rayEndVecY, this.getZ() + rayEndVecZ);
            final BlockHitResult rayHit = this.level().clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            // 26.3 answers with a HitResult.Type.MISS instead of null when nothing was hit
            final boolean blockHit = rayHit.getType() != HitResult.Type.MISS;

            //Reset data to do entity ray trace
            Vec3 entityRayEnd = new Vec3(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            if (blockHit) {
                entityRayEnd = rayHit.getLocation();
            }

            //Handle entity collision boxes
            final EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(),
                this,
                rayStart,
                entityRayEnd,
                this.getBoundingBox().move(motion.x, motion.y, motion.z).inflate(1.0D, 1.0D, 1.0D),
                this::shouldCollideWithEntityTrace,
                0.3F); // 1.12 expanded every candidate box by 0.3F

            //If we collided with an entity, set hit to entity
            if (entityHit != null) {
                final Entity entity = entityHit.getEntity();

                //rayHit.hitVec == rayHit.entityHit.pos, So, hitbox points are calculated to prevent the entity from disappearing.
                final Vec3 hitPoint = entity.getBoundingBox().clip(new Vec3(this.getX(), this.getY(), this.getZ()), entity.position()).orElse(null);

                if (!ignoreImpact(entityHit)) {
                    handleEntityCollision(hitPoint != null ? new EntityHitResult(entity, hitPoint) : entityHit, entity);
                    postImpact(entityHit);
                }
            } else if (blockHit && !ignoreImpact(rayHit)) {
                //Handle block hit
                handleBlockCollision(rayHit, velocity);
                postImpact(rayHit);
            }
            updateMotion();
        }
    }

    /**
     * Filter used by the 26.3 entity ray trace, mirrors the 1.12 loop body.
     */
    private boolean shouldCollideWithEntityTrace(@NonNull Entity entity) {
        return shouldCollideWith(entity) && (entity != this.shootingEntity || this.ticksInAir >= 5); //TODO why 5 ticks specifically? Why not 'has collider left shooter'
    }

    /**
     * Runs each tick to check what block we are inside. Raytrace this in air movement
     * but once in ground we need to refresh manually to detect block breaking.
     *
     * If block isn't the same or has changed logic will revalidate. If block has no
     * collision then things will fall
     */
    protected void checkInGround() {
        //Check if in ground TODO do we need to run this every tick?
        final BlockPos tilePos = inGroundData != null ? inGroundData.getPos() : this.blockPosition();
        final BlockState state = this.level().getBlockState(tilePos);
        final InGroundData prevInGround = this.inGroundData;

        // Only run logic if we don't have an existing collision or detected block has changed TODO use events to trigger this on block edits
        if (inGroundData == null || state != inGroundData.getState()) {
            this.inGroundData = null;

            if (!state.isAir()) {
                //Check if what we hit can be collided with
                // 1.12 state.getCollisionBoundingBox(world, pos) answered null without a bounding box
                final VoxelShape shape = state.getCollisionShape(this.level(), tilePos);
                if (!shape.isEmpty() && shape.bounds().move(tilePos).contains(new Vec3(this.getX(), this.getY(), this.getZ()))) {
                    final Direction side = Direction.UP; //TODO calculate side based on position
                    this.inGroundData = new InGroundData(tilePos, side, state);
                }
            }
        }

        // We are no longer in the ground
        if (prevInGround != null && this.inGroundData == null) {
            //TODO change to apply gravity instead of random
            final Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(
                motion.x * (double) (this.getRandom().nextFloat() * 0.2F),
                motion.y * (double) (this.getRandom().nextFloat() * 0.2F),
                motion.z * (double) (this.getRandom().nextFloat() * 0.2F));
            this.ticksInGround = 0;
            this.ticksInAir = 0;
        }
    }

    /**
     * Called to check if the projectile
     * should invoke {@link #destroy()}
     *
     * @return true to expire
     */
    protected boolean shouldExpire() {
        return ticksInAir >= inAirKillTime || ticksInGround >= inGroundKillTime || this.getY() < -100;
    }


    protected void destroy() {
        stopRiding();
        ejectPassengers();
        discard();
    }

    protected boolean ignoreImpact(HitResult hit) {
        return false;
    }

    protected void postImpact(HitResult hit) {
    }

    /**
     * Called to see if collision checks should be ignored on the
     * entity.
     *
     * @param entity
     * @return true to collide, false to ignore
     */
    protected boolean shouldCollideWith(Entity entity) {
        //TODO add listener support
        return entity.isPickable() && !(entity instanceof EntityPlayerSeat);
    }

    protected void handleBlockCollision(BlockHitResult hit, double velocity) {
        this.inGroundData = new InGroundData(this.level(), hit);

        // Special handling for ender gateways TODO move to a registry of Block -> lambda
        final IProjectileBlockInteraction.EnumHitReactions reaction = specialHandleBlock(hit, velocity);
        if (reaction.stop) {
            return;
        }

        // Move entity to collision location
        moveTowards(hit.getLocation(), this.getBbWidth() / 2f);

        //TODO this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));

        if (!this.inGroundData.getState().isAir()) {
            // 1.12 called Block#onEntityCollidedWithBlock, 26.3 routes block/entity touch handling through
            // BlockState#entityInside with an effect applier (NOOP keeps the 1.12 "no effects" behaviour)
            this.inGroundData.getState().entityInside(this.level(), this.inGroundData.getPos(), this, InsideBlockEffectApplier.NOOP, false);
        }

        if (!changingDimensions && !isRemoved() && this.portalProcess == null && reaction != IProjectileBlockInteraction.EnumHitReactions.CONTINUE_NO_IMPACT) {
            onImpactTile(hit);
            this.setDeltaMovement(0, 0, 0);
        }
    }

    protected IProjectileBlockInteraction.EnumHitReactions specialHandleBlock(BlockHitResult hit, double velocity) {
        return ProjectileBlockInteraction.handleSpecialInteraction(this.level(), this.inGroundData.getPos(), hit.getLocation(), this.inGroundData.getSide(), this.inGroundData.getState(), this);
    }

    public void moveTowards(Vec3 hit, double offset) {

        final double deltaX = hit.x - this.getX();
        final double deltaY = hit.y - this.getY();
        final double deltaZ = hit.z - this.getZ();

        final float mag = Mth.sqrt((float) (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ));
        // 1.12 used deltaX for vector Y here, kept as is to stay behaviour equivalent
        final double vecX = (deltaX / (double) mag) * offset;
        final double vecY = (deltaX / (double) mag) * offset;
        final double vecZ = (deltaZ / (double) mag) * offset;

        this.setPos(hit.x + vecX, hit.y + vecY, hit.z + vecZ);
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {

        this.changingDimensions = true;
        return super.teleport(transition);
    }

    @Override
    public int getPortalCooldown() {
        return 10;
    }
    // 1.12 also overrode getMaxInPortalTime() and canTriggerWalking(); 26.3 has no equivalent hook for either

    /**
     * Called when the projectile impacts a tile.
     * Data about the hit is stored in the entity
     * include location, block, and meta
     *
     * @param hit - exact hit position
     */
    protected void onImpactTile(BlockHitResult hit) {
        onImpact(hit);
    }

    /**
     * Handles entity being impacted by the projectile
     *
     * @param hit
     * @param entityHit
     */
    protected void handleEntityCollision(@NonNull HitResult hit, @NonNull Entity entityHit) {
        onImpactEntity(entityHit, (float) this.getDeltaMovement().length(), hit);
    }

    /**
     * Called when the projectile impacts an entity. The location of the projectile
     * may not be in the same location as the impacted entity. As impact is calculated
     * by the raytrace with no update on position provided. Meaning impact is often
     * position + velocity.
     *
     * @param entityHit by the projectile
     * @param velocity  during the impact
     * @param hit       trace used to calculate the impact, use this for projectile position
     */

    protected void onImpactEntity(@NonNull Entity entityHit, float velocity, @NonNull HitResult hit) {
        if (this.level() instanceof ServerLevel serverLevel) {
            final float damage = getImpactDamage(entityHit, velocity, hit);
            final DamageSource damageSource = getImpactDamageSource(entityHit, velocity, hit);

            if (damageSource != null && entityHit.hurtServer(serverLevel, damageSource, damage)
                && entityHit instanceof LivingEntity) {
                applyKnockBack(entityHit);
            }
            onImpact(hit);
            // TODO add deflection for some projectiles, so when impact entities the projectile might redirect rather than vanish
        }
    }

    /**
     * Called to apply knock back effects to entity during impact
     *
     * @param entity to move
     */
    protected void applyKnockBack(Entity entity) {
        final double vertKnock = 0.6000000238418579D;
        final double hortKnock = 0.1;

        // TODO rework to use projectile motionY so knockback is in motion direction, add random offset to mimic deflection
        // TODO see if we can drop sqrt, 1 projectile it is fine... 1000s it is slow
        final Vec3 motion = this.getDeltaMovement();
        final float vel_horizontal = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        if (vel_horizontal > 0.0F) {
            // 1.12 Entity#addVelocity -> 26.3 Entity#push
            entity.push(
                motion.x * vertKnock / (double) vel_horizontal,
                hortKnock,
                motion.z * vertKnock / (double) vel_horizontal
            );
        }
    }

    /**
     * Called to get damage to apply during impact
     * <p>
     * Inputs can be used to dynamically change the damage source based on angle and velocity. An example
     * is using 20% damage for side hits and 100% damage for direct impacts. As well reducing damage
     * done to specific entities. Such as 5% damage to slimes and 0% damage to ghosts.
     *
     * @param entityHit by the projectile
     * @param velocity  during the impact
     * @param hit       trace used to calculate the impact, use this for projectile position
     * @return damage to apply
     */
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit) {
        return Mth.ceil(velocity * 2f);
    }

    /**
     * Called to get damage source to apply during impact
     * <p>
     * Inputs can be used to dynamically change the damage source based on angle and velocity. An example
     * is using blunt impact for side hits and piercing for front on impacts.
     *
     * @param entityHit by the projectile
     * @param velocity  during the impact
     * @param hit       trace used to calculate the impact, use this for projectile position
     * @return damage source to use, null to skip damage
     */
    protected @Nullable DamageSource getImpactDamageSource(Entity entityHit, float velocity, HitResult hit) {
        return ModDamageTypes.source(level(), ModDamageTypes.PROJECTILE);
    }

    /**
     * Generalized impact callback, used to do cleanup
     * steps regardless of impact reason.
     * <p>
     * Use {@link #onImpactEntity(Entity, float, HitResult)} or {@link #onImpactTile(BlockHitResult)} for
     * better handling of impacts.
     */
    protected void onImpact(@NonNull HitResult hit) {
        this.destroy();
    }

    protected void updateMotion() {
        if (!isFreezeMotion() && isServer()) {
            final Vec3 motion = this.getDeltaMovement();
            final Vec3 start = this.position();

            //Update motion
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

            if (shouldAlignWithMotion()) {
                rotateTowardsMotion(0.05f);
            }

            //Decrease motion so the projectile stops
            decreaseMotion();

            //Adjust for collision      TODO check if works, rewrite code to prevent clip through of block
            // 1.12 Entity#doBlockCollisions -> 26.3 Entity#applyEffectsFromBlocks, applies the "inside block"
            // callbacks for the movement done above
            this.applyEffectsFromBlocks(start, this.position());
        }
    }

    public boolean shouldAlignWithMotion() {
        return true;
    }

    public void rotateTowardsMotion(float delta) {
        //Get rotation from motion
        final Vec3 motion = this.getDeltaMovement();
        float speed = Mth.sqrt((float) (motion.x * motion.x + motion.z * motion.z));
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI); //TODO update to catch atan for better performance on repeat calls
        float pitch = (float) (Math.atan2(motion.y, (double) speed) * 180.0D / Math.PI);

        //-------------------------------------
        //Fix rotation
        while (pitch - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while (pitch - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while (yaw - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while (yaw - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        //-------------------------------------
        //Reduce delta in rotation to provide for a smoother animation
        this.setXRot(this.xRotO + (pitch - this.xRotO) * delta);
        this.setYRot(this.yRotO + (yaw - this.yRotO) * delta);
        //-------------------------------------
    }

    /**
     * Called each tick, assuming no collisions, to slow
     * down the projectile. Both via air resistance and
     * gravity constant.
     */
    protected void decreaseMotion() {
        final float airResistance = getAirResistance();
        final Vec3 motion = this.getDeltaMovement().scale(airResistance);
        //Add gravity so the projectile will fall
        this.setDeltaMovement(motion.x, motion.y - getProjectileGravity(), motion.z);
    }

    /**
     * Multiplier to simulate air resistance. Works
     * by multiplying the resistance by the motion. So
     * if the value is 0.99 or 99% then motion will
     * decrease by 1% per tick.
     *
     * @return multiplier
     */
    protected float getAirResistance() {
        return 0.99f;
    }

    /**
     * Amount to subtract each tick to create
     * a pull towards the ground
     * <p>
     * 1.12 named this {@code getGravity()}. 26.3 made {@code Entity#getGravity()} final (it answers from the
     * entity's attributes), so the projectile gravity constant uses its own hook.
     *
     * @return gravity acceleration as m/t
     */
    protected float getProjectileGravity() {
        return DEFAULT_GRAVITY;
    }

    public void shoot(double xx, double yy, double zz, float speed, float random) {
        //Normalize
        float magnitude = Mth.sqrt((float) (xx * xx + yy * yy + zz * zz));
        xx /= (double) magnitude;
        yy /= (double) magnitude;
        zz /= (double) magnitude;

        //Add randomization to make the arrow miss
        if (random > 0) {
            xx += this.getRandom().nextGaussian() * (double) (this.getRandom().nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) random;
            yy += this.getRandom().nextGaussian() * (double) (this.getRandom().nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) random;
            zz += this.getRandom().nextGaussian() * (double) (this.getRandom().nextBoolean() ? -1 : 1) * 0.007499999832361937D * (double) random;
        }

        //Add multiplier
        xx *= (double) speed;
        yy *= (double) speed;
        zz *= (double) speed;

        //Set motion
        this.setDeltaMovement(xx, yy, zz);

        //Update rotation
        float f3 = Mth.sqrt((float) (xx * xx + zz * zz));
        final float yaw = (float) (Math.atan2(xx, zz) * 180.0D / Math.PI);
        final float pitch = (float) (Math.atan2(yy, (double) f3) * 180.0D / Math.PI);
        this.yRotO = yaw;
        this.setYRot(yaw);
        this.xRotO = pitch;
        this.setXRot(pitch);
        this.ticksInGround = 0;
    }

    protected static void vectorFromAngles(float yaw, float pitch, D3Consumer callback) {
        callback.apply(
            -Mth.sin(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F),
            -Mth.sin(pitch * 0.017453292F),
            Mth.cos(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F));
    }

    // 1.12 overrode setPositionAndRotationDirect to snap the client side copy in place. 26.3 only interpolates
    // when the entity provides a real InterpolationHandler (Entity#createInterpolationHandler), the default
    // InterpolationHandler.NO_OP already snaps, so no override is needed here.

    public boolean canBeDestroyed(Object attacker, DamageSource damageSource) {
        return hasHealth;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        final double renderDistance = getAverageEdgeLength() * Entity.getViewScale() * getRenderDistance();
        return distanceSq < renderDistance * renderDistance;
    }

    protected double getRenderDistance() {
        return RENDER_DISTANCE_SCALE;
    }

    protected double getAverageEdgeLength() {
        // 1.12 getRenderBoundingBox().getAverageEdgeLength() -> 26.3 AABB#getSize
        final AABB boundingBox = this.getBoundingBox();
        if (boundingBox == null) {
            return 1;
        }
        double size = boundingBox.getSize();
        return Double.isNaN(size) || Double.isInfinite(size) ? 1 : size;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        // Entity#addAdditionalSaveData is abstract in 26.3 and has no base implementation to call
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<EntityProjectile> SAVE_LOGIC = new SaveHandler<EntityProjectile>()
        //Stuck in ground data
        .mainRoot()
        .nodeSavable("ground", EntityProjectile::getInGroundData, EntityProjectile::setInGroundData, InGroundData::new)
        .base()
        //Ticks
        .addRoot("ticks")
        /* */.nodeInteger("air", EntityProjectile::getInAirKillTime, EntityProjectile::setInAirKillTime)
        /* */.nodeInteger("ground", EntityProjectile::getInGroundKillTime, EntityProjectile::setInGroundKillTime)
        .base();
    //Project source, if needed implement in each projectile directly. or add a boolean toggle to .addRoot. As missile doesn't need source nor shooter due to missile.targetData
        /*
        .addRoot("source")
        .nodePos("pos", (projectile) -> projectile.sourceOfProjectile, (projectile, pos) -> projectile.sourceOfProjectile = pos)
        .nodeUUID("uuid", (projectile) -> projectile.shootingEntityUUID, (projectile, uuid) -> projectile.shootingEntityUUID = uuid)
        .base();
        */

    //=========================
    //=== IWorldPosition ======
    //=========================
    // 1.12 implemented this through EntityICBM (S5.01). The projectile is used as a world position by
    // the missile code, so the four members are provided here.

    @Override
    public Level world() {
        return this.level();
    }

    @Override
    public double x() {
        return this.getX();
    }

    @Override
    public double y() {
        return this.getY();
    }

    @Override
    public double z() {
        return this.getZ();
    }
}
