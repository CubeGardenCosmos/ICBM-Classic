package icbm.classic.content.missile.entity;

import icbm.classic.api.actions.IAction;
import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.events.MissileEvent;
import icbm.classic.api.events.MissileRideEvent;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.capability.chicken.CapSpaceChicken;
import icbm.classic.lib.projectile.EntityProjectile;
import icbm.classic.lib.radar.RadarRegistry;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Optional;

/**
 * Created by Robin Seifert on 12/12/2021.
 * <p>
 * 1.12 was an {@code IEntityAdditionalSpawnData} + {@code IPacketIDReceiver} that pushed the missile
 * description (flight logic, target data, ...) to the client. 26.3 has no spawn packet payload, so only
 * the values the client actually needs are synced through {@link SynchedEntityData}
 * ({@link #DATA_RENDER_STACK} and {@link #DATA_SMOKE_COLOR}); everything else stays server side and the
 * client copy is moved by the vanilla position/velocity interpolation.
 */
public abstract class EntityMissile<E extends EntityMissile<E>> extends EntityProjectile<E> implements IMissile, IEMPReceiver
{
    /**
     * 1.12 sent the missile item (used by {@code RenderMissile}) with {@code writeSpawnData} and
     * {@code PACKET_DESC}. 26.3 syncs it as entity data instead (set from {@link #toStack()} by the server).
     */
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK = SynchedEntityData.defineId(EntityMissile.class, EntityDataSerializers.ITEM_STACK);
    /**
     * 1.12 read the engine smoke color from the flight logic on the render thread. 26.3 packs the RGB
     * (0xRRGGBB) on the server so the client renderer does not need the flight logic.
     */
    private static final EntityDataAccessor<Integer> DATA_SMOKE_COLOR = SynchedEntityData.defineId(EntityMissile.class, EntityDataSerializers.INT);

    /** Color used before a flight logic is set, matches the 1.12 white engine smoke default */
    public static final int DEFAULT_SMOKE_COLOR = 0xFFFFFF;

    // Generic shared missile data
    private final HashSet<Entity> collisionIgnoreList = new HashSet<Entity>();

    @Getter
    private final CapabilityMissile missileCapability = new CapabilityMissile(this); //TODO refactor to use interface so parts can be better customized

    @Getter
    private final IEMPReceiver empCapability = new CapabilityEmpMissile(getMissileCapability());

    /** Toggle to note the missile has impacted something and already triggered impact logic */
    protected boolean hasImpacted = false;

    protected boolean syncClient = false;

    /** Set while the missile ejects its passengers from {@link #destroy()}, so the ride event can not block it */
    private boolean ejectingPassengers = false;

    /**
     * 1.12 EntityICBM registered HEALTH/MAX_HEALTH with a 1f default and the missile subclasses raised it
     * with {@code setMaxHealth}/{@code setHealth} from {@link ConfigMissile}. The missile starts at the tier
     * 1 value here instead of the 1f default (step card S4.01 asks for the ConfigMissile tier health).
     */
    private float health = ConfigMissile.TIER_1_HEALTH;
    private float maxHealth = ConfigMissile.TIER_1_HEALTH;

    public EntityMissile(EntityType<? extends EntityMissile> type, Level level)
    {
        super(type, level);
        this.hasHealth = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
        builder.define(DATA_SMOKE_COLOR, DEFAULT_SMOKE_COLOR);
    }

    public EntityMissile<E> ignore(Entity entity)
    {
        collisionIgnoreList.add(entity);
        return this;
    }

    @Override
    public void tick() {
        super.tick();

        // 1.12 sent PACKET_DESC when setFlightLogic() flagged the missile dirty; 26.3 writes the values
        // into the synced entity data so the change is pushed by the vanilla tracker
        if(syncClient) {
            this.syncClient = false;
            if (!this.level().isClientSide()) {
                final ItemStack renderStack = toStack();
                if (renderStack != null) {
                    this.entityData.set(DATA_RENDER_STACK, renderStack);
                }
                this.entityData.set(DATA_SMOKE_COLOR, calculateSmokeColor());
            }
        }
    }

