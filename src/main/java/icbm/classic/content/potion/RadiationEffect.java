package icbm.classic.content.potion;

import icbm.classic.config.blocks.ConfigBlocks;
import icbm.classic.content.reg.ModDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Radiation effect (id {@code icbmclassic:radiation}).
 * <p>
 * 1.12 had no concrete radiation potion (only the {@code potion.radiation} lang entry). The 1.12 radioactive
 * blocks applied the effect directly in {@code BlockRadioactive#tryDecayEntities}: on a decay tick each entity
 * in range rolled {@code decayEffectChance}, and if the protective armor rating was low enough took
 * {@code attackEntityFrom(damageSource, ConfigBlocks.radioactive.decayEffectDamage)} plus
 * {@code Wither} if {@code decayWitherDuration > 0}.
 * <p>
 * Port interpretation: {@code radiation} packages that repeated exposure as a periodic effect so the armor
 * rating only has to be checked once (when the block/gas applies the effect). The tick deals
 * {@link ConfigBlocks#radioactive} damage of the {@code radioactive_block} damage type every
 * {@link #DAMAGE_INTERVAL} ticks and adds 20 ticks of Wither when the config enables it. The 1.12 code
 * provided the wither duration but hard-coded 20 ticks in the {@code PotionEffect} (a 1.12 bug), that hard
 * coded value is kept on purpose.
 * <p>
 * Colour {@code 0x33CC33} is a green chosen for the radiation theme (1.12 had no colour for this effect).
 */
public class RadiationEffect extends CustomPotion
{
    /** Ticks between damage applications (1 second). */
    public static final int DAMAGE_INTERVAL = 20;

    public RadiationEffect()
    {
        super(true, 0x33CC33);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification)
    {
        return tickCount % DAMAGE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification)
    {
        //1.12 BlockRadioactive#tryDecayEntities: entity.attackEntityFrom(damageSource, ConfigBlocks.radioactive.decayEffectDamage)
        mob.hurtServer(level, ModDamageTypes.source(level, ModDamageTypes.RADIOACTIVE_BLOCK), ConfigBlocks.radioactive.decayEffectDamage);
        if (ConfigBlocks.radioactive.decayWitherDuration > 0)
        {
            //1.12: entity.addPotionEffect(new PotionEffect(MobEffects.WITHER, 20)) - the 1.12 hard coded 20 is kept
            mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 20));
        }
        return true;
    }
}
