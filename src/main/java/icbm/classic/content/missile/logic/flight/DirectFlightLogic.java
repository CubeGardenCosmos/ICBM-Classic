package icbm.classic.content.missile.logic.flight;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.config.missile.ConfigMissile;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

/**
 * Created by Robin Seifert on 2/8/2022.
 */
public class DirectFlightLogic extends DeadFlightLogic
{
    public static final Identifier REG_NAME = ICBMConstants.id("direct");
    private double motionX; //TODO do we need to save?
    private double motionY;
    private double motionZ;

    public DirectFlightLogic() {
        super();
    }

    public DirectFlightLogic(int fuelTicks)
    {
        super(fuelTicks);
    }

    @Override
    public void calculateFlightPath(Level world, double startX, double startY, double startZ, IMissileTarget targetData)
    {
        if(targetData != null) //TODO if we have no target data have missile fly around in random directions until it hits something
        {
            motionX = targetData.getX() - startX;
            motionY = targetData.getY() - startY;
            motionZ = targetData.getZ() - startZ;

            //Normalize
            float velocity = Mth.sqrt((float)(motionX * motionX + motionY * motionY + motionZ * motionZ));
            motionX /= velocity;
            motionY /= velocity;
            motionZ /= velocity;

            //Add multiplier
            motionX *= ConfigMissile.DIRECT_FLIGHT_SPEED;
            motionY *= ConfigMissile.DIRECT_FLIGHT_SPEED;
            motionZ *= ConfigMissile.DIRECT_FLIGHT_SPEED;
        }
    }

    @Override
    public void start(Entity entity, IMissile missile)
    {
        //Set motion
        entity.setDeltaMovement(motionX, motionY, motionZ);

        //Update rotation
        float f3 = Mth.sqrt((float)(motionX * motionX + motionZ * motionZ));
        final float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        final float pitch = (float) (Math.atan2(motionY, (double) f3) * 180.0D / Math.PI);
        entity.yRotO = yaw;
        entity.setYRot(yaw);
        entity.xRotO = pitch;
        entity.setXRot(pitch);
    }

    //TODO rework to use current pitch and yaw to set motion
    //TODO update motion as long as we have fuel (ticks of motion time)

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }

    // 1.12 did not save the motion vector either, so no save/load override here (it inherited
    // DeadFlightLogic#serializeNBT which only stores "fuel"). The 1.12 "TODO do we need to save?" is kept as is.

    @Override
    public boolean equals(Object other) {
        if(other instanceof DirectFlightLogic) {
            return
                super.equals(other) //super does some checks on it's own fields
                && Math.abs(((DirectFlightLogic) other).motionX - motionX) <= 0.0001 //floating error handling
                && Math.abs(((DirectFlightLogic) other).motionY - motionY) <= 0.0001
                && Math.abs(((DirectFlightLogic) other).motionZ - motionZ) <= 0.0001;
        }
        return false;
    }

}
