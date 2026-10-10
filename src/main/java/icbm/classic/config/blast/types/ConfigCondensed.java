package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigCondensed {

    @ConfigEntry(name = "energy_scale", comment = "Amount of energy to start the explosion. Higher values destroy more blocks but also increases lag. Values over 10 will start to see skipped blocks due vanilla's TNT logic.")
    @ConfigRange(min = 1, max = Double.MAX_VALUE)
    public double energyScale = 6;

    @ConfigEntry(name = "damage", comment = "Damage to apply to entities")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int damage = 10;
}
