package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;
import icbm.classic.config.core.IcbmConfig;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 2/22/2018.
 */
@IcbmConfig(file = "battery")
public class ConfigBattery
{
    @ConfigEntry(name = "battery_tier_1_capacity", comment = "Amount of energy the battery can store")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int BATTERY_CAPACITY = 100000;

    @ConfigEntry(name = "battery_tier_1_input", comment = "Transfer limit into the battery")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int BATTERY_INPUT_LIMIT = 10000;

    @ConfigEntry(name = "battery_tier_1_output", comment = "Transfer limit out of the battery")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int BATTERY_OUTPUT_LIMIT = 10000;
}
