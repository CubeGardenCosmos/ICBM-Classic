package icbm.classic.client.particle;

import icbm.classic.config.ConfigClient;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.reg.ModParticles;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Client side particle work of S4.07.
 * <p>
 * 1.12 pushed the particle objects straight into {@code EffectRenderer} from {@code client/ClientProxy}
 * ({@code spawnSmoke}, {@code spawnAirParticle}, {@code spawnExplosionParticles}, {@code spawnMissileSmoke},
 * {@code spawnPadSmoke}). 26.3 registers a provider for the particle type and keeps one spawn helper per 1.12
 * entry point; the two payloads ({@code AirParticlePayload}, {@code BlockExplosionFxPayload}) and the missile
 * effects call them.
 * <p>
 * The 1.12 missile smoke ran from the entity update on the client. 26.3 entities do not tick any client code
 * of their own and the renderer runs per frame, so the trail is produced from the client tick event once per
 * tick per missile, which is the same rate 1.12 used.
 */
public final class ModParticlesClient
{
    /**
     * Sprites of the ICBM smoke type, captured when the particle engine builds the provider (resource reload).
     * A null value means the client has no loaded resource pack yet, spawning is skipped until then.
     */
    private static @Nullable SpriteSet smokeSprites;

    private ModParticlesClient() {}

    /** Called from {@code ICBMClassicClient#onInitializeClient()}. */
    public static void register()
    {
        ParticleProviderRegistry.getInstance().register(ModParticles.SMOKE_ICBM, (FabricSpriteSet sprites) ->
        {
            smokeSprites = sprites;
            return (type, level, x, y, z, motionX, motionY, motionZ, random) ->
                    new ParticleIcbmSmoke(level, x, y, z, motionX, motionY, motionZ, 1.0F, sprites);
        });

        ClientTickEvents.END_CLIENT_TICK.register(ModParticlesClient::tickMissileEffects);
    }

    /**
     * 1.12 {@code ClientProxy#spawnSmoke} / {@code #spawnAirParticle}: coloured smoke with a fixed age.
     *
     * @return the spawned particle, or null when there is no client level or no sprite set yet
     */
    public static @Nullable ParticleIcbmSmoke spawnSmoke(ClientLevel level, double x, double y, double z,
                                                         double motionX, double motionY, double motionZ,
                                                         float red, float green, float blue,
                                                         float scale, int ticksToLive)
    {
        final SpriteSet sprites = smokeSprites;
        if (level == null || sprites == null)
        {
            return null;
        }

        final ParticleIcbmSmoke particle = new ParticleIcbmSmoke(level, x, y, z, motionX, motionY, motionZ, scale, sprites)
                .setAge(ticksToLive)
                .setColor(red, green, blue, true);
        Minecraft.getInstance().particleEngine.add(particle);
        return particle;
    }

    /**
     * 1.12 {@code ClientProxy#spawnExplosionParticles}: two vanilla particles thrown away from the destroyed
     * block (26.3 maps {@code EXPLOSION_NORMAL} to {@link ParticleTypes#POOF} and {@code SMOKE_NORMAL} to
     * {@link ParticleTypes#SMOKE}).
     */
    public static void spawnExplosionParticles(ClientLevel level, double sourceX, double sourceY, double sourceZ,
                                               double blastScale, BlockPos blockPos)
    {
        if (level == null)
        {
            return;
        }

        final RandomSource random = level.getRandom();

        //Random position near destroyed block
        final double particleX = blockPos.getX() + random.nextFloat();
        final double particleY = blockPos.getY() + random.nextFloat();
        final double particleZ = blockPos.getZ() + random.nextFloat();

        //Get delta from center of blast so particles move out
        double particleMotionX = particleX - sourceX;
        double particleMotionY = particleY - sourceY;
        double particleMotionZ = particleZ - sourceZ;

        //Normalize motion vector
        final double speed = Math.sqrt(particleMotionX * particleMotionX + particleMotionY * particleMotionY + particleMotionZ * particleMotionZ);
        particleMotionX /= speed;
        particleMotionY /= speed;
        particleMotionZ /= speed;

        //Give motion vector a randomized multiplier based on blast size
        double multiplier = 0.5D / (speed / blastScale + 0.1D);
        multiplier *= (random.nextFloat() * random.nextFloat() + 0.3F);
        particleMotionX *= multiplier;
        particleMotionY *= multiplier;
        particleMotionZ *= multiplier;

        level.addParticle(ParticleTypes.POOF,
                (particleX + sourceX) / 2.0D,
                (particleY + sourceY) / 2.0D,
                (particleZ + sourceZ) / 2.0D,
                particleMotionX, particleMotionY, particleMotionZ);
        level.addParticle(ParticleTypes.SMOKE, particleX, particleY, particleZ, particleMotionX, particleMotionY, particleMotionZ);
    }

