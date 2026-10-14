package icbm.classic.content.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.actions.status.ImmutableStatus;

@Deprecated
public class BlastStatus {
    public static ImmutableStatus SETUP_ERROR = ImmutableStatus.error(ICBMConstants.id("blast.setup.error"));
    public static ImmutableStatus TRIGGERED_THREADING = ImmutableStatus.blocking(ICBMConstants.id("blast.threading"));
}
