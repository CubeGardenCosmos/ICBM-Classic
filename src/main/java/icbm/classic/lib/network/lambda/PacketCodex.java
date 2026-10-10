package icbm.classic.lib.network.lambda;

import icbm.classic.ICBMClassic;
import icbm.classic.network.CodexPayload;
import icbm.classic.network.IcbmNetwork;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.util.TriConsumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Codex for creating packets to read/write data using lambda accessors
 *
 * <p>1.12 wrote its own packet per codex and used an incrementing integer id; 26.3 transports the codex body
 * through {@link CodexPayload} using the codex id ({@link #getCodexId()}) for lookup, see porting-guide.md §5.
 * The {@code parent} + {@code name} pair is kept so upper layer code (machines) does not change.
 *
 * @param <RAW> object to access
 * @param <TARGET> object converted from raw and used for Read/Write, often is the same as {@link RAW}
 */
public abstract class PacketCodex<RAW, TARGET>
{
    public static final String LOGGER_TEMPLATE = "Packet(%s): %s\n\tWorld:%s\n\tPos: %sx %sy %sz\n\n\tCodex: %s\n\tParent: %s\n\tName: %s";

    /** Max distance (blocks) between the player and the target that a client sent codex is accepted from, porting-guide.md §5 */
    public static final double MAX_USE_DISTANCE = 64;
    public static final double MAX_USE_DISTANCE_SQR = MAX_USE_DISTANCE * MAX_USE_DISTANCE;

    private boolean allowServer = true;
    private boolean allowClient = true;

    /** Content representing this packet set, if this is a BlockEntity then it is the block */
    private final Identifier parent;
    /** Name of the packet, useful for debugging and error handling */
    private final Identifier name;
    /** Unique id of this codex, {@code parent path + "/" + name path}, used to look it up in {@link PacketCodexReg} */
    private final Identifier codexId;
    /** Convert to go from RAW to TARGET, often RAW == TARGET but in cases it can be useful to go from BlockEntity to a capability */
    private final Function<RAW, TARGET> converter;

    /** Entries of readers and writers */
    private final List<PacketCodexEntry<TARGET, ?>> entries = new ArrayList<>();

    /** Called when decoding is finished and all data is written to the target */
    private TriConsumer<RAW, TARGET, Player> onFinished;

    protected PacketCodex(Identifier parent, Identifier name, Function<RAW, TARGET> converter)
    {
        this.parent = parent;
        this.name = name;
        this.codexId = Identifier.fromNamespaceAndPath(parent.getNamespace(), parent.getPath() + "/" + name.getPath());
        this.converter = converter;
    }

    public PacketCodex<RAW, TARGET> asClientOnly()
    {
        this.allowClient = true;
        this.allowServer = false;
        return this;
    }

    public PacketCodex<RAW, TARGET> asServerOnly()
    {
        this.allowClient = false;
        this.allowServer = true;
        return this;
    }

    /** This codex is sent from the client to the server, so only the server may handle it */
    public PacketCodex<RAW, TARGET> fromClient()
    {
        return asServerOnly();
    }

    /** This codex is sent from the server to a client, so only the client may handle it */
    public PacketCodex<RAW, TARGET> fromServer()
    {
        return asClientOnly();
    }

    public PacketCodex<RAW, TARGET> onFinished(TriConsumer<RAW, TARGET, Player> onFinished)
    {
        this.onFinished = onFinished;
        return this;
    }

    /** True when the client may handle (receive) this codex, set by {@link #fromServer()} / {@link #asClientOnly()} */
    public boolean isAllowClient()
    {
        return allowClient;
    }

    /** True when the server may handle (receive) this codex, set by {@link #fromClient()} / {@link #asServerOnly()} */
    public boolean isAllowServer()
    {
        return allowServer;
    }

    public Identifier getParent()
    {
        return parent;
    }

    public Identifier getName()
    {
        return name;
    }

    public Identifier getCodexId()
    {
        return codexId;
    }

    public Function<RAW, TARGET> getConverter()
    {
        return converter;
    }

    public List<PacketCodexEntry<TARGET, ?>> getEntries()
    {
        return entries;
    }

    public PacketCodex<RAW, TARGET> nodeInt(Function<TARGET, Integer> getter, BiConsumer<TARGET, Integer> setter)
    {
        return node(Integer.class, false, getter, setter, (byteBuf, value) -> byteBuf.writeInt(value), byteBuf -> byteBuf.readInt());
    }

    public PacketCodex<RAW, TARGET> nodeByte(Function<TARGET, Byte> getter, BiConsumer<TARGET, Byte> setter)
    {
        return node(Byte.class, false, getter, setter, (byteBuf, value) -> byteBuf.writeByte(value), byteBuf -> byteBuf.readByte());
    }

    public <E extends Enum<E>> PacketCodex<RAW, TARGET> nodeEnum(Class<E> e, Function<TARGET, E> getter, BiConsumer<TARGET, E> setter)
    {
        if (e.getEnumConstants().length > 255)
        {
            return nodeInt((t) -> getter.apply(t).ordinal(), (t, v) -> setter.accept(t, e.getEnumConstants()[v]));
        }
        return nodeByte((t) -> (byte) getter.apply(t).ordinal(), (t, v) -> setter.accept(t, e.getEnumConstants()[v]));
    }

    public PacketCodex<RAW, TARGET> nodeDirection(Function<TARGET, Direction> getter, BiConsumer<TARGET, Direction> setter)
    {
        return node(Direction.class, false, getter, setter,
            (byteBuf, direction) -> byteBuf.writeByte(direction.ordinal()),
            (byteBuf) ->
            {
                final Direction[] values = Direction.values();
                return values[Mth.abs(byteBuf.readByte() % values.length)];
            });
    }

    public PacketCodex<RAW, TARGET> nodeDouble(Function<TARGET, Double> getter, BiConsumer<TARGET, Double> setter)
    {
        return node(Double.class, false, getter, setter, (byteBuf, value) -> byteBuf.writeDouble(value), byteBuf -> byteBuf.readDouble());
    }

    public PacketCodex<RAW, TARGET> nodeFloat(Function<TARGET, Float> getter, BiConsumer<TARGET, Float> setter)
    {
        return node(Float.class, false, getter, setter, (byteBuf, value) -> byteBuf.writeFloat(value), byteBuf -> byteBuf.readFloat());
    }

    public PacketCodex<RAW, TARGET> nodeString(Function<TARGET, String> getter, BiConsumer<TARGET, String> setter)
    {
        return node(String.class, false, getter, setter, RegistryFriendlyByteBuf::writeUtf, RegistryFriendlyByteBuf::readUtf);
    }

    public PacketCodex<RAW, TARGET> nodeItemStack(Function<TARGET, ItemStack> getter, BiConsumer<TARGET, ItemStack> setter)
    {
        return node(ItemStack.class, false, getter, setter,
            (byteBuf, stack) -> ItemStack.OPTIONAL_STREAM_CODEC.encode(byteBuf, stack),
            (byteBuf) -> ItemStack.OPTIONAL_STREAM_CODEC.decode(byteBuf));
    }

    public PacketCodex<RAW, TARGET> nodeVec3(Function<TARGET, Vec3> getter, BiConsumer<TARGET, Vec3> setter)
    {
        return node(Vec3.class, false, getter, setter,
            (byteBuf, vec) ->
            {
                byteBuf.writeDouble(vec.x);
                byteBuf.writeDouble(vec.y);
                byteBuf.writeDouble(vec.z);
            },
            (byteBuf) -> new Vec3(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble()));
    }

    public PacketCodex<RAW, TARGET> nodeNbtCompound(Function<TARGET, CompoundTag> getter, BiConsumer<TARGET, CompoundTag> setter)
    {
        return node(CompoundTag.class, false, getter, setter,
            (byteBuf, tag) -> ByteBufCodecs.COMPOUND_TAG.encode(byteBuf, tag),
            (byteBuf) -> ByteBufCodecs.COMPOUND_TAG.decode(byteBuf));
    }

    public PacketCodex<RAW, TARGET> nodeBoolean(Function<TARGET, Boolean> getter, BiConsumer<TARGET, Boolean> setter)
    {
        return node(Boolean.class, false, getter, setter, (byteBuf, value) -> byteBuf.writeBoolean(value), byteBuf -> byteBuf.readBoolean());
    }

    public PacketCodex<RAW, TARGET> toggleBoolean(Function<TARGET, Boolean> getter, BiConsumer<TARGET, Boolean> setter)
    {
        return nodeBoolean(getter, (t, b) -> setter.accept(t, !b));
    }

    public <DATA> PacketCodex<RAW, TARGET> node(Class<DATA> clazz, boolean isArray, Function<TARGET, DATA> getter, BiConsumer<TARGET, DATA> setter,
                                                BiConsumer<RegistryFriendlyByteBuf, DATA> encoder, Function<RegistryFriendlyByteBuf, DATA> decoder)
    {
        return node(new PacketCodexEntry<>(clazz, isArray, getter, setter, encoder, decoder));
    }

    public <DATA> PacketCodex<RAW, TARGET> node(Function<TARGET, DATA> getter, BiConsumer<TARGET, DATA> setter,
                                                BiConsumer<RegistryFriendlyByteBuf, DATA> encoder, Function<RegistryFriendlyByteBuf, DATA> decoder)
    {
        return node(new PacketCodexEntry<>(null, false, getter, setter, encoder, decoder));
    }

    public <DATA> PacketCodex<RAW, TARGET> node(PacketCodexEntry<TARGET, DATA> entry)
    {
        entries.add(entry);
        return this;
    }

    /**
     * Pulls the data from the target and prepares an encoder for each field.
     *
     * Purpose of this is to extract field data at time of packet creation. This way any changes on main
     * thread do not impact the packet writing process.
     *
     * @param target to encode
     * @return list of consumers to run for each field
     */
    @SuppressWarnings("unchecked")
    public List<Consumer<RegistryFriendlyByteBuf>> encodeAsWriters(TARGET target)
    {
        return getEntries().stream()
            .map(entry ->
            {
                final Object value = entry.getGetter().apply(target);
                final BiConsumer<RegistryFriendlyByteBuf, Object> encoder = (BiConsumer<RegistryFriendlyByteBuf, Object>) entry.getEncoder();
                return (Consumer<RegistryFriendlyByteBuf>) (byteBuf) -> encoder.accept(byteBuf, value);
            })
            .collect(Collectors.toList());
    }

    /**
     * Decodes packet data into fields
     *
     * All fields are read from the buffer before any of the returned setters is run, so a broken body can
     * not leave the target half applied (same behaviour as 1.12).
     *
     * @param byteBuf body of the codex, positioned at the first field
     * @return list of consumers to run for each field
     */
    @SuppressWarnings("unchecked")
    public List<Consumer<TARGET>> decodeAsSetters(RegistryFriendlyByteBuf byteBuf)
    {
        return getEntries().stream()
            .map(entry ->
            {
                final Function<RegistryFriendlyByteBuf, Object> decoder = (Function<RegistryFriendlyByteBuf, Object>) entry.getDecoder();
                final Object data = decoder.apply(byteBuf);
                final BiConsumer<TARGET, Object> setter = (BiConsumer<TARGET, Object>) entry.getSetter();
                return (Consumer<TARGET>) (target) -> setter.accept(target, data);
            })
            .collect(Collectors.toList());
    }

    /** Reads the whole body and applies every field to the target */
    public void decodeAndApply(RegistryFriendlyByteBuf byteBuf, TARGET target)
    {
        decodeAsSetters(byteBuf).forEach(setter -> setter.accept(target));
    }

    /** Writes the current value of every entry to the buffer, in entry order */
    protected void writeEntries(TARGET target, RegistryFriendlyByteBuf byteBuf)
    {
        encodeAsWriters(target).forEach(writer -> writer.accept(byteBuf));
    }

    /** Encodes the field values of the target into a new byte array, using no registry access */
    public byte[] encodeBody(TARGET target)
    {
        return encodeBody(target, RegistryAccess.EMPTY);
    }

    /**
     * Encodes the field values of the target into a new byte array.
     *
     * Codex bodies are always written with a {@link RegistryFriendlyByteBuf} so registry backed values
     * (item stacks) can be written, see porting-guide.md §5.
     *
     * @param target to read the values from
     * @param registryAccess of the level the payload is created in
     */
    public byte[] encodeBody(TARGET target, RegistryAccess registryAccess)
    {
        final RegistryFriendlyByteBuf byteBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try
        {
            writeEntries(target, byteBuf);
            return ByteBufUtil.getBytes(byteBuf);
        }
        finally
        {
            byteBuf.release();
        }
    }

    /**
     * Creates the payload for the given target.
     *
     * The payload carries the location prefix (written by the subclass) followed by the field values.
     * The direction is tagged from {@link #isAllowClient()}/{@link #isAllowServer()}, the transport layer
     * re-tags it when a codex allows both directions.
     */
    public abstract CodexPayload build(RAW raw);

    /** Creates the payload for this codex, tagged with the direction this codex is used in */
    protected CodexPayload createPayload(byte[] body)
    {
        // fromServer() codexes (client handles them) are sent to the client, everything else to the server
        return isAllowClient() && !isAllowServer() ? CodexPayload.toClient(getCodexId(), body) : CodexPayload.toServer(getCodexId(), body);
    }

    /**
     * Reads the target location prefix written by {@link #build(Object)}, validates it against the player
     * (porting-guide.md §5) and returns the raw target object, or null when the target is invalid. The reason
     * is logged by the implementation.
     *
     * @param level the level the payload was received in, already the level of the receiving player
     * @param player player that received the payload
     * @param byteBuf body of the codex, positioned at the location prefix
     */
    protected abstract RAW locate(Level level, Player player, RegistryFriendlyByteBuf byteBuf);

    /** Handles a codex received on the server from a client */
    public void handleFromClient(ServerPlayer player, RegistryFriendlyByteBuf byteBuf)
    {
        handle(player.level(), player, byteBuf);
    }

    /** Handles a codex received on the client from the server */
    public void handleFromServer(Level level, Player player, RegistryFriendlyByteBuf byteBuf)
    {
        handle(level, player, byteBuf);
    }

    private void handle(Level level, Player player, RegistryFriendlyByteBuf byteBuf)
    {
        final RAW raw = locate(level, player, byteBuf);
        if (raw == null)
        {
            return; // locate() already logged the reason
        }
        final TARGET target = getConverter().apply(raw);
        if (target == null)
        {
            logDebug(level, null, "Converter returned no target for " + raw);
            return;
        }
        decodeAndApply(byteBuf, target);
        if (onFinished != null)
        {
            onFinished.accept(raw, target, player);
        }
    }

    public abstract boolean isValid(RAW raw);

    public void sendToServer(RAW raw)
    {
        try
        {
            IcbmNetwork.sendToServer(build(raw));
        }
        catch (Exception e)
        {
            ICBMClassic.logger().error(generateLogMessage(null, null, "Failed to send packet(" + parent + ", " + name + ") to server for " + raw), e);
        }
    }

    public void sendToPlayer(RAW raw, ServerPlayer player)
    {
        try
        {
            IcbmNetwork.sendToPlayer(build(raw), player);
        }
        catch (Exception e)
        {
            logError(null, null, "Failed to send packet(" + parent + ", " + name + ") to player(" + player + ") for " + raw, e);
        }
    }

    /**
     * Sends this codex to every player tracking the raw object.
     *
     * 1.12 used a {@code NetworkRegistry.TargetPoint} around the object position; 26.3 asks Fabric which
     * players are tracking the block entity or entity (porting-guide.md §5).
     */
    public void sendToTracking(RAW raw)
    {
        try
        {
            final CodexPayload payload = build(raw);
            final Collection<ServerPlayer> players;
            if (raw instanceof Entity entity)
            {
                players = PlayerLookup.tracking(entity);
            }
            else if (raw instanceof BlockEntity blockEntity)
            {
                players = PlayerLookup.tracking(blockEntity);
            }
            else
            {
                logError(null, null, "Cannot send packet(" + parent + ", " + name + ") to tracking players, target is not an entity or block entity: " + raw);
                return;
            }
            players.forEach(player -> IcbmNetwork.sendToPlayer(payload, player));
        }
        catch (Exception e)
        {
            logError(null, null, "Failed to send packet(" + parent + ", " + name + ") to tracking players for " + raw, e);
        }
    }

    public void sendToGuiUsers(RAW raw, Collection<ServerPlayer> players)
    {
        try
        {
            final CodexPayload payload = build(raw);
            players.forEach(player -> IcbmNetwork.sendToPlayer(payload, player));
        }
        catch (Exception e)
        {
            logError(null, null, "Failed to send packet(" + parent + ", " + name + ") to gui users for " + raw, e);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s,%s,%s)", getClass().getName(), codexId, parent, name);
    }

    public String generateLogMessage(@Nullable Level level, @Nullable BlockPos pos, @NonNull String message)
    {
        return String.format(
            LOGGER_TEMPLATE,
            getCodexId(),
            message,
            Optional.ofNullable(level).map(l -> l.dimension().identifier().toString()).orElse("--"),
            Optional.ofNullable(pos).map(BlockPos::getX).map(Object::toString).orElse("-"),
            Optional.ofNullable(pos).map(BlockPos::getY).map(Object::toString).orElse("-"),
            Optional.ofNullable(pos).map(BlockPos::getZ).map(Object::toString).orElse("-"),
            this.getClass(),
            this.getParent(),
            this.getName()
        );
    }

    public void logDebug(@Nullable Level level, @Nullable BlockPos pos, @NonNull String message)
    {
        if (ICBMClassic.logger().isDebugEnabled())
        {
            ICBMClassic.logger().debug(generateLogMessage(level, pos, message));
        }
    }

    public void logError(@Nullable Level level, @Nullable BlockPos pos, @NonNull String message)
    {
        ICBMClassic.logger().error(generateLogMessage(level, pos, message));
    }

    public void logError(@Nullable Level level, @Nullable BlockPos pos, @NonNull String message, Exception e)
    {
        ICBMClassic.logger().error(generateLogMessage(level, pos, message), e);
    }
}
