package icbm.classic.content.blocks.radarstation.gui;

import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.prefab.gui.ContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerRadarStation extends ContainerBase<TileRadarStation>
{
    public ContainerRadarStation(int id, Inventory playerInventory, TileRadarStation tile)
    {
        super(ModMenus.RADAR_STATION, id, playerInventory, tile);
        addSlot(new Slot(tile.inventory, 0, 141, 47)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(icbm.classic.ICBMConstants.id("battery"));
            }
        });
        addPlayerInventory(playerInventory, 7, 102);
    }

    public ContainerRadarStation(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (TileRadarStation) playerInventory.player.level().getBlockEntity(pos));
    }

    public TileRadarStation getTile()
    {
        return host;
    }
}
