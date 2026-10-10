package icbm.classic.content.reg;

import icbm.classic.config.ConfigItems;
import icbm.classic.content.items.ItemAntidote;
import net.minecraft.world.item.Item;

public final class ModItems
{
    //=================== S2.04 crafting materials (1.12 ItemCrafting, metadata flattened)
    public static Item STEEL_INGOT;
    public static Item COPPER_INGOT;
    public static Item STEEL_CLUMP;
    public static Item STEEL_PLATE;
    public static Item IRON_PLATE;
    public static Item BASIC_CIRCUIT;
    public static Item ADVANCED_CIRCUIT;
    public static Item ELITE_CIRCUIT;
    public static Item COPPER_WIRE;
    public static Item GOLD_WIRE;

    //=================== S2.04 simple items
    public static Item SULFUR_DUST;
    public static Item SALTPETER;
    public static Item SALTPETER_BALL;
    public static Item POISON_POWDER;
    public static Item ANTIDOTE;
    public static Item EMPTY_BOMBLET;
    public static Item MISSILE_MODULE;

    private ModItems() {}

    public static void register()
    {
        RegHelper.blockItem(ModBlocks.CONCRETE);
        RegHelper.blockItem(ModBlocks.COMPACT_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_GLASS);
        RegHelper.blockItem(ModBlocks.SPIKES);
        RegHelper.blockItem(ModBlocks.POISON_SPIKES);
        RegHelper.blockItem(ModBlocks.FIRE_SPIKES);
        RegHelper.blockItem(ModBlocks.GLASS_BUTTON);
        RegHelper.blockItem(ModBlocks.GLASS_PRESSURE_PLATE);

        STEEL_INGOT = material("steel_ingot");
        COPPER_INGOT = material("copper_ingot");
        STEEL_CLUMP = material("steel_clump");
        STEEL_PLATE = material("steel_plate");
        IRON_PLATE = material("iron_plate");
        BASIC_CIRCUIT = material("basic_circuit");
        ADVANCED_CIRCUIT = material("advanced_circuit");
        ELITE_CIRCUIT = material("elite_circuit");
        COPPER_WIRE = material("copper_wire");
        GOLD_WIRE = material("gold_wire");

        SULFUR_DUST = material("sulfur_dust");
        SALTPETER = material("saltpeter");
        SALTPETER_BALL = material("saltpeter_ball");
        POISON_POWDER = material("poison_powder");
        EMPTY_BOMBLET = material("empty_bomblet");
        MISSILE_MODULE = material("missile_module");

        ANTIDOTE = RegHelper.item("antidote", ItemAntidote::new,
                new Item.Properties().food(ItemAntidote.FOOD, ItemAntidote.CONSUMABLE));
    }

    private static Item material(String id)
    {
        return RegHelper.item(id, Item::new, new Item.Properties());
    }

    public static boolean isHiddenFromCreativeTab(Item item)
    {
        if (!ConfigItems.ENABLE_CRAFTING_ITEMS)
        {
            return item == STEEL_INGOT || item == COPPER_INGOT || item == STEEL_CLUMP
                    || item == STEEL_PLATE || item == IRON_PLATE
                    || item == BASIC_CIRCUIT || item == ADVANCED_CIRCUIT || item == ELITE_CIRCUIT
                    || item == COPPER_WIRE || item == GOLD_WIRE;
        }
        if (!ConfigItems.ENABLE_INGOTS_ITEMS)
        {
            return item == STEEL_INGOT || item == COPPER_INGOT || item == STEEL_CLUMP;
        }
        if (!ConfigItems.ENABLE_PLATES_ITEMS)
        {
            return item == STEEL_PLATE || item == IRON_PLATE;
        }
        if (!ConfigItems.ENABLE_CIRCUIT_ITEMS)
        {
            return item == BASIC_CIRCUIT || item == ADVANCED_CIRCUIT || item == ELITE_CIRCUIT;
        }
        if (!ConfigItems.ENABLE_WIRES_ITEMS)
        {
            return item == COPPER_WIRE || item == GOLD_WIRE;
        }
        return false;
    }
}
