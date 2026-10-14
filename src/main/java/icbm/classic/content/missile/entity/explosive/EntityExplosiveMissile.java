package icbm.classic.content.missile.entity.explosive;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.TargetRangeDet;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.capability.ex.CapabilityExplosiveEntity;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entity version of the missile carrying an explosive payload.
 */
public class EntityExplosiveMissile extends EntityMissile<EntityExplosiveMissile> implements IExplosive
{
    /** Targeting range handler and settings for triggering explosive before impact */
    public final TargetRangeDet targetRangeDet = new TargetRangeDet(this);

    /** Explosive data and settings */
    public final CapabilityExplosiveEntity explosive = new CapabilityExplosiveEntity(this);

    public EntityExplosiveMissile(EntityType<? extends EntityExplosiveMissile> type, Level level)
    {
        super(type, level);
        this.inAirKillTime = 144000; // 2 hours (1.12)
        initHealth(getMaxHealth());
    }

    @Override
    public float getMaxHealth()
    {
        final IExplosiveData data = getExplosiveData();
        if (data != null && data.getTier() != null)
        {
            switch (data.getTier())
            {
                case TWO:
                    return ConfigMissile.TIER_2_HEALTH;
                case THREE:
                    return ConfigMissile.TIER_3_HEALTH;
                case FOUR:
                    return ConfigMissile.TIER_4_HEALTH;
                default:
                    return ConfigMissile.TIER_1_HEALTH;
            }
        }
        return ConfigMissile.TIER_1_HEALTH;
    }

    @Override
    protected void onDestroyedBy(DamageSource source, float damage)
    {
        super.onDestroyedBy(source, damage);
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
        {
            explosive.doExplosion(getX(), getY(), getZ(),
                    new ActionSource(level(), new Vec3(getX(), getY(), getZ()), new EntityCause(this)));
        }
    }

    @Override
    public void tick()
    {
        targetRangeDet.update();
        super.tick();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location)
    {
        if (ICBMClassicAPI.EX_MISSILE_REGISTRY.onInteraction(this, player, hand))
        {
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    protected void actionOnImpact(HitResult impactLocation)
    {
        final Vec3 hitVec = impactLocation.getLocation();
        explosive.doExplosion(hitVec.x, hitVec.y, hitVec.z,
                new ActionSource(level(), new Vec3(getX(), getY(), getZ()), new EntityCause(this)));
        super.actionOnImpact(impactLocation);
    }

    @Override
    public ItemStack toStack()
    {
        final ItemStack stack = explosive.toStack();
        if (stack.isEmpty())
        {
            final IExplosiveData data = getExplosiveData();
            if (data != null)
            {
                final Item item = ModItems.EXPLOSIVE_MISSILES.get(data.getRegistryKey());
                if (item != null)
                {
                    return new ItemStack(item);
                }
            }
            final Item fallback = ModItems.EXPLOSIVE_MISSILES.get(ICBMExplosives.CONDENSED.getRegistryKey());
            return fallback != null ? new ItemStack(fallback) : ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public IExplosiveData getExplosiveData()
    {
        return explosive.getExplosiveData();
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

    @Override
    public void onDefuse()
    {
        explosive.onDefuse();
    }

    @Override
    public Component getName()
    {
        final IExplosiveData data = getExplosiveData();
        if (data != null)
        {
            return Component.literal("Missile[" + data.getRegistryKey() + "]");
        }
        return Component.literal("Missile[icbmclassic:generic]");
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

    private static final SaveHandler<EntityExplosiveMissile> SAVE_LOGIC = new SaveHandler<EntityExplosiveMissile>()
        .mainRoot()
        /* */.nodeSavable("explosive", (missile) -> missile.explosive)
        .base();
}
