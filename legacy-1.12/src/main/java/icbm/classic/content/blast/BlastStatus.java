package icbm.classic.content.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.actions.status.ImmutableStatus;
import net.minecraft.util.ResourceLocation;

@Deprecated
public class BlastStatus {
    public static ImmutableStatus SETUP_ERROR = ImmutableStatus.error(new ResourceLocation(ICBMConstants.DOMAIN, "blast.setup.error"));
    public static ImmutableStatus TRIGGERED_THREADING = ImmutableStatus.blocking(new ResourceLocation(ICBMConstants.DOMAIN, "blast.threading"));
}
