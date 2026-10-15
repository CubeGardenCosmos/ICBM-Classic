package icbm.classic.content.items.behavior;

import icbm.classic.content.entity.EntityGrenade;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/** Launches the grenade in the dispenser's facing direction (1.12 {@code GrenadeDispenseBehavior}). */
public class GrenadeDispenseBehavior extends DefaultDispenseItemBehavior
{
    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return stack;
        }

        final Direction facing = source.state().getValue(DispenserBlock.FACING);
        final Vec3 center = source.center();
        final EntityGrenade grenade = new EntityGrenade(source.level())
                .setItemStack(stack.copyWithCount(1));
        grenade.setPos(center.x, center.y, center.z);
        grenade.setThrowableHeading(facing.getStepX(), 0.10000000149011612D, facing.getStepZ(), 0.5F, 1.0F);
        source.level().addFreshEntity(grenade);
        stack.shrink(1);
        return stack;
    }
}
