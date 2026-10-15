package icbm.classic.content.blocks.launcher.network;

import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** A flat view of the accepted item slots across launcher components in a network. */
public class NetworkInventory implements Container {

    private final LauncherNetwork network;
    private final List<SlotHolder> slots = new ArrayList<>();

    public NetworkInventory(LauncherNetwork network) {
        this.network = network;
    }

    public void buildInventory() {
        slots.clear();
        for (LauncherNode node : network.getComponents()) {
            if (!node.isAcceptsItems()) {
                continue;
            }
            final BlockEntity tile = node.getSelf();
            if (tile.getLevel() == null || IcbmLookups.MISSILE_LAUNCHER.find(tile.getLevel(), tile.getBlockPos(), null) == null || !(tile instanceof Container handler)) {
                continue;
            }
            for (int slot = 0; slot < handler.getContainerSize(); slot++) {
                slots.add(new SlotHolder(handler, slot));
            }
        }
    }

    @Override
    public int getContainerSize() {
        return slots.size();
    }

    @Override
    public boolean isEmpty() {
        return slots.stream().allMatch(slot -> slot.handler().getItem(slot.slotIndex()).isEmpty());
    }

    @Override
    @NonNull
    public ItemStack getItem(int slot) {
        validateSlotIndex(slot);
        final SlotHolder holder = slots.get(slot);
        return holder.handler().getItem(holder.slotIndex());
    }

    @Override
    @NonNull
    public ItemStack removeItem(int slot, int amount) {
        validateSlotIndex(slot);
        final SlotHolder holder = slots.get(slot);
        return holder.handler().removeItem(holder.slotIndex(), amount);
    }

    @Override
    @NonNull
    public ItemStack removeItemNoUpdate(int slot) {
        validateSlotIndex(slot);
        final SlotHolder holder = slots.get(slot);
        return holder.handler().removeItemNoUpdate(holder.slotIndex());
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        validateSlotIndex(slot);
        final SlotHolder holder = slots.get(slot);
        holder.handler().setItem(holder.slotIndex(), stack);
    }

    @Override
    public void setChanged() {
        // Mutations already target the underlying component container.
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < slots.size(); slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }

    /** 1.12-compatible network slot operations over vanilla containers. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        final ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final int extracted = Math.min(amount, current.getCount());
        final ItemStack result = current.copyWithCount(extracted);
        if (!simulate) {
            final SlotHolder holder = slots.get(slot);
            holder.handler().removeItem(holder.slotIndex(), extracted);
        }
        return result;
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final SlotHolder holder = slots.get(slot);
        final Container container = holder.handler();
        final int underlyingSlot = holder.slotIndex();
        if (!container.canPlaceItem(underlyingSlot, stack)) {
            return stack;
        }
        final ItemStack current = container.getItem(underlyingSlot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
            return stack;
        }
        final int limit = Math.min(container.getMaxStackSize(stack), stack.getItem().getDefaultMaxStackSize());
        final int room = limit - current.getCount();
        if (room <= 0) {
            return stack;
        }
        final int inserted = Math.min(room, stack.getCount());
        if (!simulate) {
            final ItemStack updated = current.isEmpty() ? stack.copyWithCount(inserted) : current.copyWithCount(current.getCount() + inserted);
            container.setItem(underlyingSlot, updated);
            container.setChanged();
        }
        return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
    }

    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        final Container container = slots.get(slot).handler();
        return container.getMaxStackSize(container.getItem(slots.get(slot).slotIndex()));
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        final SlotHolder holder = slots.get(slot);
        return holder.handler().canPlaceItem(holder.slotIndex(), stack);
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= slots.size()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " not in valid range [0," + slots.size() + ")");
        }
    }

    private record SlotHolder(Container handler, int slotIndex) {}
}
