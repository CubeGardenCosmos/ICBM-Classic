package icbm.classic.content.cargo.balloon;

import icbm.classic.ICBMConstants;
import icbm.classic.content.cargo.CargoProjectileData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

@EqualsAndHashCode(callSuper = true)
@ToString
public class BalloonProjectileData extends CargoProjectileData<BalloonProjectileData, EntityBalloon>
{
    public static final Identifier NAME = ICBMConstants.id("holder.balloon");

    public BalloonProjectileData()
    {
        super(NAME, createSaveHandler());
    }

    @Override
    public EntityBalloon newEntity(Level world, boolean allowItemPickup)
    {
        return new EntityBalloon(world);
    }
}
