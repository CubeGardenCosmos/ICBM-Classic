package icbm.classic.content.missile.entity.itemstack.item;

import icbm.classic.content.reg.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ItemHeldItemMissile extends Item
{
    public static final int MAX_STACK_SIZE = 1;

    public ItemHeldItemMissile(Item.Properties properties)
    {
        super(properties.stacksTo(MAX_STACK_SIZE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        final ItemStack held = stack.get(ModDataComponents.HELD_ITEM);
        if (held != null && !held.isEmpty())
        {
            tooltip.accept(Component.translatable("icbmclassic.projectile.holder.held").append(": ").append(held.getHoverName()));
        }
        else
        {
            tooltip.accept(Component.translatable("icbmclassic.item.held_item_missile.info"));
        }
    }
}
