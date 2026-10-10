package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 4/10/2018.
 */
@IcbmConfig(file = "debug")
public class ConfigDebug
{
    @ConfigEntry(name = "debug_threads", comment = "Enables extra console output to check the state of the explosive threads.")
    public static boolean DEBUG_THREADS = false;

    @ConfigEntry(name = "debug_explosives", comment = "Enables extra console output to check the state of the explosive logic.")
    public static boolean DEBUG_EXPLOSIVES = false;

    @ConfigEntry(name = "debug_missile_launches", comment = "When enabled, all missile launches are logged, including target and origin coordinates.")
    public static boolean DEBUG_MISSILE_LAUNCHES = false;

    @ConfigEntry(name = "debug_missile_logic", comment = "When enabled, missile logic will be logged, including flight steps and target changes.")
    public static boolean DEBUG_MISSILE_LOGIC = false;

    @ConfigEntry(name = "debug_missile_tracker", comment = "When enabled, additional debug output is written to the console regarding the missile tracker.")
    public static boolean DEBUG_MISSILE_TRACKER = false;
}
