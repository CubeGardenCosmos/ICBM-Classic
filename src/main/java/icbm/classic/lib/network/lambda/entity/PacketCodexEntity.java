package icbm.classic.lib.network.lambda.entity;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.network.lambda.PacketCodex;
import icbm.classic.network.CodexPayload;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Function;

/**
 * Codex for creating packets specific to Entity data
 *
 * The body starts with the network id of the target entity, followed by the field values. Receiving codexes
 * validates the entity (exists, alive, close enough to the player, correct type) before applying anything,
 * see porting-guide.md §5.
 *
 * @param <R> raw object, usually an Entity that may contain stuff
 * @param <T> target type for read/write
 */
public class PacketCodexEntity<R extends Entity, T> extends PacketCodex<R, T>
{
    /** Type the converted target has to be, used to reject codexes aimed at the wrong entity */
    private final Class<T> targetClass;

    public PacketCodexEntity(Identifier parent, String name, Class<T> targetClass, Function<R, T> converter)
    {
        this(parent, ICBMConstants.id(name), targetClass, converter);
    }

    public PacketCodexEntity(Identifier parent, Identifier name, Class<T> targetClass, Function<R, T> converter)
    {
        super(parent, name, converter);
        this.targetClass = targetClass;
    }

    public PacketCodexEntity(Identifier parent, String name, Class<T> targetClass)
    {
        this(parent, ICBMConstants.id(name), targetClass);
    }

    @SuppressWarnings("unchecked")
    public PacketCodexEntity(Identifier parent, Identifier name, Class<T> targetClass)
    {
        this(parent, name, targetClass, entity -> (T) entity);
    }

    @Override
    public CodexPayload build(R entity)
    {
        final Level level = entity.level();
        final RegistryFriendlyByteBuf byteBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try
        {
            byteBuf.writeVarInt(entity.getId());
            writeEntries(getConverter().apply(entity), byteBuf);
            return createPayload(ByteBufUtil.getBytes(byteBuf));
        }
        finally
        {
            byteBuf.release();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected R locate(Level level, Player player, RegistryFriendlyByteBuf byteBuf)
    {
        final int entityId = byteBuf.readVarInt();
        final Entity entity = level.getEntity(entityId);

        // This may be valid, as the entity could have changed or may have become invalid.
        // Average ping is same as tick rate so changes on main-thread are expected
        if (entity == null || !isValid((R) entity))
        {
            logDebug(level, null, "Target entity(" + entityId + ") is invalid, ignoring packet");
            return null;
        }

        // The level is always the level of the receiving player, so the target can not be in another dimension.
        // Distance is only meaningful on the server, the client has no authority over the target.
        if (!level.isClientSide() && player.distanceToSqr(entity) > MAX_USE_DISTANCE_SQR)
        {
            logDebug(level, null, "Player(" + player + ") is too far from target entity(" + entityId + "), ignoring packet");
            return null;
        }

        final T target = getConverter().apply((R) entity);
        if (target == null || !targetClass.isInstance(target))
        {
            logDebug(level, null, "Target entity(" + entity + ") is not of the expected type, ignoring packet");
            return null;
        }
        return (R) entity;
    }

    @Override
    public boolean isValid(R entity)
    {
        return entity != null && entity.isAlive();
    }
}
