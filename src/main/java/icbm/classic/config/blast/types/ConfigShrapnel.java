package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigShrapnel {

    @ConfigEntry(name = "fragments", comment = "Amount of rotation in yaw and pitch to spawn fragments. total = fragments * fragments")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int fragments = 30;

    @ConfigEntry(name = "damage", comment = "Damage to apply to entities")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int damage = 11;
}
