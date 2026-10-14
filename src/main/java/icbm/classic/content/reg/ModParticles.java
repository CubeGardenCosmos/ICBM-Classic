package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Particle type registration, see {@code docs/architecture/registry-ids.md} §7.
 * <p>
 * 1.12 had no particle registry: the four client classes in {@code client/fx} were constructed directly and
 * pushed into {@code EffectRenderer}. 26.3 registers a type so server side code and data packs can name the
 * particle, and the client only draws it through a registered {@code ParticleProvider}. The behaviour of the
 * 1.12 classes is covered by the single {@link #SMOKE_ICBM} type (see the port ledger for the class by class
 * mapping):
 * <ul>
 *     <li>{@code ParticleSmokeICBM}, {@code ParticleAirICBM}, {@code ParticleLauncherSmoke} were all "vanilla
 *     smoke with a colour/age override". 26.3's vanilla smoke does not accelerate upwards on its own, which
 *     was the only difference of {@code ParticleAirICBM}, and {@code ParticleLauncherSmoke} only differed in
 *     the collision exemption of the launcher blocks, which arrive with S5.</li>
 *     <li>{@code FXAntimatterParticle} and {@code ParticleCollisionLogic} had no caller in 1.12 and are not
 *     ported.</li>
 * </ul>
 */
public final class ModParticles
{
    /** Smoke particle of every ICBM effect (missile engine trail, launch pad, gas clouds). */
    public static SimpleParticleType SMOKE_ICBM;

    private ModParticles() {}

    /** Registers the particle types. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        SMOKE_ICBM = register("smoke_icbm");
    }

    private static SimpleParticleType register(String id)
    {
        // The SimpleParticleType constructor is protected, FabricParticleTypes#simple is the supported way
        final ParticleType<SimpleParticleType> type = FabricParticleTypes.simple(false);
        return (SimpleParticleType) Registry.register(BuiltInRegistries.PARTICLE_TYPE, ICBMConstants.id(id), type);
    }
}
