package icbm.classic.test.reg;

import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ExplosiveForms;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * Covers the S2.05 flat explosive content and the creative tab:
 *
 * <ul>
 *     <li>every explosive form from registry-ids.md §1 has its flat block/item registered,</li>
 *     <li>the {@code icbmclassic:main} tab exists and lists every registered icbmclassic item (a broken
 *     display list or a content id that does not match {@link ExplosiveForms} fails here),</li>
 *     <li>the tab icon is the nuclear missile (1.12 used the explosive missile container item).</li>
 * </ul>
 */
public class CreativeTabGameTest
{
    @GameTest(maxTicks = 40)
    public void flatExplosivesAndCreativeTab(GameTestHelper helper)
    {
        for (ExplosiveForm form : List.of(ExplosiveForm.MISSILE, ExplosiveForm.GRENADE,
                ExplosiveForm.BOMB_CART, ExplosiveForm.BOMBLET))
        {
            for (Identifier ex : ExplosiveForms.enabled(form))
            {
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ICBMConstants.id(form.idFor(ex))),
                        "missing " + form + " item " + form.idFor(ex));
            }
        }
        for (Identifier ex : ExplosiveForms.enabled(ExplosiveForm.BLOCK))
        {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ICBMConstants.id(ExplosiveForm.BLOCK.idFor(ex))),
                    "missing explosive block " + ExplosiveForm.BLOCK.idFor(ex));
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ICBMConstants.id(ExplosiveForm.BLOCK.idFor(ex))),
                    "missing explosive block item " + ExplosiveForm.BLOCK.idFor(ex));
        }

        helper.assertValueEqual(23, ModItems.EXPLOSIVE_MISSILES.size(), "missile map size");
        helper.assertValueEqual(10, ModItems.EXPLOSIVE_GRENADES.size(), "grenade map size");
        helper.assertValueEqual(23, ModItems.EXPLOSIVE_BOMB_CARTS.size(), "bomb cart map size");
        helper.assertValueEqual(10, ModItems.EXPLOSIVE_BOMBLETS.size(), "bomblet map size");
        helper.assertValueEqual(23, ModBlocks.EXPLOSIVE_BLOCKS.size(), "explosive block map size");

        final int registered = (int) BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(ICBMConstants.DOMAIN))
                .count();
        ICBMClassic.LOGGER.info("icbmclassic registered item count: {}", registered);
        helper.assertTrue(registered >= 115,
                "expected at least 115 icbmclassic items but got " + registered);

        final CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(ICBMConstants.id("main"));
        helper.assertTrue(tab != null, "the icbmclassic:main creative tab is not registered");
        if (tab == null)
        {
            return;
        }
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false,
                helper.getLevel().registryAccess()));
        final Collection<ItemStack> displayItems = tab.getDisplayItems();
        helper.assertValueEqual(registered, displayItems.size(),
                "the creative tab should list every icbmclassic item");

        final ItemStack icon = tab.getIconItem();
        helper.assertTrue(!icon.isEmpty() && icon.getItem() == ModItems.EXPLOSIVE_MISSILES.get(ICBMConstants.id("nuclear")),
                "the tab icon should be the nuclear missile but was " + icon);

        helper.succeed();
    }
}
