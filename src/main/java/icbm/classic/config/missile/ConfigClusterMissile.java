package icbm.classic.config.missile;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

/**
 * Config for Surface to Air missiles
 */
public class ConfigClusterMissile
{
    @ConfigEntry(name = "health", comment = "Amount of damage a missile can take from any source before death")
    @ConfigRange(min = 0.0001, max = 10)
    public float MAX_HEALTH = 100;

    @ConfigEntry(name = "item_ban_allow", comment = "Items allowed for insertion into cluster missile as a projectile payload")
    public BanList BAN_ALLOW = new BanList();

    @ConfigEntry(name = "item_sizes", comment = "Items volume size for insertion into cluster missiles")
    public ItemSizes ITEM_SIZES = new ItemSizes();

    public static class ItemSizes {

        @ConfigEntry(name = "max_size", comment = "Volume of the cluster missile, most items default to size of 1. Keep it small to avoid lag")
        @ConfigRange(min = 1, max = Integer.MAX_VALUE)
        public int MAX_SIZE = 200;

        @ConfigEntry(name = "default_size", comment = "Default volume to use for items if no configuration exists")
        @ConfigRange(min = 1, max = Integer.MAX_VALUE)
        public int DEFAULT_SIZE = 1;

        @ConfigEntry(name = "list", comment = "Item/ItemStack sizes 'domain:resource=whole_number', ex: 'minecraft:stone=4'")
        public String[] ITEMS = new String[]{};
    }

    public static class BanList {

        @ConfigEntry(name = "ban", comment = "Set to true to ban all blocks contained. False to use as allow list")
        public boolean BAN = true;

        @ConfigEntry(name = "list", comment = "Item/ItemStack names 'domain:resource=ignore' set 'ignore' to true to skip the entry during loading; ex: 'minecraft:stone' will be used while 'minecraft:fire=true' will be ignored")
        public String[] ITEMS = new String[]{};
    }

}
