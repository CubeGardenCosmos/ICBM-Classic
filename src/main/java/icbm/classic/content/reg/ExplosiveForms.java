package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.reg.content.IExplosiveContentRegistry;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The flat forms an explosive exists as (ADR 0002). 1.12 stored every form in one block/item with
 * metadata; 26.3 registers one block or item per explosive and form, so the content lists are derived
 * from the content registry that {@link ExplosiveInit} fills.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public final class ExplosiveForms
{
    /**
     * Forms an explosive can be placed as.
     * <p>
     * {@link #suffix} completes the flat id: {@code <explosive>_<suffix>} (registry-ids.md §1–§3).
     */
    public enum ExplosiveForm
    {
        BLOCK("explosive"),
        MISSILE("missile"),
        GRENADE("grenade"),
        BOMB_CART("bomb_cart"),
        BOMBLET("bomblet");

        public final String suffix;

        ExplosiveForm(String suffix)
        {
            this.suffix = suffix;
        }

        /** Flat id of this form for the given explosive, e.g. {@code icbmclassic:condensed} + BLOCK -> {@code condensed_explosive}. */
        public String idFor(Identifier explosiveId)
        {
            return explosiveId.getPath() + "_" + suffix;
        }
    }

    /**
     * Explosive order taken from registry-ids.md §1 (the 1.12 numeric id order, with {@code colors} and
     * {@code smoke} at the end of their tier). {@code rot} and {@code mutation} are listed because they
     * are registered explosives, they just have no content.
     */
    public static final List<Identifier> ORDER = List.of(
            // tier 1
            ICBMConstants.id("condensed"),
            ICBMConstants.id("shrapnel"),
            ICBMConstants.id("incendiary"),
            ICBMConstants.id("debilitation"),
            ICBMConstants.id("chemical"),
            ICBMConstants.id("anvil"),
            ICBMConstants.id("repulsive"),
            ICBMConstants.id("attractive"),
            ICBMConstants.id("colors"),
            ICBMConstants.id("smoke"),
            // tier 2
            ICBMConstants.id("fragmentation"),
            ICBMConstants.id("contagious"),
            ICBMConstants.id("sonic"),
            ICBMConstants.id("breaching"),
            ICBMConstants.id("thermobaric"),
            // tier 3
            ICBMConstants.id("nuclear"),
            ICBMConstants.id("emp"),
            ICBMConstants.id("exothermic"),
            ICBMConstants.id("endothermic"),
            ICBMConstants.id("antigravitational"),
            ICBMConstants.id("ender"),
            // tier 4
            ICBMConstants.id("antimatter"),
            ICBMConstants.id("redmatter"),
            // no content, only blast
            ICBMConstants.id("rot"),
            ICBMConstants.id("mutation"));

    private ExplosiveForms() {}

    /**
     * Explosives enabled for a form, in {@link #ORDER}.
     *
     * @param form form to list
     * @return immutable list of explosive ids, empty if the registry is not initialized yet
     */
    public static List<Identifier> enabled(ExplosiveForm form)
    {
        return ORDER.stream().filter(contentRegistry(form)::isEnabled).toList();
    }

    /**
     * Content registry that owns the flat content of a form. Bomblets follow grenades, exactly like the
     * 1.12 {@code newEx} rules (tier ONE adds the grenade and the bomblet content).
     */
    private static IExplosiveContentRegistry contentRegistry(ExplosiveForm form)
    {
        return switch (form)
        {
            case BLOCK -> ICBMClassicAPI.EX_BLOCK_REGISTRY;
            case MISSILE -> ICBMClassicAPI.EX_MISSILE_REGISTRY;
            case GRENADE, BOMBLET -> ICBMClassicAPI.EX_GRENADE_REGISTRY;
            case BOMB_CART -> ICBMClassicAPI.EX_MINECART_REGISTRY;
        };
    }
}
