package icbm.classic.config.blast.types;

import icbm.classic.config.core.ConfigEntry;
import icbm.classic.config.core.ConfigRange;

public class ConfigEmp {

    @ConfigEntry(name = "scale", comment = "Scale of the explosive; This isn't always max size but a multiplier used in calculations.")
    @ConfigRange(min = 1, max = Integer.MAX_VALUE)
    public double scale = 50;

    @ConfigEntry(name = "replacements")
    public BlockReplacements replacements = new BlockReplacements();

    public static class BlockReplacements {

        @ConfigEntry(name = "list", comment = "Sets which block to use when replacing a block during an emp event Docs: https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-block-states")
        public String[] blockStates = new String[]{};
    }

    @ConfigEntry(name = "allow_creeper_charging", comment = "Should a lighting effect be applied to the creeper to super charge it due to EMP effect?")
    public boolean ALLOW_LIGHTING_CREEPER = true;

    @ConfigEntry(name = "missiles", comment = "EMP Settings for missiles")
    public final Missiles missiles = new Missiles();

    public static class Missiles {

        @ConfigEntry(name = "enabled", comment = "Should EMP work on missiles")
        public boolean enabled = true;

        @ConfigEntry(name = "engine_kill_chance", comment = "Chance the engine will be disabled, random <= chance")
        @ConfigRange(min = 0, max = 1)
        public float engineKillChance = 0.8f;

        @ConfigEntry(name = "engine_fuel_blow_chance", comment = "Chance that when the missile is killed the fuel will detonate, random <= chance")
        @ConfigRange(min = 0, max = 1)
        public float engineFullBlowChance = 1f;

        @ConfigEntry(name = "engine_fuel_blow_strength", comment = "Power of the explosion from the fuel")
        @ConfigRange(min = 0.1, max = Double.MAX_VALUE)
        public float engineFullBlowStrength = 1f;

        @ConfigEntry(name = "missile_kill_chance", comment = "Chance the missile will be destroyed, random <= chance")
        @ConfigRange(min = 0, max = 1)
        public float missileKillChance = 0.5f;

        @ConfigEntry(name = "missile_trigger_chance", comment = "Chance the missile explosive will be triggered, random <= chance")
        @ConfigRange(min = 0, max = 1)
        public float missileTriggerChance = 0.1f;
    }

    @ConfigEntry(name = "allow_entity_inventory", comment = "Should EMP effect run on entity inventories? (Eg. Player, Cart)")
    public boolean ALLOW_ENTITY_INVENTORY = true;

    @ConfigEntry(name = "allow_tile_inventory", comment = "Should EMP effect run on block/tile inventories? (Eg. Chest, Hopper, Machine)")
    public boolean ALLOW_TILE_INVENTORY = true;

    @ConfigEntry(name = "allow_item_inventory", comment = "Should EMP effect run on item inventories? (Eg. Bag, Backpack)")
    public boolean ALLOW_ITEM_INVENTORY = true;

    @ConfigEntry(name = "allow_ground_items", comment = "Should EMP effect run on items dropped on the ground?")
    public boolean ALLOW_GROUND_ITEMS = true;

    @ConfigEntry(name = "allow_draining_energy_entity", comment = "Should EMP effect drain energy entities that do not support EMP effect directly?")
    public boolean DRAIN_ENERGY_ENTITY = true;

    @ConfigEntry(name = "allow_draining_energy_items", comment = "Should EMP effect drain energy items that do not support EMP effect directly?")
    public boolean DRAIN_ENERGY_ITEMS = true;

    @ConfigEntry(name = "allow_draining_energy_tiles", comment = "Should EMP effect drain energy tiles that do not support EMP effect directly?")
    public boolean DRAIN_ENERGY_TILES = true;

    @ConfigEntry(name = "allow_entities", comment = "Should EMP effect run on entities?")
    public boolean ALLOW_ENTITY = true;

    @ConfigEntry(name = "allow_tiles", comment = "Should EMP effect run on blocks and tiles?")
    public boolean ALLOW_TILES = true;
}
