package icbm.classic.lib.capability.ex;

import icbm.classic.ICBMClassic;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

/**
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class CapabilityExplosiveEntity implements IExplosive, ISavable
{
    public final Entity entity;
    private ItemStack stack = ItemStack.EMPTY;

    private boolean isExploding = false;

    public CapabilityExplosiveEntity(@NonNull Entity entity)
    {
        this.entity = entity;
    }

    @Override
    public void save(ValueOutput out)
    {
        // 1.12 wrote the raw ItemStack tag into the caller's tag; 26.3 needs a named node for the codec
        out.store(NBTConstants.EXPLOSIVE_STACK, ItemStack.OPTIONAL_CODEC, stack);
    }

    @Override
    public void load(ValueInput in)
    {
        stack = in.read(NBTConstants.EXPLOSIVE_STACK, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    public IActionStatus doExplosion(double x, double y, double z, IActionSource source)
    {
        if (this.isExploding) {
            return ActionResponses.COMPLETED;
        }

        //Make sure to note we are currently exploding
        this.isExploding = true;

        if (!this.entity.level().isClientSide())
        {
            return this.getExplosiveData().create(this.entity.level(), x, y, z, source, null).doAction();
        }
        return ActionResponses.COMPLETED;
    }

    @NonNull
    @Override
    public IExplosiveData getExplosiveData()
    {
        final ItemStack stack = toStack();
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(stack);
        if (explosive != null)
        {
            return explosive.getExplosiveData();
        }
        return ICBMExplosives.CONDENSED;
    }

    @Override
    public void applyCustomizations(IBlast blast) {
        final ItemStack stack = toStack();
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(stack);
        if (explosive != null)
        {
            explosive.applyCustomizations(blast);
        }
    }

    @Override
    public void addCustomization(IExplosiveCustomization customization) {
        final ItemStack stack = toStack();
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(stack);
        if (explosive != null)
        {
            explosive.addCustomization(customization);
        }
    }

    @NonNull
    @Override
    public ItemStack toStack()
    {
        if (stack == null)
        {
            stack = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public void onDefuse()
    {
        // 1.12 world.spawnEntity(new EntityItem(...)) + setDead()
        final ItemEntity dropped = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), toStack().copy());
        entity.level().addFreshEntity(dropped);
        entity.discard();
    }

    public void setStack(@NonNull ItemStack stack)
    {
        if (ICBMClassicHelpers.getExplosive(stack) == null)
        {
            ICBMClassic.logger().error("CapabilityExplosive[" + entity + "] Was set with a stack that is not an explosive [" + stack + "]");
        }
        this.stack = stack.copyWithCount(1);
    }

    @Override
    public int hashCode() {
        // 1.12 also mixed in getItemDamage(), items are flat in 26.3
        return 31 * stack.getItem().hashCode() + stack.getCount();
    }

    @Override
    public boolean equals(Object other) {
        // 1.12 ItemStack.areItemsEqual -> 26.3 ItemStack.isSameItem
        return super.equals(other) || other instanceof IExplosive && ItemStack.isSameItem(((IExplosive) other).toStack(), toStack());
    }

    @Override
    public String toString() {
        return String.format("CapabilityExplosiveEntity[%s]@%s", toStack(), super.hashCode());
    }
}
