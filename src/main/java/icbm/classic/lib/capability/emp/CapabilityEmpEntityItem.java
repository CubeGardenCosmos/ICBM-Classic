package icbm.classic.lib.capability.emp;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Wrapper to trigger EMP calls on ItemStack contained inside of {@link ItemEntity}
 * <p>
 * 1.12 wrapped this in a Forge capability ({@code ICapabilityProvider}); 26.3 provides the receiver
 * through the {@code emp_receiver} entity lookup (see {@link CapabilityEMP}).
 *
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
public class CapabilityEmpEntityItem implements IEMPReceiver
{
    public final ItemEntity entityItem;

    public CapabilityEmpEntityItem(ItemEntity entityItem)
    {
        this.entityItem = entityItem;
    }

    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
    {
        if (ConfigBlast.emp.ALLOW_GROUND_ITEMS)
        {
            ItemStack stack = entityItem.getItem();
            if (!stack.isEmpty())
            {
                //Copy to prevent changes on real item
                stack = stack.copy();

                //Run call
                power = CapabilityEmpInventory.empItemStack(stack, level, x, y, z, entityItem, emp_blast, power, doAction);

                //Check for delta
                if (doAction && !ItemStack.matches(stack, entityItem.getItem()))
                {
                    entityItem.setItem(stack);
                }
            }
        }
        return power;
    }
}