    @Override
    protected void updateMotion()
    {
        if (getMissileCapability().canRunFlightLogic())
        {
            Optional.ofNullable(getMissileCapability().getFlightLogic()).ifPresent(logic -> {
                logic.onEntityTick(this, missileCapability, ticksInAir);

                if(logic.shouldRunEngineEffects(this)) {
                    // 1.12 called ICBMClassic.proxy.spawnMissileSmoke(this, logic, ticksInAir) which only ran on
                    // the client (CommonProxy was empty). 26.3 has no proxy and entities do not tick client code,
                    // so the trail is produced by the client tick in
                    // icbm.classic.client.particle.ModParticlesClient from the synced render data
                    // (S4.07). The engine sound is played server side here (1.12
                    // ICBMSounds.MISSILE_ENGINE.play with SoundCategory.BLOCKS).
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.MISSILE_ENGINE,
                        SoundSource.BLOCKS, Math.min(1, ticksInAir / 40F),
                        (1.0F + (this.getRandom().nextFloat() - this.getRandom().nextFloat()) * 0.2F) * 0.7F);
                }
            });

            //Trigger events
            ICBMClassicAPI.EX_MISSILE_REGISTRY.triggerFlightUpdate(getMissileCapability());
        }

        super.updateMotion();
    }

    @Override
    protected void decreaseMotion()
    {
        if (getMissileCapability().getFlightLogic() == null || getMissileCapability().getFlightLogic().shouldDecreaseMotion(this))
        {
            super.decreaseMotion();
        }
    }

    @Override
    protected void handleEntityCollision(@NonNull HitResult hit, @NonNull Entity entityHit)
    {
        if(entityHit instanceof Chicken) { //TODO baby zombie for lolz?
            if(getVehicle() == null) {
                // 1.12 ICBMSounds.MEEP.play(entityHit, 2, 1, true) played at the entity with SoundCategory.NEUTRAL
                this.level().playSound(null, entityHit.getX(), entityHit.getY(), entityHit.getZ(), ModSounds.MEEP, SoundSource.NEUTRAL, 2, 1);
                entityHit.startRiding(this, true, false);
                // 1.12 queried CapSpaceChicken.INSTANCE and flagged the chicken; 26.3 stores the flag in a
                // data attachment (ModAttachments#SPACE_CHICKEN), which always exists
                CapSpaceChicken.setSpace(entityHit, true);
            }
        }
        else
        {
            onImpactEntity(entityHit, (float) getDeltaMovement().length(), hit);
        }
    }

    /**
     * 1.12 {@code setDead()}; 26.3 {@code Entity#discard()} is final, so the radar removal hangs off
     * {@link #remove(RemovalReason)} which is what discard/kill/remove all funnel through.
     */
    @Override
    public void remove(RemovalReason reason)
    {
        if (!this.level().isClientSide())
        {
            RadarRegistry.remove(this);
        }

        super.remove(reason);
    }

    /**
     * 1.12 {@code canBeCollidedWith()} returned true; 26.3 {@link Entity#isPickable()} answers false by default
     */
    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public boolean shouldAlignWithMotion() {
        //Clearing default logic to flight controllers can handle motion
        return Optional.ofNullable(missileCapability.getFlightLogic()).map((logic) -> logic.shouldAlignWithMotion(this)).orElse(false);
    }

    /**
     * 1.12 {@code processInitialInteract(player, hand)}; 26.3 answers with an
     * {@link InteractionResult} and hands in the click location (porting-guide.md §2.4).
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location)
    {
        //Handle player riding missile
        if (!this.level().isClientSide()
            && (this.getVehicle() == null || this.getVehicle() == player)
            && MissileRideEvent.Start.EVENT.invoker().onStartRide(new MissileRideEvent.Start(getMissileCapability(), player)))
        {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    /**
     * 1.12 {@code EntityMountEvent#isDismounting()} had no Fabric counterpart, so the dismount check is done
     * where the vanilla code removes the passenger. {@code Entity#stopRiding()} routes through
     * {@code vehicle.removePassenger(this)} (javap of {@code Entity#stopRiding} → {@code removeVehicle()}),
     * so this one override covers the player dismount, {@code stopRiding} and {@code ejectPassengers}.
     * <p>
     * 1.12 {@code MissileEventHandler#onEntityMount} cancelled the dismount when a listener cancelled
     * {@link MissileRideEvent.Stop}; here the dismount is refused and the passenger stays on the missile.
     */
    @Override
    protected void removePassenger(Entity passenger)
    {
        if (!this.ejectingPassengers
            && !this.level().isClientSide()
            && passenger instanceof Player player
            && !MissileRideEvent.Stop.EVENT.invoker().onStopRide(new MissileRideEvent.Stop(getMissileCapability(), player)))
        {
            return;
        }
        super.removePassenger(passenger);
    }

    /**
     * 1.12 {@code getMountedYOffset()}; the BallisticFlightLogicOld branch is dropped with the class
     * (ADR 0002, no legacy save migration).
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale)
    {
        final float height = this.getBbHeight();

        if (getMissileCapability().getFlightLogic() instanceof DeadFlightLogic)
        {
            return new Vec3(0, height / 10, 0);
        }

        return new Vec3(0, height / 2 + this.getDeltaMovement().y, 0);
    }

    @Override
    protected boolean ignoreImpact(HitResult hit)
    {
        // 1.12 posted the cancelable event and treated "canceled" as "ignore impact"
        return !MissileEvent.PreImpact.EVENT.invoker().onPreImpact(new MissileEvent.PreImpact(getMissileCapability(), this, hit));
    }

    @Override
    protected void postImpact(HitResult hit)
    {
        MissileEvent.PostImpact.EVENT.invoker().onPostImpact(new MissileEvent.PostImpact(getMissileCapability(), this, hit));
    }

    @Override
    protected boolean shouldCollideWith(Entity entityHit)
    {
        // 1.12 answered null from getCollisionBox(entity) for ignored entities, 26.3 filters candidates
        // through shouldCollideWith instead
        return super.shouldCollideWith(entityHit) && !collisionIgnoreList.contains(entityHit) && !isRider(entityHit) && entityHit != shootingEntity;
    }

    /**
     * Called while the missile is being removed to allow the passengers to be ejected. The stop ride event
     * must not trap a player on a missile that is about to be removed.
     */
    @Override
    protected void destroy()
    {
        this.ejectingPassengers = true;
        try
        {
            super.destroy();
        }
        finally
        {
            this.ejectingPassengers = false;
        }
    }

    public boolean isRider(Entity entity) {
        return entity != null && (entity.getVehicle() == this || isRider(entity.getVehicle()));
    }

    public boolean hasPlayerRiding() {
        return hasPlayerRiding(this);
    }

    public static boolean hasPlayerRiding(Entity entity) {
        return entity.getPassengers().stream().anyMatch(e -> e instanceof Player || hasPlayerRiding(e));
    }

    @Override
    protected final void onImpact(@NonNull HitResult impactLocation) {
        if(!hasImpacted) {
            this.hasImpacted = true; //TODO store impact information and move this to projectile
            logImpact(impactLocation.getLocation());
            actionOnImpact(impactLocation);
        }
    }

    protected void actionOnImpact(HitResult impactLocation) {
        this.destroy();
    }

    protected void logImpact(Vec3 impactLocation)
    {
        // TODO make optional via config
        // TODO log to ICBM file separated from main config
        // TODO offer hook for database logging
        final String formatString = "Missile[%s] (%sx, %sy, %sz, %sd) impacted at (%s, %s, %s)";
        final String formattedMessage = String.format(formatString,
            this.getId(),
            Mth.floor(this.getX()),
            Mth.floor(this.getY()),
            Mth.floor(this.getZ()),
            this.level().dimension().identifier(),
            impactLocation.x,
            impactLocation.y,
            impactLocation.z
        );
        ICBMClassic.logger().info(formattedMessage);
    }

    @Override
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit) {
        if(ConfigMissile.DAMAGE_LIMIT >= 0) {
            return Math.min(Mth.ceil(velocity * ConfigMissile.DAMAGE_SCALE), ConfigMissile.DAMAGE_LIMIT);
        }
        return Mth.ceil(velocity * ConfigMissile.DAMAGE_SCALE);
    }

    //=========================
    //=== Health ==============
    //=========================
    // 1.12 declared these on EntityICBM (S5.01). The projectile base only carries the hasHealth flag, so the
    // values the missile needs live here.

    public float getHealth() {
        return this.health;
    }

    public EntityMissile<E> setHealth(float value) {
        this.health = Mth.clamp(value, 0f, getMaxHealth());
        return this;
    }

    public float getMaxHealth() {
        return this.maxHealth;
    }

    public EntityMissile<E> setMaxHealth(float value) {
        this.maxHealth = value;
        return this;
    }

    public EntityMissile<E> initHealth(float value) {
        this.setMaxHealth(value);
        this.setHealth(value);
        return this;
    }

    /**
     * 1.12 {@code EntityICBM#attackEntityFrom(DamageSource, float)}.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        if (hasHealth && damage > 0 && canDamage(source))
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
     * 1.12 {@code EntityICBM#canDamage(entity, source)}. The 1.12 creative mode branch only applied to
     * players, and the entity checked there was always this missile, so it is not needed.
     *
     * @param source applying the damage
     * @return true if the damage can be applied
     */
    protected boolean canDamage(DamageSource source)
    {
        return !isInvulnerableToBase(source) && isAlive();
    }

    /**
     * Called when the entity is killed
     */
    protected void onDestroyedBy(DamageSource source, float damage)
    {
       this.destroy();
    }

    //=========================
    //=== Client sync =========
    //=========================

    /** Missile item to render, 1.12 read this from the spawn data. */
    public ItemStack getRenderStack() {
        return this.entityData.get(DATA_RENDER_STACK);
    }

    /** Packed 0xRRGGBB engine smoke color, 1.12 asked the flight logic on the render thread. */
    public int getRenderSmokeColor() {
        return this.entityData.get(DATA_SMOKE_COLOR);
    }

    protected int calculateSmokeColor() {
        final IMissileFlightLogic logic = getMissileCapability().getFlightLogic();
        if (logic != null) {
            final int red = Mth.clamp((int) (logic.engineSmokeRed(this) * 255), 0, 255);
            final int green = Mth.clamp((int) (logic.engineSmokeGreen(this) * 255), 0, 255);
            final int blue = Mth.clamp((int) (logic.engineSmokeBlue(this) * 255), 0, 255);
            return (red << 16) | (green << 8) | blue;
        }
        return DEFAULT_SMOKE_COLOR;
    }

    //=========================
    //=== IMissile ============
    //=========================

    @Override
    public int getTicksInAir() {
        return getMissileCapability().getTicksInAir();
    }

    @Override
    public Entity getMissileEntity() {
        return getMissileCapability().getMissileEntity();
    }

    @Override
    public void setTargetData(IMissileTarget data) {
        getMissileCapability().setTargetData(data);
    }

    @Override
    public IMissileTarget getTargetData() {
        return getMissileCapability().getTargetData();
    }

    @Override
    public void setFlightLogic(IMissileFlightLogic logic) {
        getMissileCapability().setFlightLogic(logic);
    }

    @Override
    public void switchFlightLogic(IMissileFlightLogic logic) {
        getMissileCapability().switchFlightLogic(logic);
    }

    @Override
    public IMissileFlightLogic getFlightLogic() {
        return getMissileCapability().getFlightLogic();
    }

    @Override
    public void setMissileSource(IActionSource source) {
        getMissileCapability().setMissileSource(source);
    }

    @Override
    public IActionSource getMissileSource() {
        return getMissileCapability().getMissileSource();
    }

    @Override
    public void launch() {
        getMissileCapability().launch();
    }

    //=========================
    //=== IEMPReceiver ========
    //=========================

    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction) {
        return empCapability.applyEmpAction(level, x, y, z, emp_blast, power, doAction);
    }

    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, Object container, float power, boolean doAction) {
        return empCapability.applyEmpAction(level, x, y, z, emp_blast, container, power, doAction);
    }

    //=========================
    //=== Save ================
    //=========================

    @Override
    public void addAdditionalSaveData(ValueOutput out)
    {
        super.addAdditionalSaveData(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in)
    {
        super.readAdditionalSaveData(in);
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<EntityMissile> SAVE_LOGIC = new SaveHandler<EntityMissile>()
        .mainRoot()
        // 1.12 EntityICBM wrote "health"/"health_max" at the entity level
        /* */.nodeFloat("health", (missile) -> missile.health, (missile, value) -> missile.health = value)
        /* */.nodeFloat("health_max", (missile) -> missile.maxHealth, (missile, value) -> missile.maxHealth = value)
        // 1.12 wrote the IMissile capability data as the "missile" compound
        /* */.nodeSavable("missile", EntityMissile::getMissileCapability)
        .base();

    public abstract ItemStack toStack();
}
