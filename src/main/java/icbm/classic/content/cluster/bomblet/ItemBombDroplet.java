package icbm.classic.content.cluster.bomblet;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.content.items.ExplosiveFormItem;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Bomblet item representing an explosive submunition dropped from a cluster missile.
 */
public class ItemBombDroplet extends ExplosiveFormItem
{
    public static final int MAX_STACK_SIZE = 16;

    public ItemBombDroplet(Identifier explosiveId, ExplosiveForm form, Item.Properties properties)
    {
        super(explosiveId, form, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        appendExplosiveTier(tooltip);
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(stack);
        if (explosive != null)
        {
            explosive.collectInformation(str -> tooltip.accept(Component.literal(str)));
        }
    }
}
