package icbm.classic.prefab.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Base menu of the ICBM machines, 1.12 {@code icbm.classic.prefab.gui.ContainerBase} ported onto
 * {@link AbstractContainerMenu} (porting-guide.md §7).
 * <p>
 * The 1.12 class extended Forge's {@code Container}; 26.3 menus are registered by {@code MenuType}, so every
 * constructor takes the type and the container id. Kept from 1.12: the {@code host}/{@code inventory}
 * bookkeeping, the player use list of {@link IPlayerUsing}, {@link #addPlayerInventory(Inventory)} at
 * {@code (8, 84)} and {@link #stillValid(Player)} (1.12 {@code canInteractWith}), including the 4 block
 * reach check used when the host is a block entity.
 * <p>
 * Added for 26.3: {@link #quickMoveStack(Player, int)}. 1.12's {@code ContainerBase} had no
 * {@code transferStackInSlot}, every menu implemented it itself; 26.3 makes it abstract, so a generic
 * "machine slots first, player inventory last" implementation is provided here. It assumes the machine
 * slots are added before {@link #addPlayerInventory(Inventory, int, int)}, which adds the 36 player slots
 * ({@code addStandardInventorySlots}).
 * <p>
 * Created by Dark(DarkGuardsman, Robin).
 */
public class ContainerBase<H> extends AbstractContainerMenu
{
    /** Number of player slots {@link #addPlayerInventory(Inventory, int, int)} adds */
    private static final int PLAYER_SLOTS = 36;

    protected int slotCount = 0;

    protected Container inventory;
    protected Player player;
    protected H host;

    protected ContainerBase(MenuType<?> type, int id, Inventory playerInventory)
    {
        super(type, id);
        this.player = playerInventory.player;
    }

    /**
     * 1.12 {@code ContainerBase(player, IInventory)}: the inventory is also the host of the menu, so
     * {@link #stillValid(Player)} asks it and the player is added to the use list of an {@link IPlayerUsing}
     * inventory.
     */
    public ContainerBase(MenuType<?> type, int id, Inventory playerInventory, Container inventory)
    {
        this(type, id, playerInventory);
        this.inventory = inventory;
        this.slotCount = inventory.getContainerSize();
        if (inventory instanceof IPlayerUsing using)
        {
            using.addPlayerToUseList(playerInventory.player);
        }
    }

    /**
     * 1.12 {@code ContainerBase(player, H node)}: generic host, an inventory host is detected exactly
     * like 1.12 did with {@code node instanceof IInventory}.
     */
    public ContainerBase(MenuType<?> type, int id, Inventory playerInventory, H node)
    {
        this(type, id, playerInventory);
        if (node instanceof Container container)
        {
            this.inventory = container;
            this.slotCount = container.getContainerSize();
        }
        this.host = node;
        if (node instanceof IPlayerUsing using)
        {
            using.addPlayerToUseList(playerInventory.player);
        }
    }

    /** 1.12 {@code onContainerClosed(player)} */
    @Override
    public void removed(Player player)
    {
        if (host instanceof IPlayerUsing using && player.containerMenu != this)
        {
            using.removePlayerToUseList(player);
        }
        super.removed(player);
    }

    public void addPlayerInventory(Inventory playerInventory)
    {
        addPlayerInventory(playerInventory, 8, 84);
    }

    public void addPlayerInventory(Inventory playerInventory, int x, int y)
    {
        if (this.inventory instanceof IPlayerUsing using)
        {
            using.addPlayerToUseList(playerInventory.player);
        }

        //1.12 added the 3 inventory rows and the hotbar by hand; 26.3's helper does the same layout
        this.addStandardInventorySlots(playerInventory, x, y);
    }

    /**
     * 1.12 {@code canInteractWith(player)}.
     */
    @Override
    public boolean stillValid(Player player)
    {
        if (this.inventory != null)
        {
            return this.inventory.stillValid(player);
        }
        else if (this.host instanceof BlockEntity blockEntity)
        {
            //1.12: entityplayer.getDistance(pos.getX() + 0.5, ...) <= 4.0
            return Container.stillValidBlockEntity(blockEntity, player, 4.0F);
        }
        return true;
    }

    /**
     * Generic shift-click transfer between the machine inventory (first {@link #slotCount} slots) and the
     * player inventory (the last 36 slots).
     * <p>
     * 1.12 menu code overrode {@code transferStackInSlot} per menu; this generic version keeps one
     * implementation for every machine menu and lets a menu override it when the 1.12 code did something
     * different.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        final Slot slot = index >= 0 && index < this.slots.size() ? this.slots.get(index) : null;
        if (slot == null || !slot.hasItem())
        {
            return ItemStack.EMPTY;
        }

        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();

        final int playerSlots = Math.min(PLAYER_SLOTS, this.slots.size());
        final int machineSlots = this.slots.size() - playerSlots;

        if (index < machineSlots)
        {
            //machine -> player inventory
            if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            //player inventory -> machine
            if (!this.moveItemStackTo(stack, 0, machineSlots, false))
            {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty())
        {
            slot.setByPlayer(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount())
        {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return original;
    }
}
