package icbm.classic.api.radio;

import net.minecraft.world.entity.Entity;

/**
 * Applied to objects that can be assigned a target from a radio message.
 *
 * In 1.12 the surface to air missile ({@code EntitySurfaceToAirMissile}) was directly referenced
 * from {@code IncomingMissileMessage#onTriggerCallback}. 26.3 breaks that content dependency by
 * having the anti-missile missile implement this interface and assigning the target through it
 * (see step card S1.05; the SAM itself is ported in S4.04).
 */
public interface IRadioTargetAssignable {

    /**
     * Called to assign the entity this object should track or attack.
     *
     * @param target entity to assign, may be null to clear the current target
     */
    void assignTarget(Entity target);
}
