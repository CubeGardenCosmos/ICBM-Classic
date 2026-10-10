package icbm.classic.config;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;
import icbm.classic.config.core.IcbmConfig;

/**
 * Created by Dark(DarkGuardsman, Robin) on 2/10/2019.
 */
@IcbmConfig(file = "thread")
public class ConfigThread
{
    @ConfigEntry(name = "thread_count", comment = "Number of worker threads to run to handle blast calculations. " +
            "Try to only match 50% of the number of cores your machine can support. " +
            "Otherwise the main game thread will slow down while the workers are processing. " +
            "Which is counter to the reason threads exist.")
    @ConfigRange(min = 1, max = 8)
    public static int THREAD_COUNT = 1;

    @ConfigEntry(name = "blocks_per_tick", comment = "Max block edits applied by the main thread per tick for threaded explosions")
    @ConfigRange(min = 1, max = 1_000_000)
    public static int BLOCKS_PER_TICK = 2000;
}
