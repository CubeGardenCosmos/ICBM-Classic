package icbm.classic.config.blocks;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigSpikes {

    @ConfigEntry(name = "normal_spike_damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public float normalDamage = 1;

    @ConfigEntry(name = "poison_spike_damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public float poisonDamage = 1;

    @ConfigEntry(name = "fire_spike_damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public float fireDamage = 1;
}
