package icbm.classic.config.blocks;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigRadioactive {

    @ConfigEntry(name = "random_world_tick", comment = "If true will use the gamerule 'randomTickSpeed' ignoring 'decay_delay'. Resulting in a far slower but less performance impacting tick.")
    public boolean randomTickOnly = false;

    @ConfigEntry(name = "decay_delay", comment = "Delay in ticks (20 ticks per second) between running decay chance calculation and effects. Set to zero to disable, if previously zero requires world restart for old blocks to tick.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int decayDelay = 5;

    @ConfigEntry(name = "decay_block_chance", comment = "Chance (random < value) for radioactive block to convert into harmless blocks, EX: Radioactive Stone -> Stone. Set to zero to disable. If previously zero blocks may not restart ticking.")
    @ConfigRange(min = 0, max = 1)
    public float decayBlockChance = 0.0001f;

    @ConfigEntry(name = "decay_effect_chance", comment = "Chance (random < value) for radioactive decay effects to be triggered per entity")
    @ConfigRange(min = 0, max = 1)
    public float decayEffectChance = 0.85f;

    @ConfigEntry(name = "decay_effect_range", comment = "Range (meters) for effects to be applied")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int decayEffectRange = 5;


    @ConfigEntry(name = "decay_effect_damage", comment = "Damage to apply to entities caught in decay range")
    @ConfigRange(min = 1, max = Double.MAX_VALUE)
    public float decayEffectDamage = 2;

    @ConfigEntry(name = "decay_effect_wither_duration", comment = "Time in ticks (20 ticks per second) to apply withering effect. Set to zero to disable")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int decayWitherDuration = 20;

}
