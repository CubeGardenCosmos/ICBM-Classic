package icbm.classic.api.radio;

import net.minecraft.world.entity.Entity;

/**
 * Applied to objects that can be assigned a target from a radio message.
 *
 * The anti-ballistic missile implements this interface so incoming-missile messages can assign a target
 * without coupling radio messages to content-specific entity classes.
 */
public interface IRadioTargetAssignable {

    /**
     * Called to assign the entity this object should track or attack.
     *
     * @param target entity to assign, may be null to clear the current target
     */
    void assignTarget(Entity target);
}
