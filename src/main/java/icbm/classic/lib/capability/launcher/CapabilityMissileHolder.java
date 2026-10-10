package icbm.classic.lib.capability.launcher;

import icbm.classic.api.caps.IMissileHolder;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Default {@link IMissileHolder} implementation for a slot inside a {@link Container}.
 * <p>
 * 1.12 used the Forge item handler capability ({@code IItemHandlerModifiable}); 26.3 block entities
 * expose a vanilla {@link Container}, so this implementation takes that instead. The class no longer
 * registers a Forge capability - the host block entity registers itself with
 * {@link IcbmLookups#MISSILE_HOLDER}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/9/19.
 */
public class CapabilityMissileHolder implements IMissileHolder
{
    private final Container inventory;
    private final int slot;

    public CapabilityMissileHolder(Container inventory, int slot)
    {
        this.inventory = inventory;
        this.slot = slot;
    }

    @Override
    public ItemStack getMissileStack()
    {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack insertMissileStack(ItemStack stack, boolean simulate)
    {
        if (canSupportMissile(stack))
        {
            final ItemStack existing = inventory.getItem(slot);

            //Same behavior contract as the 1.12 IItemHandler#insertItem
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack))
            {
                return stack;
            }

            // 26.3 ItemStack has no getMaxStackSize(); the item default is the closest equivalent
            final int space = Math.min(inventory.getMaxStackSize(stack), stack.getItem().getDefaultMaxStackSize()) - existing.getCount();
            final int moved = Math.min(Math.max(space, 0), stack.getCount());
            if (moved <= 0)
            {
                return stack;
            }

            if (!simulate)
            {
                final ItemStack result = existing.isEmpty()
                        ? stack.copyWithCount(moved)
                        : existing.copyWithCount(existing.getCount() + moved);
                inventory.setItem(slot, result);
                inventory.setChanged();
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }
        return null;
    }

    @Override
    public boolean consumeMissile()
    {
        if (hasMissile())
        {
            final ICapabilityMissileStack missileStack = IcbmLookups.MISSILE_STACK.find(getMissileStack(), null);
            if (missileStack != null)
            {
                inventory.setItem(slot, missileStack.consumeMissile());
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canSupportMissile(ItemStack stack)
    {
        return inventory.canPlaceItem(slot, stack);
    }
}
