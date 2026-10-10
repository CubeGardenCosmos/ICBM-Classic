package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigFragment {

    @ConfigEntry(name = "fragments", comment = "Amount of rotation in yaw and pitch to spawn fragments. total = fragments * fragments")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int fragments = 15;

    @ConfigEntry(name = "damage", comment = "Damage to apply to entities")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int damage = 11;

    @ConfigEntry(name = "explosive_energy", comment = "Energy of TNT explosion triggered on impact")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float explosionSize = 1.5f;
}
