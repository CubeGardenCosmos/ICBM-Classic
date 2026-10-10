package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

/**
 * Created by Dark(DarkGuardsman, Robin) on 2/9/2020.
 */
public class ConfigRedmatter
{
    @ConfigEntry(name = "damage", comment = "Damage to apply to entities being crushed by redmatter center")
    @ConfigRange(min = 0, max = Double.MAX_VALUE)
    public float damage = 2000;

    @ConfigEntry(name = "redmatter_max_size", comment = "Largest size the redmatter can grow into before stopping.")
    public float MAX_SIZE = 70;

    @ConfigEntry(name = "redmatter_default_size", comment = "Default spawning size for blocks/missiles/carts/etc")
    public float DEFAULT_SIZE = 1f;

    @ConfigEntry(name = "redmatter_min_size", comment = "Smallest size of the redmatter before it dies")
    public float MIN_SIZE = 0.25f;

    @ConfigEntry(name = "redmatter_scale_rendering", comment = "Multiplier of size to modify the render size")
    public float RENDER_SCALE = 0.05f;

    @ConfigEntry(name = "redmatter_scale_kill_radius", comment = "Multiplier of size to create an instant kill center")
    public float KILL_SCALE = 0.08f;

    @ConfigEntry(name = "redmatter_scale_gravity", comment = "Multiplier of size to generate a pull towards the center")
    public float GRAVITY_SCALE = 2f;

    @ConfigEntry(name = "redmatter_starve_multiplier", comment = "Multiplier on how much mass is kept after each starve tick. 1.0 equals no starving and 0.0 means instant death of the redmatter.")
    @ConfigRange(min = 0.0, max = 1.0)
    public float STARVE_SCALE = 0.99f;


    //@Config.Name("redmatter_movement")
    //@Config.Comment("Allows red matter explosions to be moved")
    @ConfigEntry(name = "REDMATTER_MOVEMENT")
    public boolean REDMATTER_MOVEMENT = true; //TODO enable

    @ConfigEntry(name = "redmatter_max_edits_per_tick", comment = "Max number of edits per tick for the redmatter")
    public int MAX_BLOCKS_EDITS_PER_TICK = 100;

    @ConfigEntry(name = "redmatter_raytrace_per_tick", comment = "Number of raytraces per tick for the redmatter at it's default size")
    public int DEFAULT_BLOCK_RAYTRACE_PER_TICK = 500;

    @ConfigEntry(name = "redmatter_flying_block_spawn_chance", comment = "Chance for a flying block to spawn when destroying blocks")
    public float CHANCE_FOR_FLYING_BLOCK = 0.8f;

    @ConfigEntry(name = "redmatter_audio_enabled", comment = "Set to true to enable audio for the redmatter")
    public boolean ENABLE_AUDIO = true;

    @ConfigEntry(name = "redmatter_spawn_flying_blocks", comment = "Set to true to enable flying blocks to spawn")
    public boolean SPAWN_FLYING_BLOCKS = true;

    @ConfigEntry(name = "redmatter_colored_beams", comment = "Set to true to render redmatter with colored beams")
    public boolean RENDER_COLORED_BEAMS = true;
}
