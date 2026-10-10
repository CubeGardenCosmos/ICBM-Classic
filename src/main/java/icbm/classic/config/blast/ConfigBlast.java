package icbm.classic.config.blast;

import icbm.classic.config.blast.types.*;
import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.IcbmConfig;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 2/22/2018.
 */
@IcbmConfig(file = "blast")
public class ConfigBlast
{
    @ConfigEntry(name = "condensed", comment = "Settings for condensed explosive")
    public static ConfigCondensed condensed = new ConfigCondensed();

    @ConfigEntry(name = "shrapnel", comment = "Settings for shrapnel explosive")
    public static ConfigShrapnel shrapnel = new ConfigShrapnel();

    @ConfigEntry(name = "incendiary", comment = "Settings for incendiary explosive")
    public static ConfigExBasics incendiary = new ConfigExBasics(14);

    @ConfigEntry(name = "debilitation", comment = "Settings for debilitation explosive")
    public static ConfigDebilitation debilitation = new ConfigDebilitation();

    @ConfigEntry(name = "chemical", comment = "Settings for chemical explosive")
    public static ConfigChemical chemical = new ConfigChemical();

    @ConfigEntry(name = "anvil", comment = "Settings for anvil explosive")
    public static ConfigAnvil anvil = new ConfigAnvil();

    @ConfigEntry(name = "repulsive", comment = "Settings for repulsive explosive")
    public static ConfigExBasics repulsive = new ConfigExBasics(2);

    @ConfigEntry(name = "attractive", comment = "Settings for attractive explosive")
    public static ConfigExBasics attractive = new ConfigExBasics(2);

    @ConfigEntry(name = "fragmentation", comment = "Settings for fragmentation explosive")
    public static ConfigFragment fragmentation = new ConfigFragment();

    @ConfigEntry(name = "contagious", comment = "Settings for contagious explosive")
    public static ConfigContagious contagious = new ConfigContagious();

    @ConfigEntry(name = "sonic", comment = "Settings for sonic explosive")
    public static ConfigExBasics sonic = new ConfigExBasics(15);

    @ConfigEntry(name = "breaching", comment = "Settings for breaching explosive")
    public static ConfigBreaching breaching = new ConfigBreaching();

    @ConfigEntry(name = "thermobaric", comment = "Settings for thermobaric explosive")
    public static ConfigThermobaric thermobaric = new ConfigThermobaric();

    @ConfigEntry(name = "nuclear", comment = "Settings for nuclear explosive")
    public static ConfigNuclear nuclear = new ConfigNuclear();

    @ConfigEntry(name = "emp", comment = "Settings for emp explosive")
    public static ConfigEmp emp = new ConfigEmp();

    @ConfigEntry(name = "exothermic", comment = "Settings for exothermic explosive")
    public static ConfigExBasics exothermic = new ConfigExBasics(30);

    @ConfigEntry(name = "endothermic", comment = "Settings for endothermic explosive")
    public static ConfigExBasics endothermic = new ConfigExBasics(30);

    @ConfigEntry(name = "antigravitational", comment = "Settings for antigravitational explosive")
    public static ConfigExBasics antigravitational = new ConfigExBasics(30);

    @ConfigEntry(name = "ender", comment = "Settings for ender explosive")
    public static ConfigExBasics ender = new ConfigExBasics(30);

    @ConfigEntry(name = "antimatter", comment = "Settings for antimatter explosive")
    public static ConfigAntimatter antimatter = new ConfigAntimatter();

    @ConfigEntry(name = "redmatter", comment = "Set for redmatter blast")
    public static ConfigRedmatter redmatter = new ConfigRedmatter();

    @ConfigEntry(name = "colorful", comment = "Set for colorful blast")
    public static ConfigExBasics colorful = new ConfigExBasics(10);

    @ConfigEntry(name = "blast_do_block_updates", comment = "Whether or not the big explosions trigger block updates for all blocks.\nSetting this to false leads to performance improvements, especially when dealing with a lot of water.")
    public static boolean BLAST_DO_BLOCKUPDATES = false;

    @Deprecated //Move to sub-config
    @ConfigEntry(name = "allow_day_night_switch", comment = "Allows explosives to change time of day")
    public static boolean ALLOW_DAY_NIGHT = true;

    @Deprecated //Move to each type
    @ConfigEntry(name = "FUSE_TIMES", comment = "Set the time between ignition and explosion seperately for each bomb cart/explosive/grenade here.")
    public static ConfigFuseTimes FUSE_TIMES = new ConfigFuseTimes();

}
