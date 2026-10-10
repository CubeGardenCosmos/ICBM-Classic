package icbm.classic.api.radio.messages;

import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.radio.IRadioMessage;
import net.minecraft.world.entity.Entity;

/**
 * Packet containing incoming missile information from radar detection
 * <p>
 * 1.12 deferred this to S4.04 because it depends on {@code api.missiles.IMissile} (step card S1.05).
 * Now that the missiles API is ported this is a straight type migration; the launch callback comes from
 * {@link ITriggerActionMessage#onTriggerCallback(icbm.classic.api.radio.IRadioTargetAssignable)}.
 */
public interface IIncomingMissileMessage extends IRadioMessage, ITargetMessage, ITriggerActionMessage {

    IMissile getMissile();

    @Override
    default Entity getTargetEntity() {
        return getMissile().getMissileEntity();
    }
}
