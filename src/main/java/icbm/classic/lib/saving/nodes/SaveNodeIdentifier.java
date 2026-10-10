package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeIdentifier<E> extends SaveNode<E>
{
    private final Function<E, Identifier> save;
    private final BiConsumer<E, Identifier> load;

    public SaveNodeIdentifier(final String name, Function<E, Identifier> save, BiConsumer<E, Identifier> load)
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
            final Identifier key = save.apply(obj);
            if (key != null)
            {
                // Identifier.CODEC stores the key as its "namespace:path" string form, same as 1.12 wrote
                out.store(getSaveKey(), Identifier.CODEC, key);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), Identifier.CODEC).ifPresent(key -> load.accept(obj, key));
        }
    }
}
