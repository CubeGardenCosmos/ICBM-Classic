package icbm.classic.content.abm;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

@IcbmConfig(file = "abm")
public class ConfigAbm
{
    @ConfigEntry(name = "use_missile_target_data", comment = "Use missile target data when predicting impact")
    public static boolean USE_MISSILE_TARGET_DATA = true;

    @ConfigEntry(name = "prediction_max_ticks", comment = "Maximum number of ticks to extrapolate a missile trajectory")
    public static int PREDICTION_MAX_TICKS = 600;
}
