package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeDirection<E> extends SaveNode<E>
{
    private final Function<E, Direction> save;
    private final BiConsumer<E, Direction> load;

    public SaveNodeDirection(final String name, Function<E, Direction> save, BiConsumer<E, Direction> load)
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
            final Direction facing = save.apply(obj);
            if (facing != null)
            {
                out.store(getSaveKey(), Direction.CODEC, facing);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), Direction.CODEC).ifPresent(facing -> load.accept(obj, facing));
        }
    }
}
