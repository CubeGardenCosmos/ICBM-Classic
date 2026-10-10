package icbm.classic.network;

import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns a colored air particle on the client.
 *
 * 1.12: {@code PacketSpawnAirParticle} (dimension id + position + velocity + color + size + life time).
 * The dimension is an id in 26.3, so the packet carries the dimension identifier. Particle types are not
 * ported yet, the receiving side only logs until S4.07 completes the effect.
 */
public record AirParticlePayload(Identifier dimension, double posX, double posY, double posZ,
                                 double v, double v1, double v2,
                                 float red, float green, float blue, float scale, int ticksToLive) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AirParticlePayload> TYPE = new CustomPacketPayload.Type<>(ICBMConstants.id("air_particle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AirParticlePayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, AirParticlePayload::dimension,
        ByteBufCodecs.DOUBLE, AirParticlePayload::posX,
        ByteBufCodecs.DOUBLE, AirParticlePayload::posY,
        ByteBufCodecs.DOUBLE, AirParticlePayload::posZ,
        ByteBufCodecs.DOUBLE, AirParticlePayload::v,
        ByteBufCodecs.DOUBLE, AirParticlePayload::v1,
        ByteBufCodecs.DOUBLE, AirParticlePayload::v2,
        ByteBufCodecs.FLOAT, AirParticlePayload::red,
        ByteBufCodecs.FLOAT, AirParticlePayload::green,
        ByteBufCodecs.FLOAT, AirParticlePayload::blue,
        ByteBufCodecs.FLOAT, AirParticlePayload::scale,
        ByteBufCodecs.VAR_INT, AirParticlePayload::ticksToLive,
        AirParticlePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /**
     * 1.12: {@code PacketSpawnAirParticle.sendToAllClients(World, ...)} sent to every player within 256
     * blocks in the same dimension, which is {@link PlayerLookup#around}.
     */
    public static void sendToAllClients(ServerLevel level, double x, double y, double z,
                                        double v, double v1, double v2,
                                        float red, float green, float blue, float scale, int ticksToLive)
    {
        final AirParticlePayload payload = new AirParticlePayload(level.dimension().identifier(), x, y, z, v, v1, v2, red, green, blue, scale, ticksToLive);
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), 256))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
