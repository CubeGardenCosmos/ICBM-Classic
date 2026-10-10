package icbm.classic.lib.actions.status;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import net.minecraft.resources.Identifier;

public class ActionResponses {
    // <editor-fold description="error status">
    /** Unexpected errors that don't align with any specific type... usually lazy dev using try-catch */
    public static IActionStatus UNKNOWN_ERROR = ImmutableStatus.error(ICBMConstants.id("action.error.unknown"));

    /** Spawning the entity failed. Could be due to chunk unloaded, join world event, or other unknown reason */
    public static IActionStatus ENTITY_SPAWN_FAILED = ImmutableStatus.error(ICBMConstants.id("action.entity.spawning.failed"));

    /** Canceled because an explosion start event was canceled */
    public static IActionStatus EXPLOSION_CANCELED = ImmutableStatus.blocking(ICBMConstants.id("action.explosion.canceled"));

    /** Generic validation issue with action state */
    public static IActionStatus VALIDATION_ERROR = ImmutableStatus.blocking(ICBMConstants.id("action.validation.failed"));

    public static IActionStatus BLOCK_PLACEMENT_FAILED = ImmutableStatus.blocking(ICBMConstants.id("action.block.placement.failed"));

    public static IActionStatus MISSING_WORLD = new MissingFieldStatus(ICBMConstants.id("action.error.field.world.missing"), "action", "world");
    public static IActionStatus MISSING_HOST = new MissingFieldStatus(ICBMConstants.id("action.error.field.host.missing"), "action", "host");
    public static IActionStatus MISSING_BLOCK_POS = new MissingFieldStatus(ICBMConstants.id("action.error.field.block.pos.missing"), "action", "blockPos");
    public static IActionStatus MISSING_BLOCK_STATE = new MissingFieldStatus(ICBMConstants.id("action.error.field.block.state.missing"), "action", "blockState");
    public static IActionStatus MISSING_VALIDATION = new MissingFieldStatus(ICBMConstants.id("action.error.field.validation.missing"), "action", "validation");

    public static IActionStatus MISSING_ACTION_DATA = new MissingFieldStatus(ICBMConstants.id("action.error.field.action.data"), "runner", "actionData");
    // </editor-fold>

    // <editor-fold description="good status">
    public static IActionStatus COMPLETED = ImmutableStatus.create(ICBMConstants.id("action.completed"), ActionStatusTypes.DONE);
    public static IActionStatus READY = ImmutableStatus.create(ICBMConstants.id("action.ready"), ActionStatusTypes.READY);
    public static IActionStatus WORKING = ImmutableStatus.create(ICBMConstants.id("action.working"), ActionStatusTypes.BLOCKING, ActionStatusTypes.WAITING);
    public static IActionStatus WAITING = ImmutableStatus.create(ICBMConstants.id("action.waiting"), ActionStatusTypes.BLOCKING, ActionStatusTypes.WAITING);
    // </editor-fold>

    public static void registerTypes() {

        UNKNOWN_ERROR.register();
        ENTITY_SPAWN_FAILED.register();
        EXPLOSION_CANCELED.register();
        VALIDATION_ERROR.register();
        BLOCK_PLACEMENT_FAILED.register();
        MISSING_WORLD.register();
        MISSING_HOST.register();
        MISSING_BLOCK_POS.register();
        MISSING_BLOCK_STATE.register();
        MISSING_VALIDATION.register();
        COMPLETED.register();
        READY.register();
        WORKING.register();
        WAITING.register();
    }
}
