package icbm.classic.content.potion;

import icbm.classic.content.reg.ModEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Chemical poison gas effect (id {@code icbmclassic:toxin}).
 * <p>
 * 1.12 had no concrete "toxin" potion: the chemical gas ({@code BlastChemical}) applied
 * {@code ICBMClassic.chemicalPotion}, a {@link ContagiousPoison} with {@code isContagious = false}, which
 * ran the "non-contagious" branch of {@code ContagiousPoison#doPoisonEntity}: <i>POISON 30s</i>, NAUSEA 30s,
 * HUNGER 30s, WEAKNESS 35s and MINING_FATIGUE 60s (the {@code //TODO was toxin potion} note marks the poison
 * line as the one that was meant to become the toxin potion).
 * <p>
 * Port interpretation: {@code toxin} <b>is</b> that non-contagious set, packaged as a named effect so it can
 * be shown on the HUD and cured by the antidote. {@code onEffectStarted} delegates to the ported pair
 * ({@link ModEffects#CHEMICAL_POISON}) so the exact 1.12 vanilla set is applied, which keeps the POISON
 * periodic damage of the 1.12 gas. {@code onEffectStarted} (not a tick) is used on purpose: 26.3 calls it on
 * every {@code addEffect}, including a refresh, which matches 1.12 re-applying the whole set on every gas hit.
 * <p>
 * Colour {@code 0xCCCC00} is the 1.12 chemical gas particle colour (red 0.8, green 0.8, blue 0).
 */
public class ToxinEffect extends CustomPotion
{
    public ToxinEffect()
    {
        super(true, 0xCCCC00);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier)
    {
        //1.12 BlastChemical: ICBMClassic.chemicalPotion.poisonEntity(location.toPos(), entity)
        ModEffects.CHEMICAL_POISON.poisonEntity(mob, amplifier);
    }
}
