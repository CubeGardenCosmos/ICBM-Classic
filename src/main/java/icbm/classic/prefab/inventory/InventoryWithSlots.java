package icbm.classic.prefab.inventory;

import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.tile.ITick;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Inventory with per slot configuration. 1.12 extended Forge's {@code ItemStackHandler}; 26.3 machine
 * inventories are vanilla {@link Container}s (porting-guide.md §2.6), so this implements {@link Container}
 * directly and keeps the 1.12 slot API.
 * <p>
 * Differences to the 1.12 class, all forced by 26.3:
 * <ul>
 *     <li>the Forge item handler names map to the vanilla container names: {@code getSlots}→
 *     {@link #getContainerSize}, {@code getStackInSlot}→{@link #getItem}, {@code setStackInSlot}→
 *     {@link #setItem}, {@code extractItem}/{@code insertItem} keep their 1.12 shape,</li>
 *     <li>1.12 {@code getSlot(int)} is renamed {@link #getSlotHandler(int)} because
 *     {@link Container#getSlot(int)} (added in 26.3) has the same name and a different return type,</li>
 *     <li>1.12 {@code setSize} and the {@code Size} save entry are gone: 26.3 containers have a fixed size
 *     that is defined by the machine code,</li>
 *     <li>the save is written through {@link ContainerHelper} into a {@link ValueOutput} (S1.03 style
 *     {@link ISavable}) instead of Forge's {@code INBTSerializable} of a raw NBT tag.</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin).
 */
public class InventoryWithSlots implements Container, ITick, ISavable
{
    private final InventorySlot[] slotHandlers;
    private NonNullList<ItemStack> stacks;

    @Getter
    private BiConsumer<Integer, ItemStack> onContentChange;

    public InventoryWithSlots(int size)
    {
        slotHandlers = new InventorySlot[size];
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // slot configuration
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    public InventoryWithSlots withSlot(InventorySlot slot)
    {
        if (slot.getSlot() < 0 || slot.getSlot() >= slotHandlers.length)
        {
            throw new IllegalArgumentException("Slot " + slot.getSlot() + " is outside of the inventory size " + slotHandlers.length);
        }
        slotHandlers[slot.getSlot()] = slot;
        return this;
    }

    public InventoryWithSlots withChangeCallback(BiConsumer<Integer, ItemStack> onChange)
    {
        this.onContentChange = onChange;
        return this;
    }

    /** Slot configuration of the given slot, empty when the slot has none. 1.12 {@code getSlot(int)} */
    public Optional<InventorySlot> getSlotHandler(int slot)
    {
        if (slot >= 0 && slot < slotHandlers.length)
        {
            return Optional.ofNullable(slotHandlers[slot]);
        }
        return Optional.empty();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // item moving (1.12 ItemStackHandler API, kept for the machines)
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 1.12 {@code ItemStackHandler#insertItem}: only inserts when the slot validator and the insert check
     * allow the stack, returns the leftover stack.
     */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (!isItemValid(slot, stack) || !getSlotHandler(slot).map(InventorySlot::getOnInsertStack).map(s -> s.apply(stack)).orElse(true))
        {
            return stack;
        }

        final ItemStack existing = getItem(slot);
        final int limit = Math.min(getMaxStackSize(), stack.getMaxStackSize());
        int room = limit;
        if (!existing.isEmpty())
        {
            if (!ItemStack.isSameItemSameComponents(existing, stack))
            {
                return stack;
            }
            room = limit - existing.getCount();
        }
        if (room <= 0)
        {
            return stack;
        }

        final int moved = Math.min(room, stack.getCount());
        if (!simulate)
        {
            final ItemStack placed = (existing.isEmpty() ? stack : existing).copy();
            placed.setCount((existing.isEmpty() ? 0 : existing.getCount()) + moved);
            setItem(slot, placed);
        }
        final ItemStack leftover = stack.copy();
        leftover.setCount(stack.getCount() - moved);
        return leftover.isEmpty() ? ItemStack.EMPTY : leftover;
    }

    /** 1.12 {@code ItemStackHandler#extractItem} */
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount <= 0)
        {
            return ItemStack.EMPTY;
        }
        final ItemStack existing = getItem(slot);
        if (existing.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        final int taken = Math.min(amount, existing.getCount());
        final ItemStack extracted = existing.copy();
        extracted.setCount(taken);
        if (!simulate)
        {
            final ItemStack left = existing.copy();
            left.setCount(existing.getCount() - taken);
            setItem(slot, left);
        }
        return extracted;
    }

    /**
     * 1.12 {@code ItemStackHandler#isItemValid}, also used as {@link Container#canPlaceItem(int, ItemStack)}.
     * Slots without a {@link InventorySlot} reject every item, as in 1.12.
     */
    public boolean isItemValid(int slot, ItemStack stack)
    {
        if (slot < 0 || slot >= stacks.size())
        {
            return false;
        }
        return getSlotHandler(slot).map(InventorySlot::getValidator).map(s -> s.apply(stack)).orElse(false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Container
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getContainerSize()
    {
        return stacks.size();
    }

    @Override
    public boolean isEmpty()
    {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        final ItemStack removed = ContainerHelper.removeItem(stacks, slot, amount);
        if (!removed.isEmpty())
        {
            onContentsChanged(slot);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        return ContainerHelper.takeItem(stacks, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack)
    {
        return isItemValid(slot, stack);
    }

    @Override
    public void clearContent()
    {
        for (int slot = 0; slot < stacks.size(); slot++)
        {
            setItem(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Container level change notification. 1.12's {@code ItemStackHandler} had no counterpart, owners only
     * passed {@code this::markDirty} to {@link #withChangeCallback(BiConsumer)} which reports the changed
     * slot. The container level call (made by vanilla menus and hopper style automation) is reported with
     * slot {@code -1} so owners do not need a second hook.
     */
    @Override
    public void setChanged()
    {
        if (onContentChange != null)
        {
            onContentChange.accept(-1, ItemStack.EMPTY);
        }
    }

    /** No position of its own, the owning menu checks the machine (see {@code ContainerBase#stillValid}) */
    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // tick + save
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void update(int tick, boolean isServer)
    {
        if (isServer)
        {
            for (InventorySlot slot : slotHandlers)
            {
                if (slot != null && slot.getOnTick() != null)
                {
                    setItem(slot.getSlot(), slot.getOnTick().apply(getItem(slot.getSlot())));
                }
            }
        }
    }

    /** 1.12 {@code ItemStackHandler#serializeNBT} wrote the {@code Items} list (and {@code Size}) */
    @Override
    public void save(ValueOutput out)
    {
        ContainerHelper.saveAllItems(out, stacks);
    }

    /** 1.12 {@code ItemStackHandler#deserializeNBT} read the {@code Items} list */
    @Override
    public void load(ValueInput in)
    {
        ContainerHelper.loadAllItems(in, stacks);
    }

    private void onContentsChanged(int slot)
    {
        if (onContentChange != null)
        {
            onContentChange.accept(slot, getItem(slot));
        }
        getSlotHandler(slot).map(InventorySlot::getOnContentChanged).ifPresent(s -> s.accept(getItem(slot)));
    }
}
