package icbm.classic.content.missile.logic.flight;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.config.missile.ConfigMissile;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

/**
 * Follows the location of the target data
 *
 * Created by Robin Seifert on 10/8/2022.
 */
public class FollowTargetLogic extends DeadFlightLogic
{
    public static final Identifier REG_NAME = ICBMConstants.id("direct.follow");

    public FollowTargetLogic() {
        //For save/load
    }

    public FollowTargetLogic(int fuel) {
        super(fuel);
    }

    @Override
    public float engineSmokeRed(Entity entity) {
        return 1;
    }

    @Override
    public float engineSmokeGreen(Entity entity) {
        return 0.5f;
    }

    @Override
    public float engineSmokeBlue(Entity entity) {
        return 0.5f;
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir)
    {
        super.onEntityTick(entity, missile, ticksInAir);
        if(!entity.level().isClientSide() && hasFuel(entity) && missile.getTargetData() != null) //TODO if we have no target data have missile fly around in random directions until it hits something
        {
            double motionX = missile.getTargetData().getX() - entity.getX();
            double motionY = missile.getTargetData().getY() - entity.getY();
            double motionZ = missile.getTargetData().getZ() - entity.getZ();

            float velocity = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
            motionX /= velocity;
            motionY /= velocity;
            motionZ /= velocity;

            //Set motion
            entity.setDeltaMovement(motionX * icbm.classic.content.abm.ConfigAbm.FLIGHT_SPEED, motionY * icbm.classic.content.abm.ConfigAbm.FLIGHT_SPEED, motionZ * icbm.classic.content.abm.ConfigAbm.FLIGHT_SPEED);

            //Update rotation
            float f3 = Mth.sqrt((float)(motionX * motionX + motionZ * motionZ));
            final float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
            final float pitch = (float) (Math.atan2(motionY, (double) f3) * 180.0D / Math.PI);
            entity.yRotO = yaw;
            entity.setYRot(yaw);
            entity.xRotO = pitch;
            entity.setXRot(pitch);
        }
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }
    // 1.12 declared no save nodes here either (DeadFlightLogic only persists "fuel").
}
