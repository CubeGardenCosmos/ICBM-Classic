package icbm.classic.content.items.behavior;

import icbm.classic.content.entity.EntityBombCart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.tags.BlockTags;

/** Places bomb carts on rails, otherwise using the normal dispenser item fallback (1.12 behavior). */
public class BombCartDispenseBehavior extends DefaultDispenseItemBehavior
{
    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return stack;
        }

        final Direction facing = source.state().getValue(DispenserBlock.FACING);
        final BlockPos frontPos = source.pos().relative(facing);
        final BlockState frontState = source.level().getBlockState(frontPos);
        final boolean railInFront = frontState.is(BlockTags.RAILS);
        final BlockPos railPos;
        final BlockState railState;
        final double heightDelta;

        if (railInFront)
        {
            railPos = frontPos;
            railState = frontState;
            heightDelta = railShape(railState).isSlope() ? 0.6D : 0.1D;
        }
        else
        {
            railPos = frontPos.below();
            railState = source.level().getBlockState(railPos);
            if (!frontState.isAir() || !railState.is(BlockTags.RAILS))
            {
                return super.execute(source, stack);
            }
            heightDelta = facing != Direction.DOWN && railShape(railState).isSlope() ? -0.4D : -0.9D;
        }

        final double x = source.center().x + facing.getStepX() * 1.125D;
        final double y = Math.floor(source.center().y) + facing.getStepY() + heightDelta;
        final double z = source.center().z + facing.getStepZ() * 1.125D;
        final EntityBombCart cart = new EntityBombCart(source.level(), x, y, z, stack.copyWithCount(1));
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
        {
            cart.setCustomName(stack.getHoverName());
        }
        source.level().addFreshEntity(cart);
        stack.shrink(1);
        return stack;
    }

    private static RailShape railShape(BlockState state)
    {
        return state.getBlock() instanceof BaseRailBlock rail
                ? state.getValue(rail.getShapeProperty())
                : RailShape.NORTH_SOUTH;
    }
}
