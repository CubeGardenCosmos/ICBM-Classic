package icbm.classic.content.cargo.parachute;

import icbm.classic.ICBMConstants;
import icbm.classic.content.cargo.CargoProjectileData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

@EqualsAndHashCode(callSuper = true)
@ToString
public class ParachuteProjectileData extends CargoProjectileData<ParachuteProjectileData, EntityParachute>
{
    public static final Identifier NAME = ICBMConstants.id("holder.parachute");

    public ParachuteProjectileData()
    {
        super(NAME, createSaveHandler());
    }

    @Override
    public EntityParachute newEntity(Level world, boolean allowItemPickup)
    {
        return new EntityParachute(world);
    }
}
