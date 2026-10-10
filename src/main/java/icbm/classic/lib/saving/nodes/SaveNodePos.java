package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated
public class SaveNodePos<E> extends SaveNode<E>
{
    private final Function<E, Pos> save;
    private final BiConsumer<E, Pos> load;

    public SaveNodePos(final String name, Function<E, Pos> save, BiConsumer<E, Pos> load)
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
            final Pos pos = save.apply(obj);
            if (pos != null)
            {
                pos.save(out.child(getSaveKey()));
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.child(getSaveKey()).ifPresent(child -> load.accept(obj, new Pos(child)));
        }
    }
}
