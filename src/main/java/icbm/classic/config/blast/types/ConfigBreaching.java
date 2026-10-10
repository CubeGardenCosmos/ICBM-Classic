package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigBreaching {

    @ConfigEntry(name = "depth", comment = "How deep (meters) the breaching charge go before zeroing out energy left.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int depth = 7;

    @ConfigEntry(name = "size", comment = "Distance (meters) from center to create edge of hole. Hole will be (s + 1 + s) width & height. So for a value of 1 this will make a 3x3 hole.")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int size = 1;

    @ConfigEntry(name = "damage", comment = "Damage to apply to entities")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int damage = 13;

    @ConfigEntry(name = "energy", comment = "Explosive energy to start each blast depth line.")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float energy = 15376; //energy needed to break several layers of high tier concrete

    @ConfigEntry(name = "energy_scale_distance", comment = "Scale factor to reduce starting energy for each block away from center. e = e_start - (e_start * (w + h) * scale)")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float energyDistanceScale = 0.25f;

    @ConfigEntry(name = "energy_cost_distance", comment = "Energy cost percentage for traveling through a block with no explosive resistance")
    @ConfigRange(min = 0, max = 1)
    public float energyCostDistance = 1 - 0.65f;
}
