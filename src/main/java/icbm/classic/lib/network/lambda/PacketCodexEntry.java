package icbm.classic.lib.network.lambda;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Single field of a {@link PacketCodex}: how to read it from the target, how to write it back and how to
 * encode/decode the value on the wire.
 *
 * @param <OBJECT> object the field is read from and written to
 * @param <DATA> type of the field value
 */
public class PacketCodexEntry<OBJECT, DATA>
{
    private final Type type;
    private final boolean isArray;
    private final Function<OBJECT, DATA> getter;
    private final BiConsumer<OBJECT, DATA> setter;
    private final BiConsumer<RegistryFriendlyByteBuf, DATA> encoder;
    private final Function<RegistryFriendlyByteBuf, DATA> decoder;

    public PacketCodexEntry(Type type, boolean isArray, Function<OBJECT, DATA> getter, BiConsumer<OBJECT, DATA> setter,
                            BiConsumer<RegistryFriendlyByteBuf, DATA> encoder, Function<RegistryFriendlyByteBuf, DATA> decoder)
    {
        this.type = type;
        this.isArray = isArray;
        this.getter = getter;
        this.setter = setter;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public Type getType()
    {
        return type;
    }

    public boolean isArray()
    {
        return isArray;
    }

    public Function<OBJECT, DATA> getGetter()
    {
        return getter;
    }

    public BiConsumer<OBJECT, DATA> getSetter()
    {
        return setter;
    }

    public BiConsumer<RegistryFriendlyByteBuf, DATA> getEncoder()
    {
        return encoder;
    }

    public Function<RegistryFriendlyByteBuf, DATA> getDecoder()
    {
        return decoder;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "(" + type + ", " + isArray + ")";
    }
}
