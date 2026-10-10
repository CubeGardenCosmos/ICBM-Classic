package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 * Client only settings
 */
@IcbmConfig(file = "client")
public class ConfigClient
{
    @ConfigEntry(name = "missile_engine_smoke", comment = "Enables engine smoke effect for missiles")
    public static boolean MISSILE_ENGINE_SMOKE = true;

    @ConfigEntry(name = "missile_launch_smoke", comment = "Enables smoke effect for launched missiles")
    public static boolean MISSILE_LAUNCH_SMOKE = true;
}
