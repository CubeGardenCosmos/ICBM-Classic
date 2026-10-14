package icbm.classic.content.missile.entity.anti.item;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.entity.anti.EntitySurfaceToAirMissile;
import icbm.classic.content.reg.ModEntities;
import net.minecraft.world.level.Level;

/**
 * Capability providing missile building capabilities for the SAM / ABM item stack.
 */
public class CapabilitySAMStack implements ICapabilityMissileStack
{
    @Override
    public String getMissileId()
    {
        return ICBMConstants.PREFIX + "missile.sam";
    }

    @Override
    public IMissile newMissile(Level world)
    {
        return new EntitySurfaceToAirMissile(ModEntities.MISSILE_SAM, world).getMissileCapability();
    }
}
