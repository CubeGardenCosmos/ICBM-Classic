package icbm.classic.content.blocks.launcher.screen.gui;

import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.prefab.gui.ContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Menu for a launcher screen's battery slot. */
public class ContainerLaunchScreen extends ContainerBase<TileLauncherScreen>
{
    public ContainerLaunchScreen(int id, Inventory playerInventory, TileLauncherScreen tile)
    {
        super(ModMenus.LAUNCHER_SCREEN, id, playerInventory, tile);
        addEnergyData(tile.energyStorage);
        addSlot(new Slot(tile.inventory, 0, 141, 47));
        addPlayerInventory(playerInventory, 7, 84);
    }

    /** Client constructor; the extended menu payload identifies the block entity. */
    public ContainerLaunchScreen(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (TileLauncherScreen) playerInventory.player.level().getBlockEntity(pos));
    }

    public TileLauncherScreen getTile()
    {
        return host;
    }
}
