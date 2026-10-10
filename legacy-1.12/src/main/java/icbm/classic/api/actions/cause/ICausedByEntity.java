package icbm.classic.api.actions.cause;

import net.minecraft.entity.Entity;

import javax.annotation.Nullable;

/**
 * Cause containing entity information
 */
public interface ICausedByEntity extends IActionCause {

    /**
     * Entity cause
     *
     * @return entity stored, or null if it didn't restore from save
     */
    @Nullable
    Entity getEntity();
}
