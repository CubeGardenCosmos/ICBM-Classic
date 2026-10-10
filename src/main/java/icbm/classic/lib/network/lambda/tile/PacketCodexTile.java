package icbm.classic.lib.network.lambda.tile;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.network.lambda.PacketCodex;
import icbm.classic.network.CodexPayload;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/**
 * Codex for creating packets specific to BlockEntity data
 *
 * The body starts with the target block position, followed by the field values. Receiving codexes validates
 * the position (loaded, close enough to the player, correct block entity type) before applying anything,
 * see porting-guide.md §5.
 *
 * @param <R> raw object, usually a BlockEntity that may contain stuff
 * @param <T> target type for read/write
 */
public class PacketCodexTile<R extends BlockEntity, T> extends PacketCodex<R, T>
{
    /** Type the converted target has to be, used to reject codexes aimed at the wrong block entity */
    private final Class<T> targetClass;

    public PacketCodexTile(Identifier parent, String name, Class<T> targetClass, Function<R, T> converter)
    {
        this(parent, ICBMConstants.id(name), targetClass, converter);
    }

    public PacketCodexTile(Identifier parent, Identifier name, Class<T> targetClass, Function<R, T> converter)
    {
        super(parent, name, converter);
        this.targetClass = targetClass;
    }

    public PacketCodexTile(Identifier parent, String name, Class<T> targetClass)
    {
        this(parent, ICBMConstants.id(name), targetClass);
    }

    @SuppressWarnings("unchecked")
    public PacketCodexTile(Identifier parent, Identifier name, Class<T> targetClass)
    {
        this(parent, name, targetClass, tile -> (T) tile);
    }

    @Override
    public CodexPayload build(R tile)
    {
        final Level level = tile.getLevel();
        final RegistryAccess registryAccess = level != null ? level.registryAccess() : RegistryAccess.EMPTY;
        final RegistryFriendlyByteBuf byteBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try
        {
            byteBuf.writeBlockPos(tile.getBlockPos());
            writeEntries(getConverter().apply(tile), byteBuf);
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
        final BlockPos pos = byteBuf.readBlockPos();

        // Area is no longer loaded, this is normal in most cases
        if (!level.isLoaded(pos))
        {
            logDebug(level, pos, "Target block is not loaded, ignoring packet");
            return null;
        }

        // The level is always the level of the receiving player, so the target can not be in another dimension.
        // Distance is only meaningful on the server, the client has no authority over the target.
        if (!level.isClientSide() && player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_USE_DISTANCE_SQR)
        {
            logDebug(level, pos, "Player(" + player + ") is too far from the target block, ignoring packet");
            return null;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);

        // Could be normal, as data changes in main thread... especially given latency
        if (blockEntity == null || !isValid((R) blockEntity))
        {
            logDebug(level, pos, "Target block entity is invalid, ignoring packet");
            return null;
        }

        final T target = getConverter().apply((R) blockEntity);
        if (target == null || !targetClass.isInstance(target))
        {
            logDebug(level, pos, "Target block entity(" + blockEntity + ") is not of the expected type, ignoring packet");
            return null;
        }
        return (R) blockEntity;
    }

    @Override
    public boolean isValid(R tile)
    {
        return tile != null && !tile.isRemoved();
    }
}
