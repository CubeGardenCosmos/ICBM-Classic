package icbm.classic.content.cargo.balloon;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.projectile.EntityProjectile;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entity that acts as a floating/slow falling mount for items/entities to use.
 */
public class EntityBalloon extends EntityProjectile<EntityBalloon>
{
    public static final float GRAVITY = 0.005f;
    public static final float AIR_RESISTANCE = 0.90f;
    public static final float FLOATING_GRAVITY = -0.02f;
    public static final int FLOATING_DURATION = ICBMConstants.TICKS_MIN_HALF;
    public static final float BREAK_CHANCE = 0.05f;

    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK =
            SynchedEntityData.defineId(EntityBalloon.class, EntityDataSerializers.ITEM_STACK);

    @Getter @Setter @Accessors(chain = true)
    private int liftTicks = FLOATING_DURATION;

    @Setter @Getter @Accessors(chain = true)
    private ItemStack renderItemStack = ItemStack.EMPTY;

    public EntityBalloon(EntityType<? extends EntityBalloon> type, Level level)
    {
        super(type, level);
        this.renderItemStack = new ItemStack(ModItems.BALLOON);
    }

    public EntityBalloon(Level level)
    {
        this(ModEntities.BALLOON, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide() && liftTicks <= 0 && this.random.nextFloat() <= BREAK_CHANCE)
        {
            releaseCargoAndDespawn();
        }
    }

    @Override
    protected void decreaseMotion()
    {
        super.decreaseMotion();
        if (this.liftTicks > 0)
        {
            this.liftTicks--;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    @Override
    protected float getProjectileGravity()
    {
        if (this.liftTicks > 0)
        {
            return FLOATING_GRAVITY;
        }
        return GRAVITY;
    }

    @Override
    protected float getAirResistance()
    {
        return AIR_RESISTANCE;
    }

    @Override
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit)
    {
        return 0;
    }

    @Override
    protected boolean shouldExpire()
    {
        return super.shouldExpire() || getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale)
    {
        if (passenger instanceof ItemEntity)
        {
            return new Vec3(0, -0.65, 0);
        }
        return new Vec3(0, passenger.getBbHeight() - 0.55, 0);
    }

    @Override
    protected boolean ignoreImpact(HitResult hit)
    {
        return hit.getType() == HitResult.Type.ENTITY;
    }

    @Override
    protected boolean shouldCollideWith(Entity entity)
    {
        return super.shouldCollideWith(entity) && entity != shootingEntity;
    }

    @Override
    protected void onImpact(HitResult hit)
    {
        releaseCargoAndDespawn();
    }

    @Override
    protected void destroy()
    {
        this.releaseCargoAndDespawn();
    }

    protected void releaseCargoAndDespawn()
    {
        if (!this.getPassengers().isEmpty())
        {
            this.ejectPassengers();
        }
        this.discard();
    }

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

    private static final SaveHandler<EntityBalloon> SAVE_LOGIC = new SaveHandler<EntityBalloon>()
            .mainRoot()
            .nodeInteger("lift_ticks", EntityBalloon::getLiftTicks, EntityBalloon::setLiftTicks)
            .nodeItemStack("render_stack", EntityBalloon::getRenderItemStack, EntityBalloon::setRenderItemStack)
            .base();
}
