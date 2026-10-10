package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeInteger<E> extends SaveNode<E>
{
    private final Function<E, Integer> save;
    private final BiConsumer<E, Integer> load;

    public SaveNodeInteger(final String name, Function<E, Integer> save, BiConsumer<E, Integer> load)
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
            final Integer value = save.apply(obj);
            if (value != null)
            {
                out.putInt(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final Integer current = save.apply(obj);
            load.accept(obj, in.getIntOr(getSaveKey(), current != null ? current : 0));
        }
    }
}
