package icbm.classic.content.reg;

import net.minecraft.world.item.Item;

public final class ModItems
{
    private ModItems() {}

    public static void register()
    {
        RegHelper.blockItem(ModBlocks.CONCRETE);
        RegHelper.blockItem(ModBlocks.COMPACT_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_GLASS);
        RegHelper.blockItem(ModBlocks.SPIKES);
        RegHelper.blockItem(ModBlocks.POISON_SPIKES);
        RegHelper.blockItem(ModBlocks.FIRE_SPIKES);
        RegHelper.blockItem(ModBlocks.GLASS_BUTTON);
        RegHelper.blockItem(ModBlocks.GLASS_PRESSURE_PLATE);
    }

    public static boolean isHiddenFromCreativeTab(Item item)
    {
        return false;
    }
}
