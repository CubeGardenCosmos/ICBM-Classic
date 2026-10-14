package icbm.classic.content.missile.entity.itemstack.item;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.itemstack.EntityHeldItemMissile;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CapabilityHeldItemMissile implements ICapabilityMissileStack
{
    private final ItemStack stack;

    public CapabilityHeldItemMissile(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public String getMissileId()
    {
        return ICBMConstants.PREFIX + "missile[held_item]";
    }

    @Override
    public IMissile newMissile(Level world)
    {
        final EntityHeldItemMissile missile = new EntityHeldItemMissile(ModEntities.HELD_ITEM_MISSILE, world);
        missile.setOriginalStack(stack);
        final ItemStack held = stack.getOrDefault(ModDataComponents.HELD_ITEM, ItemStack.EMPTY);
        missile.setHeldItem(held.copy());
        missile.initHealth(ConfigMissile.TIER_2_HEALTH);
        return missile.getMissileCapability();
    }
}
