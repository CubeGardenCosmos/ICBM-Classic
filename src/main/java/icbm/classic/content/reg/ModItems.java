package icbm.classic.content.reg;

import icbm.classic.config.ConfigItems;
import icbm.classic.content.items.ExplosiveFormItem;
import icbm.classic.content.items.ItemBombCart;
import icbm.classic.content.items.ItemGrenade;
import icbm.classic.content.items.ItemAntidote;
import icbm.classic.content.items.ItemBattery;
import icbm.classic.content.items.ItemDefuser;
import icbm.classic.content.items.ItemLaserDetonator;
import icbm.classic.content.items.ItemRadarGun;
import icbm.classic.content.items.ItemRemoteDetonator;
import icbm.classic.content.items.ItemRocketLauncher;
import icbm.classic.content.cargo.ItemThrowableProjectile;
import icbm.classic.content.cluster.bomblet.ItemBombDroplet;
import icbm.classic.content.cluster.missile.CapabilityClusterMissileStack;
import icbm.classic.content.cluster.missile.ItemClusterMissile;
import icbm.classic.content.items.ItemMissile;
import icbm.classic.content.missile.entity.anti.item.CapabilitySAMStack;
import icbm.classic.content.missile.entity.anti.item.ItemSurfaceToAirMissile;
import icbm.classic.content.missile.entity.itemstack.item.CapabilityHeldItemMissile;
import icbm.classic.content.missile.entity.itemstack.item.ItemHeldItemMissile;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import icbm.classic.lib.capability.missile.CapabilityMissileStack;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import icbm.classic.content.blocks.emptower.ItemBlockEmpTower;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Item registration. Renamed from the 1.12 {@code ItemReg} (porting-guide.md §3).
 * <p>
 * Holds every item that is not registered by {@link ModBlocks}: the block items (1.12 registered those in
 * {@code ItemReg} too, see overview.md §5) and the plain items.
 * <p>
 * Filled by S2.04 (materials and simple items), S2.05 (flat missile/grenade/bomb cart/bomblet items)
 * and later steps (tools in S5.08, battery in S6).
 */
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
    /** Empty missile shell (1.12 {@code missile} explosive id 24, registry-ids.md §3). */
    public static Item MISSILE_MODULE;

    /** Anti-ballistic missile (1.12 {@code surface_to_air_missile}, registry-ids.md §3, S4.04). */
    public static Item ANTI_BALLISTIC_MISSILE;
    public static Item CLUSTER_MISSILE;
    public static Item HELD_ITEM_MISSILE;
    public static Item PARACHUTE;
    public static Item BALLOON;

    //=================== S2.05 flat explosive items (registry-ids.md §1 forms M/G/C/D)
    /** Missiles by explosive id (form M). S4.02 replaces the placeholder item with the real missile item. */
    public static final Map<Identifier, Item> EXPLOSIVE_MISSILES = new LinkedHashMap<>();
    /** Grenades by explosive id (form G). S4.05 replaces the placeholder item. */
    public static final Map<Identifier, Item> EXPLOSIVE_GRENADES = new LinkedHashMap<>();
    /** Bomb carts by explosive id (form C). S4.05 replaces the placeholder item. */
    public static final Map<Identifier, Item> EXPLOSIVE_BOMB_CARTS = new LinkedHashMap<>();
    /** Bomblets by explosive id (form D, follows the grenade content). S4.06 replaces the placeholder item. */
    public static final Map<Identifier, Item> EXPLOSIVE_BOMBLETS = new LinkedHashMap<>();

    public static Item EMP_TOWER;
    public static Item REMOTE_DETONATOR;
    public static Item LASER_DETONATOR;
    public static Item RADAR_GUN;
    public static Item ROCKET_LAUNCHER;
    public static Item BALLISTIC_LAUNCHER;
    public static Item DEFUSER;
    public static Item BATTERY;
    private ModItems() {}

    /** Registers every item that is not a block. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // S5.02/S5.03 machine block items (1.12 ItemReg registered ordinary ItemBlock instances).
        RegHelper.blockItem(ModBlocks.LAUNCHER_BASE);
        RegHelper.blockItem(ModBlocks.LAUNCHER_FRAME);
        RegHelper.blockItem(ModBlocks.LAUNCHER_CONNECTOR);
        RegHelper.blockItem(ModBlocks.LAUNCHER_SCREEN);
        RegHelper.blockItem(ModBlocks.CRUISE_LAUNCHER);
        RegHelper.blockItem(ModBlocks.RADAR_STATION);
        EMP_TOWER = RegHelper.item("emp_tower", properties -> new ItemBlockEmpTower(ModBlocks.EMP_TOWER,
                properties.useBlockDescriptionPrefix()), new Item.Properties());
        RegHelper.blockItem(ModBlocks.CONCRETE);
        RegHelper.blockItem(ModBlocks.COMPACT_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_CONCRETE);
        RegHelper.blockItem(ModBlocks.REINFORCED_GLASS);
        RegHelper.blockItem(ModBlocks.SPIKES);
        RegHelper.blockItem(ModBlocks.POISON_SPIKES);
        RegHelper.blockItem(ModBlocks.FIRE_SPIKES);
        RegHelper.blockItem(ModBlocks.GLASS_BUTTON);
        RegHelper.blockItem(ModBlocks.GLASS_PRESSURE_PLATE);
        ModBlocks.EXPLOSIVE_BLOCKS.values().forEach(RegHelper::blockItem);
        RegHelper.blockItem(ModBlocks.RADIOACTIVE_DIRT);
        RegHelper.blockItem(ModBlocks.RADIOACTIVE_STONE);

        //Crafting materials: 1.12 ItemCrafting("ingot", "steel", "copper") and friends, one item per entry
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
        //The 1.12 OreDictionary names (ingotSteel, dustSulfur, ...) become item tags in S2.07

        SULFUR_DUST = material("sulfur_dust");
        SALTPETER = material("saltpeter");
        SALTPETER_BALL = material("saltpeter_ball");
        POISON_POWDER = material("poison_powder");
        EMPTY_BOMBLET = material("empty_bomblet");
        MISSILE_MODULE = material("missile_module");

        ANTIDOTE = RegHelper.item("antidote", ItemAntidote::new,
                new Item.Properties().food(ItemAntidote.FOOD, ItemAntidote.CONSUMABLE));

        //Flat explosive items: one item per explosive and form instead of the 1.12 metadata containers
        registerFormItems(ExplosiveForm.MISSILE, EXPLOSIVE_MISSILES);
        registerFormItems(ExplosiveForm.GRENADE, EXPLOSIVE_GRENADES);
        registerFormItems(ExplosiveForm.BOMB_CART, EXPLOSIVE_BOMB_CARTS);
        registerFormItems(ExplosiveForm.BOMBLET, EXPLOSIVE_BOMBLETS);

        // Register the missile stack capability lookup for all flat missile items (S4.02)
        IcbmLookups.MISSILE_STACK.registerForItems((stack, ctx) -> new CapabilityMissileStack(stack),
                EXPLOSIVE_MISSILES.values().toArray(new Item[0]));

        // Surface-to-air missile / Anti-ballistic missile (S4.04)
        ANTI_BALLISTIC_MISSILE = RegHelper.item("anti_ballistic_missile", ItemSurfaceToAirMissile::new, new Item.Properties());
        IcbmLookups.MISSILE_STACK.registerForItems((stack, ctx) -> new CapabilitySAMStack(), ANTI_BALLISTIC_MISSILE);

        CLUSTER_MISSILE = RegHelper.item("cluster_missile", ItemClusterMissile::new, new Item.Properties());
        HELD_ITEM_MISSILE = RegHelper.item("held_item_missile", ItemHeldItemMissile::new, new Item.Properties());
        PARACHUTE = RegHelper.item("parachute", ItemThrowableProjectile::new, new Item.Properties());
        BALLOON = RegHelper.item("balloon", ItemThrowableProjectile::new, new Item.Properties());

        IcbmLookups.MISSILE_STACK.registerForItems((stack, ctx) -> new CapabilityClusterMissileStack(stack), CLUSTER_MISSILE);
        IcbmLookups.MISSILE_STACK.registerForItems((stack, ctx) -> new CapabilityHeldItemMissile(stack), HELD_ITEM_MISSILE);

        REMOTE_DETONATOR = RegHelper.item("remote_detonator", ItemRemoteDetonator::new, new Item.Properties().stacksTo(1));
        LASER_DETONATOR = RegHelper.item("laser_detonator", ItemLaserDetonator::new, new Item.Properties().stacksTo(1));
        RADAR_GUN = RegHelper.item("radar_gun", ItemRadarGun::new, new Item.Properties().stacksTo(1));
        ROCKET_LAUNCHER = RegHelper.item("rocket_launcher", p -> new ItemRocketLauncher(false, p), new Item.Properties().stacksTo(1));
        BALLISTIC_LAUNCHER = RegHelper.item("ballistic_launcher", p -> new ItemRocketLauncher(true, p), new Item.Properties().stacksTo(1));
        DEFUSER = RegHelper.item("defuser", ItemDefuser::new, new Item.Properties().stacksTo(1));
        BATTERY = RegHelper.item("battery", ItemBattery::new, new Item.Properties().stacksTo(1));

        ItemRemoteDetonator.registerUseCallback();
        ItemLaserDetonator.registerUseCallback();
        ItemDefuser.registerAttackCallback();
    }

    /** Registers one placeholder item per explosive enabled for the form, plus the matching block item for {@link ExplosiveForm#BLOCK}. */
    private static void registerFormItems(ExplosiveForm form, Map<Identifier, Item> target)
    {
        for (Identifier ex : ExplosiveForms.enabled(form))
        {
            final Item item = RegHelper.item(form.idFor(ex),
                    p -> createFormItem(ex, form, p), new Item.Properties());
            target.put(ex, item);
        }
    }

    /**
     * Creates the item of one explosive and form. S4.05 replaced the grenade and bomb cart placeholders with
     * the real behaviour items ({@link ItemGrenade}/{@link ItemBombCart}); S4.02 (missile) and S4.06 (bomblet)
     * replace their placeholders the same way.
     *
     * @param ex   explosive id, see {@code docs/architecture/registry-ids.md} §1
     * @param form form of the item
     * @param p    item properties, the stack size is set per form (1.12 {@code setMaxStackSize})
     */
    private static Item createFormItem(Identifier ex, ExplosiveForm form, Item.Properties p)
    {
        return switch (form)
        {
            case MISSILE -> new ItemMissile(ex, form, p.stacksTo(ItemMissile.MAX_STACK_SIZE));
            case GRENADE -> new ItemGrenade(ex, form, p.stacksTo(ItemGrenade.MAX_STACK_SIZE));
            case BOMB_CART -> new ItemBombCart(ex, form, p.stacksTo(ItemBombCart.MAX_STACK_SIZE));
            case BOMBLET -> new ItemBombDroplet(ex, form, p.stacksTo(ItemBombDroplet.MAX_STACK_SIZE));
            default -> new ExplosiveFormItem(ex, form, p);
        };
    }

    private static Item material(String id)
    {
        return RegHelper.item(id, Item::new, new Item.Properties());
    }

    /**
     * 1.12 registered the crafting materials only when the matching {@code ConfigItems} switch was on.
     * 26.3 can not remove an id without breaking the data driven content, so the item is always registered
     * and only hidden from the creative tab (S2.05). The recipes are generated with the same switch as a
     * {@code fabric:load_conditions} condition (S2.08).
     *
     * @param item item to check
     * @return true when the item must not be listed in the creative tab
     */
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
