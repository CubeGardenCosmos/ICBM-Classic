package icbm.classic.network;

import icbm.classic.ICBMConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload carrying a {@link icbm.classic.lib.network.lambda.PacketCodex} body between client and server.
 *
 * <p>1.12 sent each codex as its own {@code IPacket}; 26.3 sends all codexes through this single payload
 * (porting-guide.md §5). The body layout is defined by the codex: an optional target location prefix
 * (block position for tile codexes, entity network id for entity codexes) followed by the field values.
 *
 * <p>The {@code type} component exists because 26.3 encodes a custom payload with the identifier reported by
 * {@link CustomPacketPayload#type()} (verified in {@code CustomPacketPayload$1#encode}: it writes
 * {@code payload.type().id()} and then looks the codec up in the id map of the direction being written).
 * Clientbound and serverbound play payloads are kept in separate registries, so a payload sent to the client
 * has to report {@link #S2C} while a payload sent to the server has to report {@link #C2S}. Both directions
 * share the same wire format ({@code codex id} + {@code body}), so the two codecs only differ in the type
 * they tag the decoded payload with.
 */
public record CodexPayload(Identifier codexId, byte[] body, CustomPacketPayload.Type<CodexPayload> type) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CodexPayload> S2C = new CustomPacketPayload.Type<>(ICBMConstants.id("codex_s2c"));
    public static final CustomPacketPayload.Type<CodexPayload> C2S = new CustomPacketPayload.Type<>(ICBMConstants.id("codex_c2s"));

    /** Wire format for payloads sent from the server to a client. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CodexPayload> S2C_CODEC = codec(S2C);

    /** Wire format for payloads sent from a client to the server. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CodexPayload> C2S_CODEC = codec(C2S);

    private static StreamCodec<RegistryFriendlyByteBuf, CodexPayload> codec(CustomPacketPayload.Type<CodexPayload> type)
    {
        return StreamCodec.composite(
            Identifier.STREAM_CODEC, CodexPayload::codexId,
            ByteBufCodecs.BYTE_ARRAY, CodexPayload::body,
            (codexId, body) -> new CodexPayload(codexId, body, type)
        );
    }

    /** Creates a payload to be sent from the server to a client. */
    public static CodexPayload toClient(Identifier codexId, byte[] body)
    {
        return new CodexPayload(codexId, body, S2C);
    }

    /** Creates a payload to be sent from a client to the server. */
    public static CodexPayload toServer(Identifier codexId, byte[] body)
    {
        return new CodexPayload(codexId, body, C2S);
    }

    /** This payload tagged as server to client, returns this instance when already tagged correctly. */
    public CodexPayload asClientbound()
    {
        return type == S2C ? this : new CodexPayload(codexId, body, S2C);
    }

    /** This payload tagged as client to server, returns this instance when already tagged correctly. */
    public CodexPayload asServerbound()
    {
        return type == C2S ? this : new CodexPayload(codexId, body, C2S);
    }
}
