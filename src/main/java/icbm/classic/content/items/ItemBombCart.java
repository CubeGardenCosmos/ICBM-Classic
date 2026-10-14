package icbm.classic.content.items;

import icbm.classic.content.entity.EntityBombCart;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.function.Consumer;

/**
 * Flat bomb cart item ({@code <explosive>_bomb_cart}, registry-ids.md §1 form C). Placing it on a rail
 * spawns an {@link EntityBombCart} with that explosive (1.12 {@code ItemBombCart#onItemUse}).
 * <p>
 * 1.12 used metadata to select the explosive, ADR 0002 flattens that into one item per explosive, so the
 * explosive id is the {@link ExplosiveFormItem#getExplosiveId()} of the item (see
 * {@link icbm.classic.lib.capability.ex.CapabilityExplosiveStack#fromStack}).
 * <p>
 * Porting notes: {@code onItemUse} → {@link #useOn(UseOnContext)}, {@code BlockRailBase.isRailBlock} →
 * {@link BlockTags#RAILS}, the Forge {@code initCapabilities} override is gone and the 1.12 metadata
 * plumbing ({@code getMetadata}/{@code getSubItems}/{@code getUnlocalizedName}) is dropped, one item per
 * explosive.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class ItemBombCart extends ExplosiveFormItem
{
    /** 1.12 {@code setMaxStackSize(3)} */
    public static final int MAX_STACK_SIZE = 3;

    public ItemBombCart(Identifier explosiveId, ExplosiveForm form, Item.Properties properties)
    {
        super(explosiveId, form, properties);
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have
     * one of those. Return True if something happen and false if it don't. This is for ITEMS, not
     * BLOCKS
     * <p>
     * 1.12 {@code onItemUse}: places the cart on a rail, at the vanilla minecart offset.
     */
    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);

        // 1.12 BlockRailBase.isRailBlock(iblockstate) -> 26.3 BlockState#is(BlockTags.RAILS)
        if (!state.is(BlockTags.RAILS))
        {
            return InteractionResult.FAIL;
        }

        final ItemStack itemstack = context.getItemInHand();

        if (!level.isClientSide()) //1.12: !worldIn.isRemote
        {
            final RailShape railBlock = state.getBlock() instanceof BaseRailBlock rail
                    ? state.getValue(rail.getShapeProperty())
                    : RailShape.NORTH_SOUTH;
            double d0 = 0.0D;

            if (railBlock.isSlope()) //1.12: railBlock.isAscending()
            {
                d0 = 0.5D;
            }

            final EntityBombCart cart = new EntityBombCart(level, pos.getX() + 0.5D, pos.getY() + 0.0625D + d0, pos.getZ() + 0.5D, itemstack);

            // 1.12 itemstack.hasDisplayName()/getDisplayName()
            if (itemstack.has(DataComponents.CUSTOM_NAME))
            {
                cart.setCustomName(itemstack.getHoverName());
            }

            level.addFreshEntity(cart);
        }

        itemstack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    /**
     * 1.12 {@code hasDetailedInfo}/{@code getDetailedInfo} delegated to {@code ItemBlockExplosive}, which
     * appended the explosive tier.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        appendExplosiveTier(tooltip);
    }
}
