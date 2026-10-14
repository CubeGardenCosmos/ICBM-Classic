package icbm.classic.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.client.particle.SpriteSet;

/**
 * ICBM smoke particle, the 26.3 port of 1.12 {@code client/fx/ParticleSmokeICBM}, {@code ParticleAirICBM}
 * and {@code ParticleLauncherSmoke}.
 * <p>
 * All three 1.12 classes were vanilla {@code ParticleSmokeNormal} with only two additions: a colour setter
 * that multiplies a random variant into the colour, and an age setter. 26.3's {@code SmokeParticle} is the
 * same smoke without the 1.12 upward self-acceleration, which was the only difference of
 * {@code ParticleAirICBM}, and the collision exemption of {@code ParticleLauncherSmoke} only covered the
 * launcher blocks that arrive with S5, so one class covers all three (see the port ledger).
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class ParticleIcbmSmoke extends SmokeParticle
{
    public ParticleIcbmSmoke(ClientLevel level, double x, double y, double z,
                             double motionX, double motionY, double motionZ,
                             float scale, SpriteSet sprites)
    {
        super(level, x, y, z, motionX, motionY, motionZ, scale, sprites);
    }

    /**
     * 1.12 {@code ParticleSmokeICBM#setAge}: sets the maximum age (26.3 {@code lifetime}).
     */
    public ParticleIcbmSmoke setAge(int age)
    {
        this.lifetime = Math.max(1, age);
        return this;
    }

    /**
     * 1.12 {@code ParticleSmokeICBM#setColor}: replaces the colour and optionally multiplies a random
     * variant (0..0.9) into every channel, which is what gives the smoke its darker/lighter puffs.
     */
    public ParticleIcbmSmoke setColor(float red, float green, float blue, boolean addColorVariant)
    {
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;

        if (addColorVariant)
        {
            final float colourVariant = (float) (Math.random() * 0.90000001192092896D);
            this.rCol *= colourVariant;
            this.gCol *= colourVariant;
            this.bCol *= colourVariant;
        }
        return this;
    }
}
