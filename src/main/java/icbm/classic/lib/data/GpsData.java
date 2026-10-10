package icbm.classic.lib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * Stored position of GPS items (remote detonator, laser detonator, radar gun, coord cards) kept in the
 * {@code icbmclassic:gps_data} data component. 1.12 exposed this as the {@code GPS_CAPABILITY}.
 *
 * @param pos dimension + position, empty when the item has no target yet
 */
public record GpsData(Optional<GlobalPos> pos)
{
    public static final GpsData EMPTY = new GpsData(Optional.empty());

    public static final Codec<GpsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("pos").forGetter(GpsData::pos)
    ).apply(instance, GpsData::new));

    public static final StreamCodec<ByteBuf, GpsData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), GpsData::pos, GpsData::new);

    public GpsData(GlobalPos pos)
    {
        this(Optional.of(pos));
    }
}
