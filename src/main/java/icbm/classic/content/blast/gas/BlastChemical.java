package icbm.classic.content.blast.gas;

import icbm.classic.config.ConfigMain;
import icbm.classic.content.potion.CustomPotionEffect;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEffects;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Vec3i;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Chemical gas. Applies {@code toxin} and, once enough gas has built up on the entity, chemical damage.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>1.12 {@code CHEMICAL_DAMAGE = new DamageSource("icbm.chemical")} -> the registered damage type
 *     {@link ModDamageTypes#CHEMICAL}; a 26.3 {@link DamageSource} needs a level's damage type registry, so it
 *     is built per use instead of being a static constant.</li>
 *     <li>{@code entity.isEntityInvulnerable(CHEMICAL_DAMAGE)} -> {@code LivingEntity#isInvulnerableTo(ServerLevel, DamageSource)}
 *     (26.3 requires the level argument).</li>
 *     <li>{@code attackEntityFrom(...)} -> {@code Entity#hurtServer(ServerLevel, DamageSource, float)}.</li>
 *     <li>The 1.12 poison call {@code ICBMClassic.chemicalPotion.poisonEntity(location.toPos(), entity)} is
 *     replaced by applying the {@code toxin} effect, which carries the exact 1.12 vanilla set (see
 *     {@code ToxinEffect}). Duration 30s is the 1.12 chemical set duration.</li>
 * </ul>
 */
public class BlastChemical extends BlastGasBase
{
    public static final float red = 0.8f, green = 0.8f, blue = 0;

    /** Duration given to the {@code toxin} effect, matches the 1.12 30s chemical set. */
    public static final int TOXIN_DURATION = 30 * 20;

    @Setter @Accessors(chain = true)
    private int toxicityBuildup = 20; // tick rate is 5
    @Setter @Accessors(chain = true)
    private float toxicityScale = 0.1f;
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
        //1.12: super.canGasEffect(entity) && !entity.isEntityInvulnerable(CHEMICAL_DAMAGE)
        return super.canGasEffect(entity) && !entity.isInvulnerableTo(this.level(), chemicalDamage());
    }

    @Override
    protected float minGasProtection() {
        return ConfigMain.protectiveArmor.minProtectionChemicalGas;
    }

    @Override
    protected void applyEffect(final LivingEntity entity, final int hitCount)
    {
        //1.12: ICBMClassic.chemicalPotion.poisonEntity(location.toPos(), entity)
        entity.addEffect(new CustomPotionEffect(ModEffects.TOXIN, TOXIN_DURATION, 0));
        if (hitCount > toxicityBuildup)
        {
            // TODO https://builtbroken.codecks.io/decks/14-icbm-backlog/card/1aq-rework-gas-weapons
            entity.hurtServer(this.level(), chemicalDamage(), Math.max(toxicityMinDamage, hitCount * toxicityScale));
        }
    }

    /** 1.12 {@code CHEMICAL_DAMAGE}, rebuilt from the level registries on every use. */
    private DamageSource chemicalDamage()
    {
        return ModDamageTypes.source(world(), ModDamageTypes.CHEMICAL);
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
