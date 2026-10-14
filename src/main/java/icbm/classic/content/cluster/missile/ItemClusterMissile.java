package icbm.classic.content.cluster.missile;

import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.lib.data.ExplosiveSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ItemClusterMissile extends Item
{
    public static final int MAX_STACK_SIZE = 1;

    public ItemClusterMissile(Item.Properties properties)
    {
        super(properties.stacksTo(MAX_STACK_SIZE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        tooltip.accept(Component.translatable("icbmclassic.item.cluster_missile.info"));
        final ExplosiveSettings settings = stack.get(ModDataComponents.EXPLOSIVE_SETTINGS);
        if (settings != null && settings.cluster().isPresent())
        {
            final ExplosiveSettings.ClusterSettings cluster = settings.cluster().get();
            tooltip.accept(Component.literal("  " + cluster.count() + " x " + cluster.bombletExplosive().getPath()));
        }
    }
}
