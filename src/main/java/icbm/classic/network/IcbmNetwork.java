package icbm.classic.network;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.network.lambda.PacketCodex;
import icbm.classic.lib.network.lambda.PacketCodexReg;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Transport layer of the mod: payload type registration and the codex entry points.
 *
 * 1.12 used a single Netty based {@code PacketManager} with an integer packet id per packet
 * ({@code lib/network/netty/**}); 26.3 registers custom payload types instead (porting-guide.md §5).
 */
public final class IcbmNetwork
{
    /**
     * Routes {@link #sendToServer(CodexPayload)} into the client source set. The client entry point injects
     * {@code ClientPlayNetworking::send}; a server (or a client before the entry point ran) has no sender.
     */
    private static Consumer<CodexPayload> clientSender;

    private IcbmNetwork()
    {
    }

    /**
     * Registers the payload types of this mod, called from {@code ICBMClassic#onInitialize}
     * (docs/architecture/overview.md §5).
     */
    public static void registerPayloads()
    {
        PayloadTypeRegistry.clientboundPlay().register(CodexPayload.S2C, CodexPayload.S2C_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CodexPayload.C2S, CodexPayload.C2S_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AirParticlePayload.TYPE, AirParticlePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlockExplosionFxPayload.TYPE, BlockExplosionFxPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CodexPayload.C2S, IcbmNetwork::handleServer);
    }

    /** Injected by the client entry point, see {@code icbm.classic.client.network.IcbmClientNetwork} */
    public static void setClientSender(Consumer<CodexPayload> sender)
    {
        clientSender = sender;
    }

    /** Sends a codex payload to the server. Dropped with a warning when this side has no client sender */
    public static void sendToServer(CodexPayload payload)
    {
        final Consumer<CodexPayload> sender = clientSender;
        if (sender == null)
        {
            ICBMClassic.logger().warn("Dropped codex({}) send to server, no client sender is registered", payload.codexId());
            return;
        }
        sender.accept(payload.asServerbound());
    }

    /** Sends a codex payload to a player */
    public static void sendToPlayer(CodexPayload payload, ServerPlayer player)
    {
        ServerPlayNetworking.send(player, payload.asClientbound());
    }

    /**
     * True when the codex may be received on the server from a client.
     *
     * Codexes define their direction with {@code fromClient()}/{@code fromServer()}: {@code fromClient()}
     * (1.12 {@code asServerOnly()}) sets {@link PacketCodex#isAllowServer()}, so only those codexes are
     * accepted from clients.
     */
    public static boolean canHandleFromClient(PacketCodex<?, ?> codex)
    {
        return codex != null && codex.isAllowServer();
    }

    /**
     * Server side receiver of {@link CodexPayload#C2S}.
     *
     * Fabric calls payload handlers on the game thread (verified: {@code ServerPlayNetworkAddon#schedule}
     * runs the handler through {@code MinecraftServer#execute}), so the body is validated and applied on the
     * server main thread. Nothing is thrown at the client: every failure is logged and dropped.
     */
    public static void handleServer(CodexPayload payload, ServerPlayNetworking.Context context)
    {
        final ServerPlayer player = context.player();
        final PacketCodex<?, ?> codex = PacketCodexReg.get(payload.codexId());
        if (codex == null)
        {
            ICBMClassic.logger().warn("Received codex({}) from player({}) but no codex is registered for it",
                payload.codexId(), player.getName().getString());
            return;
        }
        if (!canHandleFromClient(codex))
        {
            codex.logDebug(player.level(), null, "Received codex from client(" + player.getName().getString() + ") but codex is not allowed from clients");
            return;
        }

        final RegistryFriendlyByteBuf byteBuf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.body()), player.level().registryAccess());
        try
        {
            codex.handleFromClient(player, byteBuf);
        }
        catch (Exception e)
        {
            codex.logError(player.level(), null, "Failed to handle codex(" + payload.codexId() + ") from player(" + player.getName().getString() + ")", e);
        }
    }
}
