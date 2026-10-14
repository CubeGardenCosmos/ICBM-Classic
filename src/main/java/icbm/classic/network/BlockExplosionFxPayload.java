package icbm.classic.network;

import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Spawns the explosion effect of a block on the client.
 *
 * 1.12: {@code PacketSpawnBlockExplosion} (dimension id + explosion source + blast scale + block position).
 * The dimension is an id in 26.3, so the packet carries the dimension identifier. The client receiver spawns
 * the vanilla poof/smoke particles (S4.07, {@code ModParticlesClient#spawnExplosionParticles}).
 */
public record BlockExplosionFxPayload(Identifier dimension, double sourceX, double sourceY, double sourceZ,
                                      double blastScale, BlockPos blockPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<BlockExplosionFxPayload> TYPE = new CustomPacketPayload.Type<>(ICBMConstants.id("block_explosion_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockExplosionFxPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC, BlockExplosionFxPayload::dimension,
        ByteBufCodecs.DOUBLE, BlockExplosionFxPayload::sourceX,
        ByteBufCodecs.DOUBLE, BlockExplosionFxPayload::sourceY,
        ByteBufCodecs.DOUBLE, BlockExplosionFxPayload::sourceZ,
        ByteBufCodecs.DOUBLE, BlockExplosionFxPayload::blastScale,
        BlockPos.STREAM_CODEC, BlockExplosionFxPayload::blockPos,
        BlockExplosionFxPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /**
     * 1.12: {@code PacketSpawnBlockExplosion.sendToAllClients(World, ...)} sent to every player within 256
     * blocks of the block in the same dimension, which is {@link PlayerLookup#around}.
     */
    public static void sendToAllClients(ServerLevel level, double sourceX, double sourceY, double sourceZ, double blastScale, BlockPos blockPos)
    {
        final BlockExplosionFxPayload payload = new BlockExplosionFxPayload(level.dimension().identifier(), sourceX, sourceY, sourceZ, blastScale, blockPos);
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(sourceX, sourceY, sourceZ), 256))
        {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
