package icbm.classic.content.blocks.launcher.cruise.gui;

import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.prefab.gui.ContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Cruise launcher menu with missile and reserved battery slots. */
public class ContainerCruiseLauncher extends ContainerBase<TileCruiseLauncher>
{
    public ContainerCruiseLauncher(int id, Inventory playerInventory, TileCruiseLauncher tile)
    {
        super(ModMenus.CRUISE_LAUNCHER, id, playerInventory, tile);
        addSlot(new Slot(tile.inventory, 0, 88, 47));
        addSlot(new Slot(tile.inventory, 1, 141, 47));
        addPlayerInventory(playerInventory, 7, 84);
    }

    public ContainerCruiseLauncher(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (TileCruiseLauncher) playerInventory.player.level().getBlockEntity(pos));
    }

    public TileCruiseLauncher getTile()
    {
        return host;
    }
}
