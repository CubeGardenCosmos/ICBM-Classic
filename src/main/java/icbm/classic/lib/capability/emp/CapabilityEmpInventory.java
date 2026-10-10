package icbm.classic.lib.capability.emp;

import icbm.classic.ICBMClassic;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Used to wrapper an inventory to provide support to EMP the contents.
 * <p>
 * By default the EMP content ({@code ActionEmpArea}, S3.07) will generate this object
 * for the target of the EMP.
 * <p>
 * 1.12 wrapped the Forge item handler capability ({@code IItemHandlerModifiable}). 26.3 dropped that
 * in favor of the vanilla {@link Container}, so the wrappers below type check the host instead of
 * asking for a capability.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
public abstract class CapabilityEmpInventory<H extends Object> implements IEMPReceiver
{
    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
    {
        final Container container = getContainer();
        if (container != null)
        {
            //Loop items
            for (int slotIndex = 0; slotIndex < container.getContainerSize(); slotIndex++)
            {
                final ItemStack slotStack = container.getItem(slotIndex);

                //Check to make sure its not a placeholder
                if (!slotStack.isEmpty())
                {
                    //Copy stack for editing
                    final ItemStack itemStack = slotStack.copy();

                    //Run call
                    power = empItemStack(itemStack, level, x, y, z, getHost(), emp_blast, power, doAction);

                    //Only apply changes if we are not simulating and there was a change in the stack
                    if (doAction && !ItemStack.matches(itemStack, slotStack))
                    {
                        try
                        {
                            container.setItem(slotIndex, itemStack);
                            container.setChanged();
                        }
                        catch (RuntimeException e) //Safety check, just in case
                        {
                            ICBMClassic.logger().error("BlastEMP: Unexpected error while updating inventory item '" + itemStack
                                    + "' from slot '" + slotIndex
                                    + "' in '" + container
                                    + "' contained in '" + getHost() + "' while running " + this, e);
                        }
                    }
                }
            }
        }
        return power;
    }

    public static float empItemStack(ItemStack itemStack, Level level, double x, double y, double z, Object container, IAction emp_blast, float power, boolean doAction)
    {
        boolean doInventory = ConfigBlast.emp.ALLOW_ITEM_INVENTORY;

        //Check for EMP support
        final IEMPReceiver receiver = IcbmLookups.EMP_RECEIVER_ITEM.find(itemStack, null);
        if (receiver != null)
        {
            //Apply effect, 1.12 always passed true here even when simulating
            power = receiver.applyEmpAction(level, x, y, z, emp_blast, power, true);
            doInventory = receiver.shouldEmpSubObjects(level, x, y, z) && doInventory;
        }
        else if (ConfigBlast.emp.DRAIN_ENERGY_ITEMS)
        {
            // TODO(S6.05) 1.12 drained the item's energy storage here through lib/energy/system/**, which was
            //  replaced by Team Reborn Energy (ADR 0003). Reconnect once the S6 energy facade exists.
        }

        if (doInventory)
        {
            if (container instanceof Entity entity)
            {
                power = new EntityInv(entity).applyEmpAction(level, x, y, z, emp_blast, power, doAction);
            }
            else if (container instanceof BlockEntity blockEntity)
            {
                power = new TileInv(blockEntity).applyEmpAction(level, x, y, z, emp_blast, power, doAction);
            }
            // TODO(S3.07) item contained inventories (1.12 ItemInvInEntity/ItemInvInTile): 1.12 asked the
            //  ItemStack for the Forge item handler capability; 26.3 stores item inventories in the
            //  icbmclassic content data components, which arrive with S2.04/S3.07.
        }
        return power;
    }


    protected abstract Container getContainer();

    protected abstract H getHost();

    public static class EntityInv extends CapabilityEmpInventory<Entity>
    {
        public final Entity entity;

        public EntityInv(Entity entity)
        {
            this.entity = entity;
        }

        @Override
        protected Container getContainer()
        {
            if (ConfigBlast.emp.ALLOW_ENTITY_INVENTORY && entity instanceof Container container)
            {
                return container;
            }
            return null;
        }

        @Override
        protected Entity getHost()
        {
            return entity;
        }
    }

    public static class TileInv extends CapabilityEmpInventory<BlockEntity>
    {
        public final BlockEntity entity;

        public TileInv(BlockEntity entity)
        {
            this.entity = entity;
        }

        @Override
        protected Container getContainer()
        {
            if (ConfigBlast.emp.ALLOW_TILE_INVENTORY && entity instanceof Container container)
            {
                return container;
            }
            return null;
        }

        @Override
        protected BlockEntity getHost()
        {
            return entity;
        }
    }
}
