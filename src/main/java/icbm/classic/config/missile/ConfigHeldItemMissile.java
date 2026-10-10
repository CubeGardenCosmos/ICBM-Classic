package icbm.classic.config.missile;

import icbm.classic.config.core.ConfigEntry;

/**
 * Config for any item that deploys an entity that acts as a holder of cargo (balloon, parachute)
 */
public class ConfigHeldItemMissile
{
    @ConfigEntry(name = "item_ban_allow", comment = "Items allowed for insertion into held-item missile as a projectile payload") // TODO rename to `items`
    public BanList BAN_ALLOW = new BanList();

    @ConfigEntry(name = "block_damage_multiplier", comment = "Multiplier to apply to missile's held item's block break speed.")
    public float BLOCK_DAMAGE_MULTIPLIER = 4;

    public static class BanList {

        @ConfigEntry(name = "ban", comment = "Set to true to ban all blocks contained. False to use as allow list") //TODO rename to `invert`
        public boolean BAN = true;

        @ConfigEntry(name = "list", comment = "Item/ItemStack names 'domain:resource=ignore' set 'ignore' to true to skip the entry during loading; ex: 'minecraft:stone' will be used while 'minecraft:fire=true' will be ignored")
        public String[] ITEMS = new String[]{};
    }
}
