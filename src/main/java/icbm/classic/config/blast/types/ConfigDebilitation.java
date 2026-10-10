package icbm.classic.config.blast.types;

import icbm.classic.ICBMConstants;
import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigDebilitation {

    @ConfigEntry(name = "size", comment = "Size (meters) of the gas cloud")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int size = 20;

    @ConfigEntry(name = "duration", comment = "Duration (ticks) of the gas cloud")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int duration = ICBMConstants.TICKS_SEC * 30;
}
