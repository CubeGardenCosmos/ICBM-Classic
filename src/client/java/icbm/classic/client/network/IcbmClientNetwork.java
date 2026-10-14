package icbm.classic.client.network;

import icbm.classic.ICBMClassic;
import icbm.classic.client.particle.ModParticlesClient;
import icbm.classic.lib.network.lambda.PacketCodex;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import icbm.classic.network.AirParticlePayload;
import icbm.classic.network.BlockExplosionFxPayload;
import icbm.classic.network.CodexPayload;
import icbm.classic.network.IcbmNetwork;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Client side of the mod transport layer.
 *
 * Registers the client receivers for the payloads defined in {@link IcbmNetwork} and injects the client
 * sender used by {@link IcbmNetwork#sendToServer(CodexPayload)} (the main source set can not reference
 * client classes).
 */
public final class IcbmClientNetwork
{
    private IcbmClientNetwork()
    {
    }

    /** Called from {@code ICBMClassicClient#onInitializeClient} */
    public static void register()
    {
        IcbmNetwork.setClientSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(CodexPayload.S2C, IcbmClientNetwork::handleCodex);
        ClientPlayNetworking.registerGlobalReceiver(AirParticlePayload.TYPE, IcbmClientNetwork::handleAirParticle);
        ClientPlayNetworking.registerGlobalReceiver(BlockExplosionFxPayload.TYPE, IcbmClientNetwork::handleBlockExplosion);
    }

    /**
     * Receives a codex from the server.
     *
     * Fabric calls payload handlers on the game thread (verified: {@code ClientCommonNetworkAddon#schedule}
     * runs the handler through {@code Minecraft#execute}), the extra scheduling matches the 1.12
     * {@code addScheduledTask} behaviour and keeps the body readable after the payload is dropped.
     */
    private static void handleCodex(CodexPayload payload, ClientPlayNetworking.Context context)
    {
        context.client().execute(() -> applyCodex(payload, context));
    }

    private static void applyCodex(CodexPayload payload, ClientPlayNetworking.Context context)
    {
        final ClientLevel level = context.client().level;
        if (level == null)
        {
            ICBMClassic.logger().debug("Dropped codex({}) from server, no level is loaded on the client", payload.codexId());
            return;
        }
        final PacketCodex<?, ?> codex = PacketCodexReg.get(payload.codexId());
        if (codex == null)
        {
            ICBMClassic.logger().warn("Received codex({}) from server but no codex is registered for it", payload.codexId());
            return;
        }
        if (!codex.isAllowClient())
        {
            codex.logDebug(level, null, "Received codex from server but codex is not allowed on clients");
            return;
        }

        final RegistryFriendlyByteBuf byteBuf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.body()), level.registryAccess());
        try
        {
            codex.handleFromServer(level, context.player(), byteBuf);
        }
        catch (Exception e)
        {
            codex.logError(level, null, "Failed to handle codex(" + payload.codexId() + ") from server", e);
        }
    }

    private static void handleAirParticle(AirParticlePayload payload, ClientPlayNetworking.Context context)
    {
        //1.12 PacketSpawnAirParticle: spawnAirParticle(player.world, pos, motion, colour, scale, ticksToLive)
        final ClientLevel level = context.client().level;
        if (level == null || !level.dimension().identifier().equals(payload.dimension()))
        {
            return;
        }

        ModParticlesClient.spawnSmoke(level,
                payload.posX(), payload.posY(), payload.posZ(),
                payload.v(), payload.v1(), payload.v2(),
                payload.red(), payload.green(), payload.blue(),
                payload.scale(), payload.ticksToLive());
    }

    private static void handleBlockExplosion(BlockExplosionFxPayload payload, ClientPlayNetworking.Context context)
    {
        //1.12 PacketSpawnBlockExplosion: spawnExplosionParticles(player.world, source, blastScale, blockPos)
        final ClientLevel level = context.client().level;
        if (level == null || !level.dimension().identifier().equals(payload.dimension()))
        {
            return;
        }

        ModParticlesClient.spawnExplosionParticles(level,
                payload.sourceX(), payload.sourceY(), payload.sourceZ(),
                payload.blastScale(), payload.blockPos());
    }
}
