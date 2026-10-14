package icbm.classic.content.potion;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The vanilla effect sets used by the chemical and contagious gases.
 * <p>
 * 1.12 created two instances in {@code ICBMClassic}: {@code chemicalPotion = new ContagiousPoison("Chemical",
 * 0, false)} and {@code contagiousPotion = new ContagiousPoison("Contagious", 1, true)}. The {@code id}
 * argument is gone (26.3 has no numeric ids) and the two instances now live in
 * {@link icbm.classic.content.reg.ModEffects} as {@code CHEMICAL_POISON}/{@code CONTAGIOUS_POISON} because the
 * 26.3 {@code ICBMClassic} entry class only wires registrations.
 * <p>
 * @deprecated remove at some point and apply potion effects directly (1.12 note kept).
 * <p>
 * 26.3 port: {@code addPotionEffect(new CustomPotionEffect(...))} becomes
 * {@code addEffect(new CustomPotionEffect(holder, duration, amplifier))}. The effect sets, durations and the
 * {@code isContagious} branch are copied verbatim from the 1.12 body (the {@code //TODO was ... potion}
 * author notes are kept with them).
 */
@Deprecated
public class ContagiousPoison extends Poison
{
    /** 1.12 {@code ICBMClassic.chemicalPotion} name. */
    public static final String CHEMICAL = "Chemical";
    /** 1.12 {@code ICBMClassic.contagiousPotion} name. */
    public static final String CONTAGIOUS = "Contagious";

    private final boolean isContagious;

    public ContagiousPoison(String name, boolean isContagious)
    {
        super(name);
        this.isContagious = isContagious;
    }

    @Override
    protected void doPoisonEntity(LivingEntity entity, int amplifier)
    {
        if (this.isContagious)
        {
            //1.12: entity.addPotionEffect(new CustomPotionEffect(MobEffects.POISON, 45 * 20, amplifier, null))
            entity.addEffect(new CustomPotionEffect(MobEffects.POISON, 45 * 20, amplifier)); //TODO was contagious potion
            entity.addEffect(new CustomPotionEffect(MobEffects.BLINDNESS, 15 * 20, amplifier));
        }
        else
        {
            //1.12: entity.addPotionEffect(new CustomPotionEffect(MobEffects.POISON, 30 * 20, amplifier, null))
            entity.addEffect(new CustomPotionEffect(MobEffects.POISON, 30 * 20, amplifier)); //TODO was toxin potion
            entity.addEffect(new CustomPotionEffect(MobEffects.NAUSEA, 30 * 20, amplifier));
        }

        entity.addEffect(new CustomPotionEffect(MobEffects.HUNGER, 30 * 20, amplifier));
        entity.addEffect(new CustomPotionEffect(MobEffects.WEAKNESS, 35 * 20, amplifier));
        entity.addEffect(new CustomPotionEffect(MobEffects.MINING_FATIGUE, 60 * 20, amplifier));
    }
}