    /**
     * 1.12 spawned the missile effects from {@code EntityMissile#updateMotion} (engine trail, through
     * {@code ClientProxy#spawnMissileSmoke}) and {@code WarmupFlightLogic#onEntityTick} (launch pad smoke,
     * through {@code ClientProxy#spawnPadSmoke}). Both were client only. 26.3 has no client entity update for
     * the missile, so the effect runs from the client tick and picks the variant from the missile motion: a
     * missile that is not moving is still on the pad (its warmup logic holds it in place), everything else is
     * in flight.
     */
    private static void tickMissileEffects(Minecraft client)
    {
        final ClientLevel level = client.level;
        if (level == null || (!ConfigClient.MISSILE_ENGINE_SMOKE && !ConfigClient.MISSILE_LAUNCH_SMOKE))
        {
            return;
        }

        for (final Entity entity : level.entitiesForRendering())
        {
            if (!(entity instanceof EntityMissile missile) || !missile.isAlive())
            {
                continue;
            }

            if (missile.getDeltaMovement().lengthSqr() < 0.01D)
            {
                if (ConfigClient.MISSILE_LAUNCH_SMOKE)
                {
                    spawnPadSmoke(level, missile);
                }
            }
            else if (ConfigClient.MISSILE_ENGINE_SMOKE)
            {
                spawnMissileSmoke(level, missile);
            }
        }
    }

    /** 1.12 {@code ClientProxy#spawnMissileSmoke}: trail of 10 puffs behind the missile. */
    private static void spawnMissileSmoke(ClientLevel level, EntityMissile missile)
    {
        final Vec3 motion = missile.getDeltaMovement();

        // The distance of the smoke relative to the missile
        final double distance = -1.2F;
        // The delta Y of the smoke
        final double deltaY = Math.sin(Math.toRadians(missile.getXRot())) * distance;
        // The horizontal distance of the smoke
        final double deltaHorizontal = Math.cos(Math.toRadians(missile.getXRot())) * distance;
        // The delta X and Z
        double x = missile.getX() + Math.sin(Math.toRadians(missile.getYRot())) * deltaHorizontal;
        double y = missile.getY() + deltaY;
        double z = missile.getZ() + Math.cos(Math.toRadians(missile.getYRot())) * deltaHorizontal;

        final int colour = missile.getRenderSmokeColor();
        final float red = ((colour >> 16) & 0xFF) / 255.0F;
        final float green = ((colour >> 8) & 0xFF) / 255.0F;
        final float blue = (colour & 0xFF) / 255.0F;

        for (int i = 0; i < 10; i++)
        {
            spawnSmoke(level, x, y, z,
                    -motion.x * 0.5, -motion.y * 0.5, -motion.z * 0.5,
                    red, green, blue,
                    (float) Math.max(1D, 6D * (1 / (1 + missile.getY() / 100))), 100);
            // Shrink the 10 positions into a small cone
            x *= 1 - 0.025 * Math.random();
            y *= 1 - 0.025 * Math.random();
            z *= 1 - 0.025 * Math.random();
        }
    }

    /** 1.12 {@code ClientProxy#spawnPadSmoke}: 10 white puffs blown down in a cone under the missile. */
    private static void spawnPadSmoke(ClientLevel level, EntityMissile missile)
    {
        final RandomSource random = level.getRandom();

        final double posX = missile.getX();
        final double posY = missile.getY() - 1.2; //TODO get missile height from type
        final double posZ = missile.getZ();

        for (int smokeCount = 0; smokeCount < 10; smokeCount++)
        {
            //Randomize flight direction down in a cone
            final double velocityX = (random.nextFloat() - random.nextFloat()) * 0.3;
            final double velocityY = 1 - (random.nextFloat() * 0.5);
            final double velocityZ = (random.nextFloat() - random.nextFloat()) * 0.3;

            spawnSmoke(level, posX, posY, posZ, velocityX, -velocityY, velocityZ,
                    1, 1, 1, 1 + 2 * random.nextFloat(), 180);
        }
    }
}
