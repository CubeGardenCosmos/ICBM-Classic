package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * Creative mode tabs. 1.12 had a single {@code ICBMCreativeTab} that ordered items explicitly and then
 * appended everything else; 26.3 registers one tab {@code icbmclassic:main} whose display list is filled
 * here.
 * <p>
 * Order copied from the 1.12 {@code ICBMCreativeTab#init} list: machines, base blocks, tools, missiles,
 * grenades, explosive blocks, bomb carts, then the remaining items in the 1.12 collection order. Content
 * that is not registered yet (machines and tools, S5) is looked up by id and skipped, so it appears
 * automatically once its step registers it.
 * <p>
 * Difference to 1.12: the glass button and pressure plate were in the vanilla redstone tab (1.12 set
 * {@code CreativeTabs.REDSTONE} on them); they are listed here instead so no item is missing from the mod tab.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public final class ModCreativeTabs
{
    /** Machine blocks, registered by S5.1–S5.6. */
    private static final List<String> MACHINE_IDS = List.of(
            "launcher_base", "launcher_screen", "launcher_frame", "launcher_connector",
            "cruise_launcher", "emp_tower", "radar_station");

    /** Tool items, registered by S5.08. */
    private static final List<String> TOOL_IDS = List.of(
            "rocket_launcher", "ballistic_launcher", "radar_gun",
            "remote_detonator", "laser_detonator", "defuser", "battery");

    public static CreativeModeTab MAIN;

    private ModCreativeTabs() {}

    /** Registers the creative tab. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ICBMConstants.id("main"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.icbmclassic.main"))
                        .icon(() -> new ItemStack(ModItems.EXPLOSIVE_MISSILES.get(ICBMConstants.id("nuclear"))))
                        .displayItems(ModCreativeTabs::displayItems)
                        .build());
    }

    private static void displayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output)
    {
        //Machines (S5)
        MACHINE_IDS.forEach(id -> acceptById(output, id));

        //Base blocks: 1.12 ordered concrete, reinforced glass, spikes (metadata containers expanded)
        accept(output, ModBlocks.CONCRETE);
        accept(output, ModBlocks.COMPACT_CONCRETE);
        accept(output, ModBlocks.REINFORCED_CONCRETE);
        accept(output, ModBlocks.REINFORCED_GLASS);
        accept(output, ModBlocks.SPIKES);
        accept(output, ModBlocks.POISON_SPIKES);
        accept(output, ModBlocks.FIRE_SPIKES);

        //Tools (S5.08)
        TOOL_IDS.forEach(id -> acceptById(output, id));

        //Explosives in the registry-ids.md §1 order (form missiles, then grenades, blocks, bomb carts, bomblets)
        acceptAll(output, ModItems.EXPLOSIVE_MISSILES);
        accept(output, ModItems.ANTI_BALLISTIC_MISSILE);
        acceptById(output, "held_item_missile");
        acceptById(output, "cluster_missile");
        acceptAll(output, ModItems.EXPLOSIVE_GRENADES);
        ModBlocks.EXPLOSIVE_BLOCKS.values().forEach(block -> accept(output, block));
        acceptAll(output, ModItems.EXPLOSIVE_BOMB_CARTS);
        acceptAll(output, ModItems.EXPLOSIVE_BOMBLETS);

        //Remaining items, in the 1.12 collection order
        accept(output, ModItems.EMPTY_BOMBLET);
        acceptById(output, "parachute");
        acceptById(output, "balloon");
        accept(output, ModBlocks.GLASS_BUTTON);
        accept(output, ModBlocks.GLASS_PRESSURE_PLATE);
        accept(output, ModItems.POISON_POWDER);
        accept(output, ModItems.SULFUR_DUST);
        accept(output, ModItems.SALTPETER);
        accept(output, ModItems.SALTPETER_BALL);
        accept(output, ModItems.ANTIDOTE);
        accept(output, ModItems.MISSILE_MODULE);
        // 1.12 ItemReg registered the radioactive block item last among the block items (ItemReg:157), so the
        // leftover pass puts them right before the crafting materials
        accept(output, ModBlocks.RADIOACTIVE_DIRT);
        accept(output, ModBlocks.RADIOACTIVE_STONE);

        //Crafting materials (the 1.12 collection appended them last)
        accept(output, ModItems.STEEL_INGOT);
        accept(output, ModItems.COPPER_INGOT);
        accept(output, ModItems.STEEL_CLUMP);
        accept(output, ModItems.STEEL_PLATE);
        accept(output, ModItems.IRON_PLATE);
        accept(output, ModItems.BASIC_CIRCUIT);
        accept(output, ModItems.ADVANCED_CIRCUIT);
        accept(output, ModItems.ELITE_CIRCUIT);
        accept(output, ModItems.COPPER_WIRE);
        accept(output, ModItems.GOLD_WIRE);

        // Energy storage/display for the battery arrives in S6.04; the S5.08 item is listed here already.
    }

    private static void accept(CreativeModeTab.Output output, Block block)
    {
        accept(output, RegHelper.item(BuiltInRegistries.BLOCK.getKey(block).getPath()));
    }

    private static void accept(CreativeModeTab.Output output, Item item)
    {
        //Null when the content is not registered yet; config gated items stay registered but hidden (S2.04)
        if (item != null && !ModItems.isHiddenFromCreativeTab(item))
        {
            output.accept(item);
        }
    }

    private static void acceptById(CreativeModeTab.Output output, String id)
    {
        accept(output, RegHelper.item(id));
    }

    private static void acceptAll(CreativeModeTab.Output output, Map<net.minecraft.resources.Identifier, Item> items)
    {
        items.values().forEach(item -> accept(output, item));
    }
}
