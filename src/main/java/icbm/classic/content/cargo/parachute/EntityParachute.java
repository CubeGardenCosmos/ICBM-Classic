package icbm.classic.content.cargo.parachute;

import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entity that acts as a slow falling mount for items/entities to use.
 */
public class EntityParachute extends EntityProjectile<EntityParachute>
{
    public static final float GRAVITY = 0.01f;
    public static final float AIR_RESISTANCE = 0.95f;
    public static final float ENTITY_SIZE = 0.5f;

    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK =
            SynchedEntityData.defineId(EntityParachute.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> DATA_RENDER_SCALE =
            SynchedEntityData.defineId(EntityParachute.class, EntityDataSerializers.FLOAT);

    @Setter @Getter @Accessors(chain = true)
    private ItemStack renderItemStack = ItemStack.EMPTY;

    @Setter @Getter @Accessors(chain = true)
    private ItemStack dropItemStack = ItemStack.EMPTY;

    /**
     * Scale the parachute model is drawn at, 1.12 {@code EntityParachute#renderScale}. It is synced so the
     * client renderer (S4.07) sees the value the server set.
     */
    public float getRenderScale()
    {
        return this.entityData.get(DATA_RENDER_SCALE);
    }

    public EntityParachute setRenderScale(float scale)
    {
        this.entityData.set(DATA_RENDER_SCALE, scale);
        return this;
    }

    public EntityParachute(EntityType<? extends EntityParachute> type, Level level)
    {
        super(type, level);
        this.renderItemStack = new ItemStack(ModItems.PARACHUTE);
        this.dropItemStack = new ItemStack(ModItems.PARACHUTE);
    }

    public EntityParachute(Level level)
    {
        this(ModEntities.PARACHUTE, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
        builder.define(DATA_RENDER_SCALE, 1.0f);
    }

    @Override
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit)
    {
        return 0;
    }
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        return false;
    }

    @Override
    public float getProjectileGravity()
    {
        return GRAVITY;
    }

    @Override
    public float getAirResistance()
    {
        return AIR_RESISTANCE;
    }

    @Override
    protected boolean shouldExpire()
    {
        return ticksInAir >= inAirKillTime || getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale)
    {
        if (passenger instanceof ItemEntity)
        {
            return new Vec3(0, -0.7, 0);
        }
        else if (passenger instanceof EntityFlyingBlock)
        {
            return new Vec3(0, -1.5, 0);
        }
        return new Vec3(0, passenger.getBbHeight() - 0.25, 0);
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
    protected void onImpact(HitResult impactLocation)
    {
        releaseParachute();
    }

    @Override
    protected void destroy()
    {
        this.releaseParachute();
    }

    protected void releaseParachute()
    {
        this.ejectPassengers();
        this.discard();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel
                && this.dropItemStack != null && !this.dropItemStack.isEmpty())
        {
            final ItemEntity entityItem = new ItemEntity(serverLevel, this.getX(), this.getY(), this.getZ(), this.dropItemStack.copy());
            entityItem.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(entityItem);
        }
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

    private static final SaveHandler<EntityParachute> SAVE_LOGIC = new SaveHandler<EntityParachute>()
            .mainRoot()
            .nodeItemStack("renderItem", EntityParachute::getRenderItemStack, EntityParachute::setRenderItemStack)
            .nodeItemStack("dropItem", EntityParachute::getDropItemStack, EntityParachute::setDropItemStack)
            .base();
}
