package icbm.classic.config.blast;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigThermobaric {

    @ConfigEntry(name = "scale", comment = "Scale of the explosive; This isn't always max size but a multiplier used in calculations.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double scale = 30;

    @ConfigEntry(name = "entityDamageScale", comment = "Scale of the entity damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double entityDamageScale = 50;

    @ConfigEntry(name = "energy", comment = "Energy scale used for breaking blocks and doing entity damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double energy = 80;

    @ConfigEntry(name = "entityDamageMultiplier", comment = "Multiplier to apply to energy before scaling blast damage to entities. Damage still scales by distance and other factors.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double entityDamageMultiplier = 1000;
}
