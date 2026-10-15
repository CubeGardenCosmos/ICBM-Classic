package icbm.classic.content.abm;

import icbm.classic.api.radio.IRadioTargetAssignable;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EntityAntiBallisticMissile extends EntityMissile<EntityAntiBallisticMissile> implements IRadioTargetAssignable
{
    private ItemStack renderStackCache;
    private UUID targetId;
    private AbmGuidance.AbmState abmState = AbmGuidance.AbmState.BOOST;
    private int ticksInState;
    private int fuelLeft = ConfigAbm.FUEL_TICKS;
    private Vec3 targetVelocityLastTick = Vec3.ZERO;
    private Vec3 previousPositionThisTick = Vec3.ZERO;
    public Vec3 getPreviousPositionThisTick() { return previousPositionThisTick; }
    public void selfDestruct()
    {
        setAbmState(AbmGuidance.AbmState.SELF_DESTRUCT);
        performSelfDestruct();
    }

    /** Detonates at the current position without block interaction and removes the ABM. */
    public void performSelfDestruct()
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            serverLevel.explode(this, getX(), getY(), getZ(), AbmGuidance.SELF_DESTRUCT_POWER, Level.ExplosionInteraction.NONE);
        }
        discard();
    }

    /** Resolves the proximity fuse roll. Overridable so deterministic tests can force a hit or a miss. */
    protected boolean rollProximityFuseHit() { return level().getRandom().nextDouble() < ConfigAbm.HIT_CHANCE; }

    /** Whether an intercepted missile detonates its payload. Overridable so deterministic tests can force either branch. */
    protected boolean shouldDetonateInterceptedWarhead() { return ConfigAbm.TARGET_WARHEAD_ON_KILL; }

    public void killInterceptedTarget(Entity target)
    {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        Vec3 point = position();
        if (!InterceptEvents.INTERCEPTED.invoker().onIntercept(serverLevel, this, target, point))
        {
            selfDestruct();
            return;
        }
        if (shouldDetonateInterceptedWarhead() && target instanceof icbm.classic.api.missiles.IMissile missile)
            MissileKillHelper.detonate(missile, missile.getMissileSource());
        else target.discard();
        level().explode(this, point.x, point.y, point.z, ConfigAbm.KILL_EFFECT_POWER, Level.ExplosionInteraction.NONE);
        level().playSound(null, point.x, point.y, point.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 0.7F);
        EngagementRegistry.markIntercepted(serverLevel, target.getUUID());
        discard();
    }

    private boolean hasTargetVelocity;

    public EntityAntiBallisticMissile(EntityType<? extends EntityAntiBallisticMissile> type, Level level)
    {
        super(type, level);
        initHealth(ConfigMissile.TIER_2_HEALTH);
        syncClient = true;
    }

    public EntityAntiBallisticMissile(Level level) { this(ModEntities.MISSILE_SAM, level); }

    @Override
    public void assignTarget(Entity target) { setTargetId(target == null ? null : target.getUUID()); }

    /**
     * Stores the target chosen before the ABM exists in the world (launcher order). Does not touch the
     * engagement registry: an ABM that was not spawned yet is not in flight and must not be counted.
     */
    public void setTargetId(UUID targetId)
    {
        this.targetId = targetId;
        this.abmState = AbmGuidance.AbmState.BOOST;
        this.ticksInState = 0;
    }

    /**
     * Moves an in-flight ABM to a new target, carrying its engagement registration along: the previous target
     * is released and {@code targetId} is registered. {@code null} only releases, because the ABM is no longer
     * engaging anything. Registration lifecycle lives here so the registry never has to rely on a target id
     * that was captured earlier by someone else.
     */
    public void retarget(UUID targetId)
    {
        if (level() instanceof ServerLevel serverLevel)
        {
            if (targetId == null) EngagementRegistry.unregister(serverLevel, getUUID());
            else EngagementRegistry.register(serverLevel, targetId, getUUID());
        }
        setTargetId(targetId);
    }

    public UUID getTargetId() { return targetId; }
    public AbmGuidance.AbmState getAbmState() { return abmState; }
    public void setAbmState(AbmGuidance.AbmState state) { abmState = state; ticksInState = 0; }
    public int getTicksInState() { return ticksInState; }
    public void incrementTicksInState() { ticksInState++; }
    public int getFuelLeft() { return fuelLeft; }
    public void decrementFuel() { fuelLeft--; }
    public Vec3 getTargetVelocityLastTick() { return targetVelocityLastTick; }
    public boolean hasTargetVelocity() { return hasTargetVelocity; }
    public void setTargetVelocityLastTick(Vec3 velocity) { targetVelocityLastTick = velocity; hasTargetVelocity = true; }

    @Override
    public void remove(Entity.RemovalReason reason)
    {
        // Keyed by ABM, never by the target it happens to hold at removal (perf-audit C2)
        if (level() instanceof ServerLevel serverLevel)
            EngagementRegistry.unregister(serverLevel, getUUID());
        super.remove(reason);
    }
    @Override
    public void tick()
    {
        if (!level().isClientSide())
        {
            AbmGuidance.tick(this);
            if (isRemoved()) return;
        }
        previousPositionThisTick = position();
        super.tick();
        if (!level().isClientSide() && !isRemoved() && getAbmState() == AbmGuidance.AbmState.GUIDE)
        {
            Entity target = getTargetId() == null ? null : level().getEntity(getTargetId());
            if (target != null && target.isAlive()) ProximityFuse.check(this, target);
        }
    }

    @Override
    protected int calculateSmokeColor() { return 0xFF8080; }

    /** EntityProjectile.tick owns raytracing and movement, but legacy launch flight logic is not ABM guidance. */
    @Override
    protected void updateMotion()
    {
        if (getMissileCapability().getFlightLogic() != null) getMissileCapability().setFlightLogic(null);
        super.updateMotion();
    }

    /** Preserve the speed commanded by guidance instead of applying generic drag/gravity. */
    @Override
    protected void decreaseMotion() { }

    /**
     * Interception is resolved by the proximity fuse (ABM design §8.2). Letting the projectile impact path
     * run would destroy the ABM on contact with its target - and only if the entity happens to be traced
     * before the fuse check in the same level tick - leaving the target alive. Ignore entity impacts so the
     * closest-approach fuse always decides the engagement; terrain impacts keep the inherited behaviour.
     */
    @Override
    protected boolean ignoreImpact(net.minecraft.world.phys.HitResult hit)
    {
        return hit instanceof net.minecraft.world.phys.EntityHitResult || super.ignoreImpact(hit);
    }

    public void updateGuidanceRotation(Vec3 velocity)
    {
        if (velocity.horizontalDistanceSqr() > 1.0e-12)
            setYRot((float)(Math.atan2(velocity.z, velocity.x) * (180.0 / Math.PI)) - 90.0F);
        setXRot((float)(-Math.atan2(velocity.y, velocity.horizontalDistance()) * (180.0 / Math.PI)));
    }

    @Override
    public ItemStack toStack()
    {
        if (level().isClientSide())
        {
            if (renderStackCache == null) renderStackCache = new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
            return renderStackCache;
        }
        return new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out)
    {
        super.addAdditionalSaveData(out);
        out.putString("abm_state", abmState.name());
        out.putInt("ticks_in_state", ticksInState);
        out.putInt("fuel_left", fuelLeft);
        if (targetId != null) out.store("target_id", UUIDUtil.CODEC, targetId);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in)
    {
        super.readAdditionalSaveData(in);
        in.getString("abm_state").ifPresent(name -> {
            try { abmState = AbmGuidance.AbmState.valueOf(name); }
            catch (IllegalArgumentException ignored) { abmState = AbmGuidance.AbmState.BOOST; }
        });
        ticksInState = in.getIntOr("ticks_in_state", 0);
        fuelLeft = in.getIntOr("fuel_left", ConfigAbm.FUEL_TICKS);
        targetId = in.read("target_id", UUIDUtil.CODEC).orElse(null);
        if (targetId == null && abmState == AbmGuidance.AbmState.GUIDE) abmState = AbmGuidance.AbmState.REACQUIRE;
    }
}

