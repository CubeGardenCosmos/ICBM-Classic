package icbm.classic.config.blast;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

/**
 * Created by Dark(DarkGuardsman, Robin) on 2/9/2020.
 */
public class ConfigFuseTimes
{
    @ConfigEntry(name = "BOMB_CARTS", comment = "Set fuse times for bomb carts here. The times are written in ticks, where 20 ticks = 1 second.")
    public BombCarts BOMB_CARTS = new BombCarts();

    @ConfigEntry(name = "EXPLOSIVES", comment = "Set fuse times for explosives (the block) here. The times are written in ticks, where 20 ticks = 1 second.")
    public Explosives EXPLOSIVES = new Explosives();

    @ConfigEntry(name = "GRENADES", comment = "Set fuse times for grenades here. The times are written in ticks, where 20 ticks = 1 second.")
    public Grenades GRENADES = new Grenades();

    public class BombCarts
    {
        @ConfigEntry(name = "CONDENSED")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CONDENSED = 100;

        @ConfigEntry(name = "SHRAPNEL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int SHRAPNEL = 100;

        @ConfigEntry(name = "INCENDIARY")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int INCENDIARY = 100;

        @ConfigEntry(name = "DEBILITATION")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int DEBILITATION = 100;

        @ConfigEntry(name = "CHEMICAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CHEMICAL = 100;

        @ConfigEntry(name = "ANVIL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANVIL = 100;

        @ConfigEntry(name = "REPULSIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int REPULSIVE = 120;

        @ConfigEntry(name = "ATTRACTIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ATTRACTIVE = 120;

        @ConfigEntry(name = "FRAGMENTATION")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int FRAGMENTATION = 100;

        @ConfigEntry(name = "CONTAGIOUS")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CONTAGIOUS = 100;

        @ConfigEntry(name = "SONIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int SONIC = 100;

        @ConfigEntry(name = "BREACHING")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int BREACHING = 0;

        @ConfigEntry(name = "THERMOBARIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int THERMOBARIC = 100;

        @ConfigEntry(name = "NUCLEAR")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int NUCLEAR = 100;

        @ConfigEntry(name = "EMP")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int EMP = 100;

        @ConfigEntry(name = "EXOTHERMIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int EXOTHERMIC = 100;

        @ConfigEntry(name = "ENDOTHERMIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ENDOTHERMIC = 100;

        @ConfigEntry(name = "ANTI_GRAVITATIONAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANTI_GRAVITATIONAL = 100;

        @ConfigEntry(name = "ENDER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ENDER = 100;

        @ConfigEntry(name = "HYPERSONIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int HYPERSONIC = 100;

        @ConfigEntry(name = "ANTIMATTER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANTIMATTER = 300;

        @ConfigEntry(name = "REDMATTER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int REDMATTER = 100;
    }

    public class Explosives
    {
        @ConfigEntry(name = "CONDENSED")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CONDENSED = 0;

        @ConfigEntry(name = "SHRAPNEL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int SHRAPNEL = 100;

        @ConfigEntry(name = "INCENDIARY")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int INCENDIARY = 100;

        @ConfigEntry(name = "DEBILITATION")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int DEBILITATION = 100;

        @ConfigEntry(name = "CHEMICAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CHEMICAL = 100;

        @ConfigEntry(name = "ANVIL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANVIL = 100;

        @ConfigEntry(name = "REPULSIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int REPULSIVE = 120;

        @ConfigEntry(name = "ATTRACTIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ATTRACTIVE = 120;

        @ConfigEntry(name = "FRAGMENTATION")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int FRAGMENTATION = 100;

        @ConfigEntry(name = "CONTAGIOUS")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CONTAGIOUS = 100;

        @ConfigEntry(name = "SONIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int SONIC = 100;

        @ConfigEntry(name = "BREACHING")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int BREACHING = 0;

        @ConfigEntry(name = "THERMOBARIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int THERMOBARIC = 100;

        @ConfigEntry(name = "NUCLEAR")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int NUCLEAR = 100;

        @ConfigEntry(name = "EMP")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int EMP = 100;

        @ConfigEntry(name = "EXOTHERMIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int EXOTHERMIC = 100;

        @ConfigEntry(name = "ENDOTHERMIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ENDOTHERMIC = 100;

        @ConfigEntry(name = "ANTI_GRAVITATIONAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANTI_GRAVITATIONAL = 100;

        @ConfigEntry(name = "ENDER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ENDER = 100;

        @ConfigEntry(name = "HYPERSONIC")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int HYPERSONIC = 100;

        @ConfigEntry(name = "ANTIMATTER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANTIMATTER = 300;

        @ConfigEntry(name = "REDMATTER")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int REDMATTER = 100;
    }

    public class Grenades
    {
        @ConfigEntry(name = "CONVENTIONAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CONVENTIONAL = 100;

        @ConfigEntry(name = "SHRAPNEL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int SHRAPNEL = 100;

        @ConfigEntry(name = "INCENDIARY")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int INCENDIARY = 100;

        @ConfigEntry(name = "DEBILITATION")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int DEBILITATION = 100;

        @ConfigEntry(name = "CHEMICAL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int CHEMICAL = 100;

        @ConfigEntry(name = "ANVIL")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ANVIL = 100;

        @ConfigEntry(name = "REPULSIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int REPULSIVE = 120;

        @ConfigEntry(name = "ATTRACTIVE")
        @ConfigRange(min = 0, max = Integer.MAX_VALUE)
        public int ATTRACTIVE = 120;
    }
}
