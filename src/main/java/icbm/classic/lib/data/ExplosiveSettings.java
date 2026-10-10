package icbm.classic.lib.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Customization data carried by explosive items in the {@code icbmclassic:explosive_settings} data
 * component. 1.12 stored the same data through {@code IExplosiveCustomization} objects on the
 * explosive capability (item metadata + NBT); 26.3 items are flat so the customization is a component.
 * <p>
 * S2.01 starts with an empty record so the component type exists. Later steps add fields
 * (S3.07 ender target, S4.06 cluster settings); dev builds do not need backwards compatibility.
 */
public record ExplosiveSettings()
{
    public static final ExplosiveSettings EMPTY = new ExplosiveSettings();

    public static final Codec<ExplosiveSettings> CODEC = MapCodec.unitCodec(EMPTY);
    public static final StreamCodec<ByteBuf, ExplosiveSettings> STREAM_CODEC = StreamCodec.unit(EMPTY);
}
