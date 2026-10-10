package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigExBasics {

    @ConfigEntry(name = "scale", comment = "Scale of the explosive; This isn't always max size but a multiplier used in calculations.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double scale;

    public ConfigExBasics(double scale) {
        this.scale = scale;
    }
}
