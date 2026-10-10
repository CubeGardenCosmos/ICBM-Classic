package icbm.classic.client.network;

import icbm.classic.ICBMClassic;
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
        // Particle types are not ported yet, S4.07 completes the effect (see docs/implementation/port-ledger.md)
        ICBMClassic.logger().debug("Received air particle payload in dimension({}) at {}x {}y {}z, particle rendering is done in S4.07",
            payload.dimension(), payload.posX(), payload.posY(), payload.posZ());
    }

    private static void handleBlockExplosion(BlockExplosionFxPayload payload, ClientPlayNetworking.Context context)
    {
        // Particle types are not ported yet, S4.07 completes the effect (see docs/implementation/port-ledger.md)
        ICBMClassic.logger().debug("Received block explosion payload in dimension({}) at {}x {}y {}z for block {}, particle rendering is done in S4.07",
            payload.dimension(), payload.sourceX(), payload.sourceY(), payload.sourceZ(), payload.blockPos());
    }
}
