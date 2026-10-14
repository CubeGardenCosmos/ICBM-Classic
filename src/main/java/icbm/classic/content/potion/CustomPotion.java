package icbm.classic.content.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Base class of the ICBM custom effects.
 * <p>
 * 1.12: {@code CustomPotion extends Potion} with a constructor taking
 * {@code (boolean isBadEffect, int color, int id, String name)}; the constructor registered the potion into the
 * static potion registry under a numeric id and set the translation key to {@code "potion." + name}. It was
 * abstract and never subclassed in the 1.12 tree (only referenced by the unused {@code CustomPotionEffect}).
 * <p>
 * 26.3 port:
 * <ul>
 *     <li>{@code Potion} is {@code MobEffect} and bad/good is {@link MobEffectCategory}
 *     (HARMFUL/BENEFICIAL).</li>
 *     <li>Numeric ids no longer exist. Registration happens in {@link icbm.classic.content.reg.ModEffects}
 *     through {@code Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier, effect)} and the
 *     description id is derived from the registry key ({@code effect.icbmclassic.<name>}), so the 1.12
 *     {@code "potion." + name} key and the {@code id}/{@code name} constructor arguments are gone.</li>
 *     <li>{@code setIconIndex}/{@code setEffectiveness} no longer exist in 26.3 and were dropped with them.</li>
 * </ul>
 */
public abstract class CustomPotion extends MobEffect
{
    /**
     * @param isBadEffect true for a harmful effect, matching the 1.12 flag
     * @param color       rgb colour used for the effect icon/particles
     */
    public CustomPotion(boolean isBadEffect, int color)
    {
        super(isBadEffect ? MobEffectCategory.HARMFUL : MobEffectCategory.BENEFICIAL, color);
    }
}
