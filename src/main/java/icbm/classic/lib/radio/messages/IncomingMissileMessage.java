package icbm.classic.lib.radio.messages;

import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.radio.messages.IIncomingMissileMessage;
import icbm.classic.api.radio.IRadioTargetAssignable;
import lombok.Data;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Radio notification that an incoming missile is present on the channel.
 */
@Data
public class IncomingMissileMessage implements IIncomingMissileMessage {
    private final String channel;
    private final IMissile missile;
    private final boolean trigger;

    @Override
    public Vec3 getIntercept(double x, double y, double z, double velocity) {
        if(missile != null && missile.getMissileEntity() != null) {
            final Entity entity = missile.getMissileEntity();
            final Vec3 motion = entity.getDeltaMovement();
            final int maxTime = 20;
            for(int time = 1; time <= maxTime; time++) { //TODO replace with alg equation that can calculate fn(t)
                final double tX = entity.getX() + motion.x * time;
                final double tY = entity.getY() + motion.y * time;
                final double tZ = entity.getZ() + motion.z * time;

                final double mag = Math.sqrt(tX * tX + tY * tY + tZ * tZ); //TODO remove need for sqrt

                if(mag < velocity * time || time == maxTime) {
                    return missile.getVec3();
                }
            }
            return missile.getVec3();
        }
        return null;
    }

    @Override
    public Vec3 getTarget() {
        if(missile != null) {
            return missile.getVec3();
        }
        return null;
    }

    @Override
    public boolean shouldTrigger() {
        return trigger;
    }

    @Override
    public void onTriggerCallback(IRadioTargetAssignable target) {
    }
}
