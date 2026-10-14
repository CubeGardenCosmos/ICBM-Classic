package icbm.classic.content.items;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Flat missile item ({@code <explosive>_missile}, registry-ids.md §1 form M).
 * <p>
 * 1.12 used metadata to select the explosive; ADR 0002 flattens them into one item per explosive.
 * Missile items cannot be used directly by players (they are loaded into launchers).
 */
public class ItemMissile extends ExplosiveFormItem
{
    /** 1.12 {@code setMaxStackSize(1)} */
    public static final int MAX_STACK_SIZE = 1;

    public ItemMissile(Identifier explosiveId, ExplosiveForm form, Item.Properties properties)
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
