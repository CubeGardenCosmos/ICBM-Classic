package icbm.classic.config.blocks;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

@IcbmConfig(file = "blocks")
public class ConfigBlocks
{
    @ConfigEntry(name = "spikes", comment = "Config for spike blocks")
    public static ConfigSpikes spikes = new ConfigSpikes();

    @ConfigEntry(name = "radioactive", comment = "Config for radioactive blocks")
    public static ConfigRadioactive radioactive = new ConfigRadioactive();
}
