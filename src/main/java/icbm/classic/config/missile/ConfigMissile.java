package icbm.classic.config.missile;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;
import icbm.classic.config.core.IcbmConfig;

/**
 * Configs for Missiles
 *
 * Created by Dark(DarkGuardsman, Robin) on 2/28/2018.
 */
@IcbmConfig(file = "missile")
public class ConfigMissile
{
    /**  */
    @ConfigEntry(name = "speed_direct_fire", comment = "Speed (meters per tick) limiter of the missile when fired directly without a target")
    @ConfigRange(min = 0.0001, max = 10)
    public static float DIRECT_FLIGHT_SPEED = 2;

    @ConfigEntry(name = "simulation_flight_speed", comment = "Speed (meters per tick) of missiles in simulation")
    @ConfigRange(min = 0.0001, max = Double.MAX_VALUE)
    public static float SIMULATION_FLIGHT_SPEED = 10;

    @ConfigEntry(name = "simulation_exit_height", comment = "Height (y level) to start simulating a missile when it travels above the map")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int SIMULATION_EXIT_HEIGHT = 500;

    @ConfigEntry(name = "simulation_enter_height", comment = "Height (y level) to spawn missiles when they enter the map")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int SIMULATION_ENTER_HEIGHT = 450;

    @ConfigEntry(name = "arc_distance_limit", comment = "Distance, ignoring y, to allow flying in an arc towards target. Over distance will fly strait up for better performance.")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public static int ARC_DISTANCE_LIMIT = 500;

    @ConfigEntry(name = "simulation_enter_speed", comment = "Speed (meters per tick) to spawn the missile")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public static float SIMULATION_ENTER_SPEED = 1;

    @ConfigEntry(name = "cruise_fuel", comment = "Fuel (ticks) before a missile fired from a cruise launcher starts to fall out of the air")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public static int CRUISE_FUEL = 200;

    @ConfigEntry(name = "handheld_fuel", comment = "Fuel (ticks) before a missile fired from a handheld launcher starts to fall out of the air")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public static int HANDHELD_FUEL = 200;

    @ConfigEntry(name = "health_tier_1", comment = "Amount of damage a missile can take from any source before death")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int TIER_1_HEALTH = 50;

    @ConfigEntry(name = "health_tier_2", comment = "Amount of damage a missile can take from any source before death")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int TIER_2_HEALTH = 80;

    @ConfigEntry(name = "health_tier_3", comment = "Amount of damage a missile can take from any source before death")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int TIER_3_HEALTH = 100;

    @ConfigEntry(name = "health_tier_4", comment = "Amount of damage a missile can take from any source before death")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public static int TIER_4_HEALTH = 200;

    @ConfigEntry(name = "impact_damage_limit", comment = "Max amount of damage a missile can apply to a target on impact. Set to -1 to have no limit. Set to 0 to disable")
    @ConfigRange(min = -1, max = Integer.MAX_VALUE)
    public static int DAMAGE_LIMIT = -1;

    @ConfigEntry(name = "impact_damage_scale", comment = "scale * velocity = damage. RPG fired missiles often impact with a velocity of 2")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public static float DAMAGE_SCALE = 20f;

    @ConfigEntry(name = "SAM_MISSILE")
    public static ConfigSAMMissile SAM_MISSILE = new ConfigSAMMissile();

    @ConfigEntry(name = "bomblet")
    public static ConfigBomblet bomblet = new ConfigBomblet();

    @ConfigEntry(name = "cluster", comment = "https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-cluster")
    public static ConfigClusterMissile CLUSTER_MISSILE = new ConfigClusterMissile();

    @ConfigEntry(name = "held_item", comment = "https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/Config-Cargo-Holders")
    public static ConfigHeldItemMissile HELD_ITEM_MISSILE = new ConfigHeldItemMissile();

    @ConfigEntry(name = "cargo_holder", comment = "https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/Config-Cargo-Holders")
    public static ConfigCargoItem CARGO_HOLDERS = new ConfigCargoItem();
}
