package icbm.classic.content.missile.entity.itemstack;

import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.entity.itemstack.item.HeldItemMissileHandler;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.world.IProjectileBlockInteraction;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Missile holding an item, drops or uses the item on impact.
 */
public class EntityHeldItemMissile extends EntityMissile<EntityHeldItemMissile>
{
    private static final EntityDataAccessor<ItemStack> DATA_HELD_ITEM =
            SynchedEntityData.defineId(EntityHeldItemMissile.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_ACTION_MODE =
            SynchedEntityData.defineId(EntityHeldItemMissile.class, EntityDataSerializers.INT);

    @Getter
    private ItemStack heldItem = ItemStack.EMPTY;

    @Getter @Setter
    private ItemStack originalStack = ItemStack.EMPTY;

    @Getter @Setter
    private HeldActionMode actionMode = HeldActionMode.PRIMARY_FIRST;

    private boolean hasUsedAction = false;

    public EntityHeldItemMissile(EntityType<? extends EntityHeldItemMissile> type, Level world)
    {
        super(type, world);
        this.initHealth(ConfigMissile.TIER_2_HEALTH);
    }

    public EntityHeldItemMissile(Level world)
    {
        this(ModEntities.HELD_ITEM_MISSILE, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(DATA_HELD_ITEM, ItemStack.EMPTY);
        builder.define(DATA_ACTION_MODE, HeldActionMode.PRIMARY_FIRST.ordinal());
    }

    public void setHeldItem(ItemStack stack)
    {
        this.heldItem = stack == null ? ItemStack.EMPTY : stack;
        if (!this.heldItem.isEmpty())
        {
            this.entityData.set(DATA_HELD_ITEM, this.heldItem.copy());
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (DATA_HELD_ITEM.equals(key) && this.level().isClientSide())
        {
            this.heldItem = this.entityData.get(DATA_HELD_ITEM);
        }
    }

    @Override
    public ItemStack toStack()
    {
        final ItemStack stack = new ItemStack(ModItems.HELD_ITEM_MISSILE);
        if (!heldItem.isEmpty())
        {
            stack.set(ModDataComponents.HELD_ITEM, heldItem.copy());
        }
        return stack;
    }

    @Override
    protected IProjectileBlockInteraction.EnumHitReactions specialHandleBlock(BlockHitResult hit, double velocity)
    {
        if (!level().isClientSide())
        {
            if (!hasUsedAction && !heldItem.isEmpty() && HeldItemMissileHandler.isAllowed(heldItem))
            {
                hasUsedAction = true;
            }
            return IProjectileBlockInteraction.EnumHitReactions.CONTINUE;
        }
        return IProjectileBlockInteraction.EnumHitReactions.PASS;
    }

    @Override
    public void actionOnImpact(HitResult hit)
    {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel)
        {
            if (!this.heldItem.isEmpty())
            {
                this.spawnAtLocation(serverLevel, this.heldItem.copy());
                this.heldItem = ItemStack.EMPTY;
            }
            else
            {
                this.spawnAtLocation(serverLevel, toStack());
            }
            this.destroy();
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

    private static final SaveHandler<EntityHeldItemMissile> SAVE_LOGIC = new SaveHandler<EntityHeldItemMissile>()
            .mainRoot()
            .nodeItemStack("held", EntityHeldItemMissile::getHeldItem, EntityHeldItemMissile::setHeldItem)
            .nodeEnumString("action_mode", EntityHeldItemMissile::getActionMode, EntityHeldItemMissile::setActionMode, HeldActionMode::valueOf)
            .base();
}
