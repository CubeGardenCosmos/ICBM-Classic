package icbm.classic.content.potion;

import icbm.classic.content.reg.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Contagious virus gas effect (id {@code icbmclassic:virus}).
 * <p>
 * 1.12 had no concrete "virus" potion: the contagious gas ({@code BlastContagious}) applied
 * {@code ICBMClassic.contagiousPotion}, a {@link ContagiousPoison} with {@code isContagious = true}
 * (amplifier 3), which ran the "contagious" branch of {@code ContagiousPoison#doPoisonEntity}:
 * <i>POISON 45s</i>, BLINDNESS 15s, HUNGER 30s, WEAKNESS 35s and MINING_FATIGUE 60s (the
 * {@code //TODO was contagious potion} note marks the poison line as the one that was meant to become the
 * contagious potion).
 * <p>
 * Port interpretation:
 * <ul>
 *     <li>{@code onEffectStarted} delegates the 1.12 set to {@link ModEffects#CONTAGIOUS_POISON}, so the
 *     vanilla effect set (including the POISON periodic damage) is identical to 1.12.</li>
 *     <li>The "contagious" half of the name is added as a spread: every
 *     {@link #SPREAD_INTERVAL} ticks the effect infects living entities within {@link #SPREAD_RADIUS} blocks
 *     that do not already carry the virus, for {@link #SPREAD_DURATION} ticks. 1.12 had no spread mechanic
 *     (the gas simply re-hit entities inside the cloud), so this is an OPEN interpretation: it was chosen
 *     because the effect name/type ("virus", isContagious) implies transmission and the parent brief asked
 *     the effect to carry "spread". It is bounded by radius, by the short infection duration and by skipping
 *     creative players, so it can not become a world wide plague.</li>
 * </ul>
 * Colour {@code 0x4CCC00} is the 1.12 contagious gas particle colour (red 0.3, green 0.8, blue 0).
 */
public class VirusEffect extends CustomPotion
{
    /** Ticks between spread attempts. */
    public static final int SPREAD_INTERVAL = 60;
    /** Radius (meters) the virus spreads over. */
    public static final double SPREAD_RADIUS = 2.5D;
    /** Duration given to a freshly infected entity. */
    public static final int SPREAD_DURATION = 10 * 20;

    public VirusEffect()
    {
        super(true, 0x4CCC00);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier)
    {
        //1.12 BlastContagious: ICBMClassic.contagiousPotion.poisonEntity(location.toPos(), entity, 3)
        ModEffects.CONTAGIOUS_POISON.poisonEntity(mob, amplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification)
    {
        return tickCount % SPREAD_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification)
    {
        final AABB bounds = mob.getBoundingBox().inflate(SPREAD_RADIUS);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != mob && entity.isAlive() && !entity.hasEffect(ModEffects.VIRUS)
                        && !(entity instanceof Player player && player.isCreative())))
        {
            other.addEffect(new MobEffectInstance(ModEffects.VIRUS, SPREAD_DURATION, amplification));
        }
        return true;
    }
}
