package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModCreativeTabs
{
    public static CreativeModeTab MAIN;

    private ModCreativeTabs() {}

    public static void register()
    {
        MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ICBMConstants.id("main"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.icbmclassic.main"))
                        .icon(() -> new ItemStack(Items.TNT))
                        .displayItems((p, out) -> {})
                        .build());
    }
}
