package icbm.classic.content.blocks.emptower;

import icbm.classic.config.machines.ConfigEmpTower;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Block item tooltip corresponding to the two legacy ItemBlockSubTypes variants. */
public class ItemBlockEmpTower extends BlockItem
{
    public ItemBlockEmpTower(Block block, Item.Properties properties)
    {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, context, display, tooltip, flag);
        final BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
        final boolean extender = properties != null
                && properties.get(BlockEmpTower.TOWER_MODELS) == PropertyTowerStates.EnumTowerTypes.SPIN;
        tooltip.accept(extender
                ? Component.translatable("icbmclassic.block.emp_tower.1.info", ConfigEmpTower.BONUS_RADIUS)
                : Component.translatable("icbmclassic.block.emp_tower.0.info"));
    }
}
