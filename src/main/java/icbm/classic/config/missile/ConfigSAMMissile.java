package icbm.classic.config.missile;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

/**
 * Config for Surface to Air missiles
 */
public class ConfigSAMMissile
{
    @ConfigEntry(name = "speed", comment = "Speed (meters per tick) limiter of the missile")
    @ConfigRange(min = 0.0001, max = 10)
    public float FLIGHT_SPEED = 4;

    @ConfigEntry(name = "use_radar_only", comment = "Forces seeker to only use radar data for better performance. See wiki for supported radar entities. Default is AABB scanning using world entity data.")
    public boolean RADAR_MAP_ONLY = false;

    @ConfigEntry(name = "target_range", comment = "Range (meters) to limit scanning for new targets. Higher values will cause lag if using AABB scanning. Set use_radar_only=true to disable AABB scanning.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public int TARGET_RANGE = 30;

    @ConfigEntry(name = "fuel", comment = "Fuel (ticks) before a missile starts to fall out of the air")
    @ConfigRange(min = 0, max = Integer.MAX_VALUE)
    public int FUEL = 200;

    @ConfigEntry(name = "attack_damage", comment = "Damage (hearts) to apply to the target on intercept")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float ATTACK_DAMAGE = 100;
}
