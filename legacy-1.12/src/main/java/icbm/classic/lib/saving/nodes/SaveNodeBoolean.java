package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.NbtSaveNode;
import net.minecraft.nbt.NBTTagByte;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeBoolean<E> extends NbtSaveNode<E, NBTTagByte>
{
    public SaveNodeBoolean(final String name, Function<E, Boolean> save, BiConsumer<E, Boolean> load)
    {
        super(name,
            (obj) -> save(save.apply(obj)),
            (obj, data) -> load.accept(obj, load(data))
        );
    }

    public static NBTTagByte save(Boolean b) {
        return new NBTTagByte((byte) (b ? 1 : 0));
    }

    public static boolean load(NBTTagByte tag) {
        return tag.getByte() == 1;
    }
}
