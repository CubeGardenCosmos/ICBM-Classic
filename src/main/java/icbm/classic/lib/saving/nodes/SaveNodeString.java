package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeString<E> extends SaveNode<E>
{
    private final Function<E, String> save;
    private final BiConsumer<E, String> load;

    public SaveNodeString(final String name, Function<E, String> save, BiConsumer<E, String> load)
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
            final String value = save.apply(obj);
            if (value != null)
            {
                out.putString(getSaveKey(), value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            // 1.12 left the value untouched when the key was missing, so re-read the getter as default
            final String current = save.apply(obj);
            load.accept(obj, in.getStringOr(getSaveKey(), current != null ? current : ""));
        }
    }
}
