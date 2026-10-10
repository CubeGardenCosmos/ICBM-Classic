package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeByte<E> extends SaveNode<E>
{
    private final Function<E, Byte> save;
    private final BiConsumer<E, Byte> load;

    public SaveNodeByte(final String name, Function<E, Byte> save, BiConsumer<E, Byte> load)
    {
        super(name);
        this.save = save;
        this.load = load;
    }

    @Override
    public void save(E obj, ValueOutput out)
    {
        if (obj != null)
        {
            final Byte value = save.apply(obj);
            if (value != null)
            {
                out.putByte(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final Byte current = save.apply(obj);
            load.accept(obj, in.getByteOr(getSaveKey(), current != null ? current : (byte) 0));
        }
    }
}
