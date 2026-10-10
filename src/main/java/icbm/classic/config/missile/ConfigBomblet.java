package icbm.classic.config.missile;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigBomblet {

    @ConfigEntry(name = "impact_damage", comment = "Damage applied on direct impact, scaled by velocity")
    @ConfigRange(min = 1, max = Double.MAX_VALUE)
    public float impactDamage = 3F;

    @ConfigEntry(name = "health", comment = "Hearts of damage before bomblet is destroyed")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int health = 5;
}
