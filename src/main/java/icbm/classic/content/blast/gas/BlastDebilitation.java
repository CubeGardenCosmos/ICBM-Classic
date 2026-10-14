package icbm.classic.content.blast.gas;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.potion.CustomPotionEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Vec3i;

/**
 * Created by Robin Seifert on 4/1/2022.
 * <p>
 * 26.3 port: {@code addPotionEffect(new CustomPotionEffect(...))} -> {@code addEffect(...)}. The 1.12 effect
 * set is kept verbatim; no ICBM custom effect maps to the debilitation gas (it applies the vanilla set), so it
 * still applies plain vanilla {@link MobEffects} holders.
 */
public class BlastDebilitation extends BlastGasBase
{
    public static final int DURATION = 20 * 30; //TODO move to config

    public static final float red = 1f, green = 1f, blue = 1f;

    public BlastDebilitation()
    {
        setDuration(DURATION);
    }

    @Override
    protected boolean canEffectEntities()
    {
        return true;
    }

    @Override
    protected float minGasProtection() {
        return ConfigMain.protectiveArmor.minProtectionDebilitationGas;
    }

    @Override
    protected void applyEffect(final LivingEntity entity, final int hitCount)
    {
        entity.addEffect(new CustomPotionEffect(MobEffects.POISON, 18 * 20, 0)); //TODO scale and make configurable
        entity.addEffect(new CustomPotionEffect(MobEffects.MINING_FATIGUE, 20 * 60, 0));
        entity.addEffect(new CustomPotionEffect(MobEffects.SLOWNESS, 20 * 60, 2));
    }

    @Override
    protected float getParticleColorRed(final Vec3i pos)
    {
        return red;
    }

    @Override
    protected float getParticleColorGreen(final Vec3i pos)
    {
        return green;
    }

    @Override
    protected float getParticleColorBlue(final Vec3i pos)
    {
        return blue;
    }
}
