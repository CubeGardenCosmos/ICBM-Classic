package icbm.classic.content.blocks.launcher.status;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.lib.actions.status.ActionStatus;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import net.minecraft.resources.Identifier;

public final class LauncherStatus {

    public static final ActionStatus ERROR_GENERIC = status("launcher.error.generic", LauncherLangs.ERROR, true, false);
    public static final ActionStatus ERROR_SPAWN = status("launcher.error.spawning", LauncherLangs.ERROR_MISSILE_SPAWNING, true, false);
    public static final ActionStatus ERROR_SPAWN_COLLIDER = status("launcher.error.spawning.space", LauncherLangs.ERROR_MISSILE_SPACE, true, false);
    public static final ActionStatus ERROR_MIN_RANGE = status("launcher.error.range.min", LauncherLangs.ERROR_TARGET_MIN, true, false);
    public static final ActionStatus ERROR_MAX_RANGE = status("launcher.error.range.max", LauncherLangs.ERROR_TARGET_MAX, true, false);
    public static final ActionStatus ERROR_TARGET_NULL = status("launcher.error.target.null", LauncherLangs.ERROR_TARGET_NONE, true, false);
    public static final ActionStatus ERROR_POWER = status("launcher.error.power", LauncherLangs.ERROR_NO_POWER, true, false);
    public static final ActionStatus ERROR_INVALID_STACK = status("launcher.error.missile.invalid", LauncherLangs.ERROR_MISSILE_INVALID, true, false);
    public static final ActionStatus ERROR_EMPTY_STACK = status("launcher.error.missile.empty", LauncherLangs.ERROR_MISSILE_NONE, true, false);
    public static final ActionStatus ERROR_QUEUED = status("launcher.error.missile.queued", LauncherLangs.ERROR_MISSILE_QUEUED, true, false);
    public static final ActionStatus ERROR_EMPTY_GROUP = status("launcher.error.group.empty", LauncherLangs.ERROR_GROUP_EMPTY, true, false);
    public static final ActionStatus ERROR_NO_NETWORK = status("launcher.error.network.none", LauncherLangs.ERROR_NO_NETWORK, true, false);

    public static final ActionStatus READY = status("launcher.ready", LauncherLangs.STATUS_READY, false, false);
    public static final ActionStatus LAUNCHED = status("launcher.launched", LauncherLangs.STATUS_LAUNCHED, false, false);
    public static final ActionStatus CANCELED = status("launcher.canceled", LauncherLangs.STATUS_CANCELED, false, false);
    public static final ActionStatus FIRING_AIMING = status("launcher.firing.aiming", LauncherLangs.STATUS_FIRING_AIMING, false, true);

    private LauncherStatus() {
    }

    private static ActionStatus status(String id, String translation, boolean error, boolean blocking) {
        final ActionStatus status = new ActionStatus().withRegName(id).withTranslation(translation);
        if (error) {
            status.asError();
        }
        if (blocking) {
            status.asBlocking();
        }
        return status;
    }

    public static void registerTypes() {
        register(ERROR_GENERIC);
        register(ERROR_SPAWN);
        register(ERROR_SPAWN_COLLIDER);
        register(ERROR_MIN_RANGE);
        register(ERROR_MAX_RANGE);
        register(ERROR_TARGET_NULL);
        register(ERROR_POWER);
        register(ERROR_INVALID_STACK);
        register(ERROR_EMPTY_STACK);
        register(ERROR_QUEUED);
        register(LAUNCHED);
        register(READY);
        register(CANCELED);
        register(FIRING_AIMING);
        ICBMClassicAPI.ACTION_STATUS_REGISTRY.register(FiringWithDelay.REG_NAME, FiringWithDelay::new);
        ICBMClassicAPI.ACTION_STATUS_REGISTRY.register(LaunchedWithMissile.REG_NAME, LaunchedWithMissile::new);
    }

    private static void register(ActionStatus status) {
        final Identifier key = status.getRegistryKey();
        ICBMClassicAPI.ACTION_STATUS_REGISTRY.register(key, () -> status);
    }
}
