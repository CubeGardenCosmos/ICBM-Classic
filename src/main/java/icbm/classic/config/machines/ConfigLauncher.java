package icbm.classic.config.machines;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 * Settings class for various configuration settings.
 *
 * @author Calclavia, DarkCow
 */
@IcbmConfig(file = "launcher")
public class ConfigLauncher
{
    @ConfigEntry(name = "max_range", comment = "Range of silo launcher in blocks (meters)")
    public static int RANGE = 10000;

    @ConfigEntry(name = "min_inaccuracy_range", comment = "Inaccuracy offset to apply by default (meters)")
    public static double MIN_INACCURACY = 2;

    @ConfigEntry(name = "scaled_inaccuracy_range", comment = "Inaccuracy offset to apply based on range (meters) with max range being full value. In addition to min inaccuracy already applied.")
    public static double SCALED_INACCURACY_DISTANCE = 10;

    @ConfigEntry(name = "scaled_inaccuracy_per_launcher", comment = "Inaccuracy offset to apply per launcher fired in the same circuit.")
    public static double SCALED_INACCURACY_LAUNCHERS = 1;

    @ConfigEntry(name = "power_capacity", comment = "Size of the energy buffer")
    public static int POWER_CAPACITY = 1200;

    @ConfigEntry(name = "power_per_launch", comment = "Energy consumed per launcher")
    public static int POWER_COST = 1200;
}
