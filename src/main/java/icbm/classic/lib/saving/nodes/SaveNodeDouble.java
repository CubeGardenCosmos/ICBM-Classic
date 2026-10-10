package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeDouble<E> extends SaveNode<E>
{
    private final Function<E, Double> save;
    private final BiConsumer<E, Double> load;

    public SaveNodeDouble(final String name, Function<E, Double> save, BiConsumer<E, Double> load)
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
            final Double value = save.apply(obj);
            if (value != null)
            {
                out.putDouble(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final Double current = save.apply(obj);
            load.accept(obj, in.getDoubleOr(getSaveKey(), current != null ? current : 0D));
        }
    }
}
