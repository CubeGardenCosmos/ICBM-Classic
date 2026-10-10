package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigAntimatter {

    @ConfigEntry(name = "break_unbreakable", comment = "Should antimatter ignore hardness checks for unbreakable blocks (bedrock, wards)")
    public boolean damageUnbreakable = true;

    @ConfigEntry(name = "damage_on_redmatter_kill", comment = "Whether or not antimatter damages blocks/entities when detonating and killing redmatter")
    public boolean damageOnRedmatterKill = false;

    @ConfigEntry(name = "size", comment = "Size (meters) of the blast")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int size = 55;

    @ConfigEntry(name = "damage", comment = "Damage to apply to entities. Is applied at start and end of blast.")
    @ConfigRange(min = 1, max = Double.MAX_VALUE)
    public float damage = Integer.MAX_VALUE;
}
