package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeBoolean<E> extends SaveNode<E>
{
    private final Function<E, Boolean> save;
    private final BiConsumer<E, Boolean> load;

    public SaveNodeBoolean(final String name, Function<E, Boolean> save, BiConsumer<E, Boolean> load)
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
            final Boolean value = save.apply(obj);
            if (value != null)
            {
                out.putBoolean(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final Boolean current = save.apply(obj);
            load.accept(obj, in.getBooleanOr(getSaveKey(), current != null ? current : false));
        }
    }
}
