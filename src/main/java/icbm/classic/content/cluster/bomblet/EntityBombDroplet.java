package icbm.classic.content.cluster.bomblet;

import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.capability.ex.CapabilityExplosiveEntity;
import icbm.classic.lib.projectile.EntityProjectile;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Droplet entity spawned from cluster missiles or dropped as submunitions.
 */
public class EntityBombDroplet extends EntityProjectile<EntityBombDroplet>
{
    private static final EntityDataAccessor<ItemStack> DATA_RENDER_STACK =
            SynchedEntityData.defineId(EntityBombDroplet.class, EntityDataSerializers.ITEM_STACK);

    public final CapabilityExplosiveEntity explosive = new CapabilityExplosiveEntity(this);

    public EntityBombDroplet(EntityType<? extends EntityBombDroplet> type, Level level)
    {
        super(type, level);
        this.hasHealth = false;
    }

    public EntityBombDroplet(Level level)
    {
        this(ModEntities.BOMB_DROPLET, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_RENDER_STACK, ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (DATA_RENDER_STACK.equals(key) && this.level().isClientSide())
        {
            final ItemStack stack = this.entityData.get(DATA_RENDER_STACK);
            if (!stack.isEmpty())
            {
                explosive.setStack(stack);
            }
        }
    }

    public void setExplosiveStack(ItemStack stack)
    {
        this.explosive.setStack(stack);
        if (!stack.isEmpty())
        {
            this.entityData.set(DATA_RENDER_STACK, stack.copy());
        }
    }

    @Override
    protected float getImpactDamage(Entity entityHit, float velocity, HitResult hit)
    {
        return Mth.ceil(velocity * ConfigMissile.bomblet.impactDamage);
    }

    @Override
    protected DamageSource getImpactDamageSource(Entity entityHit, float velocity, HitResult hit)
    {
        return ModDamageTypes.source(level(), ModDamageTypes.BOMBLET, this, shootingEntity);
    }

    @Override
    protected void onImpact(HitResult hit)
    {
        super.onImpact(hit);
        final Vec3 loc = hit.getLocation();
        explosive.doExplosion(loc.x, loc.y, loc.z, new ActionSource(level(), position(), new EntityCause(this)));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
        {
            explosive.doExplosion(getX(), getY(), getZ(), new ActionSource(level(), position(), new EntityCause(this)));
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldCollideWith(Entity entity)
    {
        return super.shouldCollideWith(entity) && !(entity instanceof EntityBombDroplet);
    }

    @Override
    public Component getName()
    {
        final IExplosiveData data = explosive.getExplosiveData();
        if (data != null)
        {
            return Component.literal("icbm.droplet." + data.getRegistryKey());
        }
        return Component.literal("icbm.droplet.generic");
    }

    public ItemStack toStack()
    {
        return explosive.toStack();
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

    private static final SaveHandler<EntityBombDroplet> SAVE_LOGIC = new SaveHandler<EntityBombDroplet>()
            .mainRoot()
            .nodeSavable("explosive", droplet -> droplet.explosive)
            .base();
}
