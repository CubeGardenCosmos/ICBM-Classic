package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/20/2018.
 */
@IcbmConfig(file = "item")
public class ConfigItems
{
    @ConfigEntry(name = "enable_crafting_items", comment = "Enables crafting items required to make most of the content. Only disable if other mods provide the resources or custom recipes are implemented.")
    public static boolean ENABLE_CRAFTING_ITEMS = true;

    @ConfigEntry(name = "enable_circuits")
    public static boolean ENABLE_CIRCUIT_ITEMS = true;

    @ConfigEntry(name = "enable_ingots")
    public static boolean ENABLE_INGOTS_ITEMS = true;

    @ConfigEntry(name = "enable_plates")
    public static boolean ENABLE_PLATES_ITEMS = true;

    @ConfigEntry(name = "enable_wires")
    public static boolean ENABLE_WIRES_ITEMS = true;

    @ConfigEntry(name = "enable_battery")
    public static boolean ENABLE_BATTERY = true;

    @ConfigEntry(name = "enable_sulfur_drops", comment = "Enables dropping sulfur from creepers and blazes. Use a loot table mod to change the drop rates.")
    public static boolean ENABLE_SULFUR_LOOT_DROPS = true;

    @ConfigEntry(name = "enable_loot_drops", comment = "Enables finding crafting items inside of chests as loot. Use a loot table mod to change the drop rates.")
    public static boolean ENABLE_LOOT_DROPS = true;
}
