package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;
import icbm.classic.config.core.IcbmConfig;

/**
 * Settings class for various configuration settings.
 *
 * @author Calclavia, DarkCow
 */
@IcbmConfig(file = "main")
public class ConfigMain
{
    @ConfigEntry(name = "use_energy", comment = "Range of tier 1 launcher")
    public static boolean REQUIRES_POWER = true;



    @ConfigEntry(name = "protectiveArmor", comment = "Settings for setting protection support on armor items")
    public static ProtectiveArmorConfig protectiveArmor = new ProtectiveArmorConfig();

    public static class ProtectiveArmorConfig {

        @ConfigEntry(name = "min_chemical_gas_protection", comment = "Minimal percentage of protection required to start guarding from chemical attacks")
        @ConfigRange(min = 0, max = 1)
        public float minProtectionChemicalGas = 0.5f;

        @ConfigEntry(name = "min_viral_gas_protection", comment = "Minimal percentage of protection required to start guarding from viral attacks")
        @ConfigRange(min = 0, max = 1)
        public float minProtectionViralGas = 0.8f;

        @ConfigEntry(name = "min_radiation_protection", comment = "Minimal percentage of protection required to start guarding from radiation attacks")
        @ConfigRange(min = 0, max = 1)
        public float minProtectionRadiation = 1f;

        @ConfigEntry(name = "min_debilitation_gas_protection", comment = "Minimal percentage of protection required to start guarding from debilitation attacks")
        @ConfigRange(min = 0, max = 1)
        public float minProtectionDebilitationGas = 0.1f;

        @ConfigEntry(name = "require_helmet", comment = "Is head slot required to count as protection")
        public boolean requireHelmet = true;

        //TODO once on latest MC rethinking gas protection to be per weapon system.
        @ConfigEntry(name = "item_ratings", comment = "Item/ItemStack to percentage protection between 0.0 to 1.0 'domain:resource=floating_point', ex: 'minecraft:iron_helmet=0.02'")
        public String[] ITEMS = new String[]{};
    }
}
