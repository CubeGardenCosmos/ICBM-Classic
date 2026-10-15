package icbm.classic.content.abm;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

@IcbmConfig(file = "abm")
public class ConfigAbm
{
    public static final float FLIGHT_SPEED = 4.0F;
    @ConfigEntry(name = "proximity_radius", comment = "Distance at which an ABM proximity fuse resolves")
    public static double PROXIMITY_RADIUS = 2.5;
    @ConfigEntry(name = "hit_chance", comment = "Chance for an ABM proximity fuse to destroy its target")
    public static double HIT_CHANCE = 1.0;
    @ConfigEntry(name = "target_warhead_on_kill", comment = "Whether an intercepted target missile triggers its payload")
    public static boolean TARGET_WARHEAD_ON_KILL = false;
    @ConfigEntry(name = "kill_effect_power", comment = "Visual explosion power when an ABM intercepts a target")
    public static float KILL_EFFECT_POWER = 1.5F;
    @ConfigEntry(name = "use_missile_target_data", comment = "Use missile target data when predicting impact")
    public static boolean USE_MISSILE_TARGET_DATA = true;

    @ConfigEntry(name = "prediction_max_ticks", comment = "Maximum number of ticks to extrapolate a missile trajectory")
    public static int PREDICTION_MAX_TICKS = 600;
}
