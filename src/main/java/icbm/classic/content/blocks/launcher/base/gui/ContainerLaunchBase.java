package icbm.classic.content.blocks.launcher.base.gui;

import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.prefab.gui.ContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Menu for the launcher base inventory and machine settings. */
public class ContainerLaunchBase extends ContainerBase<TileLauncherBase>
{
    public ContainerLaunchBase(int id, Inventory playerInventory, TileLauncherBase tile)
    {
        super(ModMenus.LAUNCHER_BASE, id, playerInventory, tile);
        addEnergyData(tile.energyStorage);
        addSlot(new Slot(tile.inventory, 0, 88, 47));
        addSlot(new Slot(tile.inventory, 1, 141, 47));
        addPlayerInventory(playerInventory, 7, 84);
    }

    /** Client constructor; the extended menu payload identifies the block entity. */
    public ContainerLaunchBase(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (TileLauncherBase) playerInventory.player.level().getBlockEntity(pos));
    }

    public TileLauncherBase getTile()
    {
        return host;
    }
}
