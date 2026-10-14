package icbm.classic.content.blast.gas;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.blast.BlastMutation;
import icbm.classic.content.potion.CustomPotionEffect;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEffects;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Vec3i;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Contagious (viral) gas. Applies {@code virus} and, once enough gas has built up on a non-mutated entity,
 * contagious damage.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>1.12 {@code CONTAGIOUS_DAMAGE = new DamageSource("icbm.contagious")} -> {@link ModDamageTypes#CONTAGIOUS},
 *     built per use because a 26.3 {@link DamageSource} needs a level's damage type registry.</li>
 *     <li>{@code isEntityInvulnerable} -> {@code LivingEntity#isInvulnerableTo(ServerLevel, DamageSource)}.</li>
 *     <li>{@code attackEntityFrom} -> {@code Entity#hurtServer(ServerLevel, DamageSource, float)}.</li>
 *     <li>The 1.12 call {@code ICBMClassic.contagiousPotion.poisonEntity(location.toPos(), entity, 3)} (the
 *     {@code isContagious = true} branch, amplifier 3) is replaced by applying the {@code virus} effect at
 *     amplifier 3, which carries the exact 1.12 contagious set (see {@code VirusEffect}) plus a bounded
 *     spread. Duration 45s is the 1.12 contagious set duration.</li>
 * </ul>
 */
public class BlastContagious extends BlastGasBase
{
    public static final float red = 0.3f, green = 0.8f, blue = 0;

    /** Duration given to the {@code virus} effect, matches the 1.12 45s contagious set. */
    public static final int VIRUS_DURATION = 45 * 20;

    @Setter @Accessors(chain = true)
    private int toxicityBuildup = 10; // tick rate is 5
    @Setter @Accessors(chain = true)
    private float toxicityScale = 0.05f;
    @Setter @Accessors(chain = true)
    private float toxicityMinDamage = 1f;

    @Override
    protected boolean canEffectEntities()
    {
        return true;
    }

    @Override
    protected boolean canGasEffect(LivingEntity entity)
    {
        //1.12: super.canGasEffect(entity) && !entity.isEntityInvulnerable(CONTAGIOUS_DAMAGE)
        return super.canGasEffect(entity) && !entity.isInvulnerableTo(this.level(), contagiousDamage());
    }

    @Override
    protected float minGasProtection() {
        return ConfigMain.protectiveArmor.minProtectionViralGas;
    }

    @Override
    protected void applyEffect(final LivingEntity entity, final int hitCount)
    {
        //1.12: ICBMClassic.contagiousPotion.poisonEntity(location.toPos(), entity, 3)
        entity.addEffect(new CustomPotionEffect(ModEffects.VIRUS, VIRUS_DURATION, 3));

        //Apply damage to non-mutated entities if toxin level is high enough
        if (!BlastMutation.applyMutationEffect(entity) && hitCount > toxicityBuildup)
        {
            entity.hurtServer(this.level(), contagiousDamage(), Math.max(toxicityMinDamage, hitCount * toxicityScale));
        }
    }

    /** 1.12 {@code CONTAGIOUS_DAMAGE}, rebuilt from the level registries on every use. */
    private DamageSource contagiousDamage()
    {
        return ModDamageTypes.source(world(), ModDamageTypes.CONTAGIOUS);
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
