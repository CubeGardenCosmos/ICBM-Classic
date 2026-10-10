package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

@IcbmConfig(file = "flying_blocks")
public class ConfigFlyingBlocks
{
    @ConfigEntry(name = "enable", comment = "Enables flying blocks, set to false to prevent additional usage in blasts. Doesn't remove existing or prevent other mods from spawning more.")
    public static boolean enabled = true;

    //@Config.Name("block_replacements") TODO implement
    //@Config.Comment("Replacements to use, Format: 'blockStateA | blockStateB' Docs: https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-block-states")
    //public String[] replacements = new String[]{"minecraft:water | minecraft:ice"};

    @ConfigEntry(name = "block_ban_allow")
    public static BanList banAllow = new BanList();

    @ConfigEntry(name = "damage_scale", comment = "Damage to apply, scaled by velocity")
    public static float damageScale = 2f;

    public static class BanList {

        @ConfigEntry(name = "ban", comment = "Set to true to ban all blocks contained. False to use as allow list")
        public boolean ban = true;

        @ConfigEntry(name = "list", comment = "Block/BlastState names. Docs: https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-block-states")
        public String[] blockStates = new String[]{"minecraft:fire"};
    }
}
