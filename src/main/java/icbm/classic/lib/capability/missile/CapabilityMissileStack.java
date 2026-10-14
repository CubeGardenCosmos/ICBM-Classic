package icbm.classic.lib.capability.missile;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.reg.ModEntities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Capability applied to {@link ItemStack}s that are missiles.
 */
public class CapabilityMissileStack implements ICapabilityMissileStack
{
    private final ItemStack stack;

    public CapabilityMissileStack(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public String getMissileId()
    {
        return ICBMConstants.PREFIX + "missile["
            + Optional.ofNullable(ICBMClassicHelpers.getExplosive(stack))
            .map(IExplosive::getExplosiveData)
            .map(IExplosiveData::getRegistryKey)
            .map(Object::toString)
            .orElse("unknown")
            + "]";
    }

    @Override
    public IMissile newMissile(Level world)
    {
        final EntityExplosiveMissile missile = new EntityExplosiveMissile(ModEntities.EXPLOSIVE_MISSILE, world);
        missile.explosive.setStack(stack);
        missile.initHealth(missile.getMaxHealth());
        return missile.getMissileCapability();
    }
}
