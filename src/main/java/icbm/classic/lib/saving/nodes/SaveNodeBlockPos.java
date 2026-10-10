package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeBlockPos<E> extends SaveNode<E>
{
    private final Function<E, BlockPos> save;
    private final BiConsumer<E, BlockPos> load;

    public SaveNodeBlockPos(final String name, Function<E, BlockPos> save, BiConsumer<E, BlockPos> load)
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
            final BlockPos pos = save.apply(obj);
            if (pos != null)
            {
                out.store(getSaveKey(), BlockPos.CODEC, pos);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), BlockPos.CODEC).ifPresent(pos -> load.accept(obj, pos));
        }
    }
}
