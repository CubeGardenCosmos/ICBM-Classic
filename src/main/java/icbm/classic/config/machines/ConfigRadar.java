package icbm.classic.config.machines;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

@IcbmConfig(file = "radar")
public class ConfigRadar
{
    @ConfigEntry(name = "max_range", comment = "Max range of radar in blocks (meters) to detect entities")
    public static int MAX_RANGE = 500;

    @ConfigEntry(name = "power_capacity", comment = "Size of the energy buffer")
    public static int POWER_CAPACITY = 20_000;

    @ConfigEntry(name = "power_cost", comment = "Energy consumed per tick")
    public static int POWER_COST = 1_000;

    @ConfigEntry(name = "scan_rate", comment = "Delay in ticks before scanning for entities")
    public static int SCAN_TICKS = 1;

    @ConfigEntry(name = "sam_delay", comment = "Delay in ticks before triggering more SAM launches")
    public static int SAM_TICKS = 40;
}
