package icbm.classic.test.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.EnumTier;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.reg.ExplosiveForms;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Covers the S2.02 explosive registry and the flat form lists:
 *
 * <ul>
 *     <li>every explosive of registry-ids.md §1 is registered and the registry is locked after init,</li>
 *     <li>the tier rules from the 1.12 {@code newEx} are preserved: tier != NONE has block/missile/bomb cart,
 *     tier ONE adds grenade and bomblet (which are the same set),</li>
 *     <li>the form order follows registry-ids.md §1, so a reordering or a dropped entry fails the test,</li>
 *     <li>registering after the lock throws instead of silently adding content.</li>
 * </ul>
 */
public class ExplosiveFormsGameTest
{
    @GameTest(maxTicks = 20)
    public void explosiveFormsMatchTheRegistryTable(GameTestHelper helper)
    {
        // 23 content explosives + rot + mutation (tier NONE, blast only)
        helper.assertTrue(ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosives().size() == 25,
                "expected 25 registered explosives but got " + ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosives().size());

        final List<Identifier> blocks = ExplosiveForms.enabled(ExplosiveForm.BLOCK);
        final List<Identifier> missiles = ExplosiveForms.enabled(ExplosiveForm.MISSILE);
        final List<Identifier> grenades = ExplosiveForms.enabled(ExplosiveForm.GRENADE);
        final List<Identifier> bomblets = ExplosiveForms.enabled(ExplosiveForm.BOMBLET);
        final List<Identifier> carts = ExplosiveForms.enabled(ExplosiveForm.BOMB_CART);

        helper.assertTrue(blocks.size() == 23, "expected 23 explosive blocks but got " + blocks);
        helper.assertTrue(missiles.size() == 23, "expected 23 missiles but got " + missiles);
        helper.assertTrue(carts.size() == 23, "expected 23 bomb carts but got " + carts);
        helper.assertTrue(grenades.size() == 10, "expected 10 grenades but got " + grenades);
        helper.assertTrue(bomblets.equals(grenades), "bomblets should follow grenades but were " + bomblets);

        // order comes from registry-ids.md §1 (colors and smoke at the end of tier 1)
        helper.assertTrue(blocks.get(0).equals(ICBMConstants.id("condensed")),
                "first block should be condensed but was " + blocks.get(0));
        helper.assertTrue(blocks.get(blocks.size() - 1).equals(ICBMConstants.id("redmatter")),
                "last block should be redmatter but was " + blocks.get(blocks.size() - 1));
        helper.assertTrue(blocks.indexOf(ICBMConstants.id("colors")) > blocks.indexOf(ICBMConstants.id("attractive")),
                "colors should follow the tier 1 explosives but was at " + blocks.indexOf(ICBMConstants.id("colors")));
        helper.assertTrue(blocks.indexOf(ICBMConstants.id("smoke")) > blocks.indexOf(ICBMConstants.id("colors")),
                "smoke should follow colors but was at " + blocks.indexOf(ICBMConstants.id("smoke")));

        // tier NONE explosives have no content at all
        helper.assertTrue(!blocks.contains(ICBMConstants.id("rot")), "rot should not have block content");
        helper.assertTrue(!blocks.contains(ICBMConstants.id("mutation")), "mutation should not have block content");

        // tier 3 explosive is a missile but never a grenade
        helper.assertTrue(missiles.contains(ICBMConstants.id("nuclear")), "nuclear should have missile content");
        helper.assertTrue(!grenades.contains(ICBMConstants.id("nuclear")), "nuclear should not have grenade content");
        // tier ONE additions are grenades even though their 1.12 id was -1
        helper.assertTrue(grenades.contains(ICBMConstants.id("colors")), "colors should have grenade content");
        helper.assertTrue(grenades.contains(ICBMConstants.id("smoke")), "smoke should have grenade content");

        // tiers match registry-ids.md §1
        helper.assertTrue(ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id("redmatter")).getTier() == EnumTier.FOUR,
                "redmatter should be tier FOUR");
        helper.assertTrue(ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id("rot")).getTier() == EnumTier.NONE,
                "rot should be tier NONE");

        // the registry is locked after init
        try
        {
            ICBMClassicAPI.EXPLOSIVE_REGISTRY.register(ICBMConstants.id("test_after_lock"), EnumTier.ONE,
                    (world, x, y, z, source) -> null);
            helper.fail("registering an explosive after the lock should throw");
        }
        catch (IllegalStateException expected)
        {
            helper.assertTrue("ICBM explosive registry is locked".equals(expected.getMessage()),
                    "unexpected lock message: " + expected.getMessage());
        }

        // getExplosiveData falls back to condensed when the name is unknown
        final IExplosiveData fallback = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id("does_not_exist"));
        helper.assertTrue(fallback != null && fallback.getRegistryKey().equals(ICBMConstants.id("condensed")),
                "unknown explosive should fall back to condensed but got " + fallback);

        helper.succeed();
    }
}
