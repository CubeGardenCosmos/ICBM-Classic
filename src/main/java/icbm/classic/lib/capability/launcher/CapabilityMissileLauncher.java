package icbm.classic.lib.capability.launcher;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.lib.actions.status.ActionResponses;

import org.jspecify.annotations.Nullable;
import java.util.UUID;

/**
 * Default {@link IMissileLauncher} implementation that reports a generic error for every call.
 * <p>
 * 1.12 returned {@code LauncherStatus.ERROR_GENERIC} (a content class in
 * {@code content/blocks/launcher/status}, ported in S5.02/§S5); this default now returns the API level
 * {@link ActionResponses#UNKNOWN_ERROR} so the lib does not depend on launcher content.
 * <p>
 * The class no longer registers a Forge capability - a launcher block entity implements
 * {@link IMissileLauncher} itself and registers with {@link icbm.classic.lib.lookup.IcbmLookups#MISSILE_LAUNCHER}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class CapabilityMissileLauncher implements IMissileLauncher
{
    @Override
    public UUID getUniqueId() {
        return null;
    }

    @Override
    public IActionStatus getStatus() {
        // TODO(S5.02) return LauncherStatus.ERROR_GENERIC once the launcher content status class is ported
        return ActionResponses.UNKNOWN_ERROR;
    }

    @Override
    public IActionStatus preCheckLaunch(IMissileTarget target, @Nullable IActionCause cause) {
        return ActionResponses.UNKNOWN_ERROR;
    }

    @Override
    public IActionStatus launch(ILauncherSolution firingSolution, @Nullable IActionCause cause, boolean simulate) {
        return ActionResponses.UNKNOWN_ERROR;
    }
}
