package icbm.classic.content.potion;

import icbm.classic.ICBMConstants;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Cold / frostbite effect (id {@code icbmclassic:frost_bite}).
 * <p>
 * 1.12 had no concrete "frostBite" potion (only the {@code potion.frostBite} lang entry). Its intended user
 * is visible in {@code BlastEndothermic}: the cold blast applied
 * {@code new PotionEffect(MobEffects.POISON, 10 * 20, 2)}, {@code MINING_FATIGUE 120s amplifier 2} and
 * {@code SLOWNESS 120s amplifier 4}, with the actual custom potion line left commented out as
 * {@code //entity.addPotionEffect(new CustomPotionEffect(PoisonFrostBite.INSTANCE, 60 * 20, 1, null));}
 * (legacy {@code BlastEndothermic.java:141-144}).
 * <p>
 * Port interpretation: {@code frost_bite} carries the frozen/slowed half of that set.
 * <ul>
 *     <li>The SLOWNESS part is a movement speed attribute modifier, which is how 26.3 expresses movement
 *     effects (vanilla {@code MobEffects.SLOWNESS} is declared with exactly this modifier), value
 *     {@code -0.15} per level matching vanilla slowness.</li>
 *     <li>The MINING_FATIGUE part is applied on start (120s, amplifier of this effect), copied from the 1.12
 *     endothermic set (legacy line 143).</li>
 *     <li>The POISON line of the 1.12 set is <b>not</b> reproduced: it was the stand-in for the missing
 *     frostbite potion and this effect is now that potion, so keeping it would double the cold damage. This
 *     is an OPEN interpretation.</li>
 * </ul>
 * Colour {@code 0x99CCFF} is a light blue chosen for the cold theme (1.12 had no colour for this effect).
 */
public class FrostBiteEffect extends CustomPotion
{
    public FrostBiteEffect()
    {
        super(true, 0x99CCFF);
        //1.12 BlastEndothermic: MobEffects.SLOWNESS, 120 * 20, 4 -> 26.3 uses the vanilla slowness attribute modifier
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ICBMConstants.id("effect.frost_bite"), -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier)
    {
        //1.12 BlastEndothermic: entity.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 120 * 20, 2))
        mob.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120 * 20, amplifier));
    }
}
