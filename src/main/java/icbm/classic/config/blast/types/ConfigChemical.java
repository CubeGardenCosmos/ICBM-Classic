package icbm.classic.config.blast.types;

import icbm.classic.ICBMConstants;
import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigChemical {

    @ConfigEntry(name = "size", comment = "Size (meters) of the gas cloud")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int size = 20;

    @ConfigEntry(name = "duration", comment = "Duration (ticks) of the gas cloud")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int duration = ICBMConstants.TICKS_SEC * 30;

    @ConfigEntry(name = "toxicity_start", comment = "Minimal toxicity built up on an entity before starting chemical damage. User gets 4 points per second (1 per 5 server ticks)")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int toxicityBuildup = 20;

    @ConfigEntry(name = "toxicity_scale", comment = "Scale to use for calculating damage once toxicity starts. Damage is applied every 5 ticks. damage = toxicity * scale")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float toxicityScale = 0.1f;

    @ConfigEntry(name = "toxicity_min_damage", comment = "Minimal damage to apply once toxicity starts")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float toxicityMinDamage = 1f;
}
