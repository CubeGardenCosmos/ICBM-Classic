package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigNuclear {

    @ConfigEntry(name = "scale", comment = "Scale of the explosive; This isn't always max size but a multiplier used in calculations.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double scale = 50;

    @ConfigEntry(name = "rotScale", comment = "Scale of the block rot blast. Handles radioactive blocks and destroying plants")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double rotScale = 80;

    @ConfigEntry(name = "mutationScale", comment = "Scale of the entity mutation blast")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double mutationScale = 80;

    @ConfigEntry(name = "entityDamageScale", comment = "Scale of the entity damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double entityDamageScale = 50;

    @ConfigEntry(name = "energy", comment = "Energy scale used for breaking blocks and doing entity damage")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double energy = 80;

    @ConfigEntry(name = "entityDamageMultiplier", comment = "Multiplier to apply to energy before scaling blast damage to entities. Damage still scales by distance and other factors.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double entityDamageMultiplier = 1000;

    @ConfigEntry(name = "radioactive_replacements")
    public BlockReplacements radiationReplacements = new BlockReplacements();

    public static class BlockReplacements {

        @ConfigEntry(name = "list", comment = "Sets which block to use when replacing a block during a radiation event Docs: https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-block-states")
        public String[] blockStates = new String[]{};
    }
}
