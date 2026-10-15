package icbm.classic.content.blocks.emptower.gui;

import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.reg.ModMenus;
import icbm.classic.prefab.gui.ContainerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class ContainerEMPTower extends ContainerBase<TileEMPTower>
{
    public ContainerEMPTower(int id, Inventory playerInventory, TileEMPTower tile)
    {
        super(ModMenus.EMP_TOWER, id, playerInventory, tile);
        addSlot(new Slot(tile.inventory, 0, 141, 47)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(icbm.classic.ICBMConstants.id("battery"));
            }
        });
        addPlayerInventory(playerInventory, 7, 84);
    }

    public ContainerEMPTower(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, (TileEMPTower) playerInventory.player.level().getBlockEntity(pos));
    }

    public TileEMPTower getTile() { return host; }
}
