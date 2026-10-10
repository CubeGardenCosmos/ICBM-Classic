package icbm.classic.config.machines;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 * Settings class for various configuration settings.
 *
 * @author Calclavia, DarkCow
 */
@IcbmConfig(file = "emp_tower")
public class ConfigEmpTower {
    @ConfigEntry(name = "extender_range_bonus", comment = "Bonus max range (meters) given for each extender added above the base")
    public static int BONUS_RADIUS = 20;

    @ConfigEntry(name = "firing_cost_area", comment = "Scaling energy cost (FE, ForgeEnergy) per meter^2 covered. Math: (range * range) * cost")
    public static int ENERGY_COST_AREA = 100;

    @ConfigEntry(name = "energy_upkeep", comment = "Scaling energy cost (FE, ForgeEnergy) to keep the emp tower charged. Math: range * cost")
    public static int ENERGY_COST_TICKING = 10;

    @ConfigEntry(name = "energy_upkeep_capacity", comment = "Ticks (20 ticks a second) of energy to store to cover ticking cost")
    public static int ENERGY_COST_TICKING_CAP = 100;

    @ConfigEntry(name = "firing_cooling_ticks", comment = "Time in ticks (20 ticks a second) to wait before recharging, after firing.")
    public static int COOLDOWN = 10 * 20;

    @ConfigEntry(name = "energy_receive_limit", comment = "Input energy limit (FE, ForgeEnergy) per energy received action (can receive several actions per tick).")
    public static int ENERGY_INPUT = 10000;

    @ConfigEntry(name = "base_max_range", comment = "Max user configurable range of base emp tower (meters)")
    public static int MAX_BASE_RANGE = 150;
}
