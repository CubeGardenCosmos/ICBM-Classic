package icbm.classic.content.missile.entity.explosive;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.actions.PotentialAction;
import icbm.classic.lib.actions.fields.ActionFieldProvider;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Missile with generic action handling.
 */
public class EntityMissileActionable extends EntityMissile<EntityMissileActionable>
{
    /** Explosive data and settings */
    @Getter
    private final PotentialAction mainAction = new PotentialAction()
        .withProvider(new ActionFieldProvider()
            .field(ActionFields.IMPACTED, () -> hasImpacted)
            .field(ActionFields.HOST_ENTITY, () -> this)
            .field(ActionFields.HOST_POSITION, this::position)
            .field(ActionFields.TARGET_POSITION, () -> this.getMissileCapability().getTargetData() != null ? this.getMissileCapability().getTargetData().getPosition() : null)
        );

    @Getter @Setter @Accessors(chain = true)
    private ItemStack originalStack = ItemStack.EMPTY;

    public EntityMissileActionable(EntityType<? extends EntityMissileActionable> type, Level level)
    {
        super(type, level);
        this.inAirKillTime = 144000; // 2 hours (1.12)
    }

    public EntityMissileActionable setActionData(IActionData actionData)
    {
        mainAction.setActionData(actionData);
        return this;
    }

    @Override
    protected void onDestroyedBy(DamageSource source, float damage)
    {
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
        {
            final IActionStatus status = this.mainAction.doAction(level(), getX(), getY(), getZ(), new EntityCause(this));
            if (!status.isType(ActionStatusTypes.BLOCKING))
            {
                super.onDestroyedBy(source, damage);
            }
        }
        else
        {
            super.onDestroyedBy(source, damage);
        }
    }

    @Override
    public void tick()
    {
        super.tick();
        this.mainAction.update(this.tickCount, !this.level().isClientSide());

        // Ticking trigger
        if (!this.level().isClientSide())
        {
            final IActionStatus status = mainAction.doAction(level(), getX(), getY(), getZ(), new EntityCause(this));
            if (!status.isType(ActionStatusTypes.BLOCKING) && !status.isType(ActionStatusTypes.ERROR))
            {
                this.destroy();
            }
        }
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
        final IActionStatus status = mainAction.doAction(level(), hitVec.x, hitVec.y, hitVec.z, new EntityCause(this));
        if (!status.isType(ActionStatusTypes.BLOCKING))
        {
            super.actionOnImpact(impactLocation);
        }
    }

    @Override
    public ItemStack toStack()
    {
        return originalStack.isEmpty() ? (ModItems.MISSILE_MODULE != null ? new ItemStack(ModItems.MISSILE_MODULE) : ItemStack.EMPTY) : originalStack;
    }

    @Override
    public Component getName()
    {
        final IActionData data = mainAction.getActionData();
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

    private static final SaveHandler<EntityMissileActionable> SAVE_LOGIC = new SaveHandler<EntityMissileActionable>()
        .mainRoot()
        /* */.nodeSavable("potential_action", EntityMissileActionable::getMainAction)
        /* */.nodeItemStack("original_stack", EntityMissileActionable::getOriginalStack, EntityMissileActionable::setOriginalStack)
        .base();
}
