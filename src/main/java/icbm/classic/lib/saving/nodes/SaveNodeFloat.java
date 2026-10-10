package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeFloat<E> extends SaveNode<E>
{
    private final Function<E, Float> save;
    private final BiConsumer<E, Float> load;

    public SaveNodeFloat(final String name, Function<E, Float> save, BiConsumer<E, Float> load)
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
            final Float value = save.apply(obj);
            if (value != null)
            {
                out.putFloat(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final Float current = save.apply(obj);
            load.accept(obj, in.getFloatOr(getSaveKey(), current != null ? current : 0F));
        }
    }
}
