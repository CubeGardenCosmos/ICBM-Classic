package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeBlockState<E> extends SaveNode<E>
{
    private final Function<E, BlockState> save;
    private final BiConsumer<E, BlockState> load;

    public SaveNodeBlockState(final String name, Function<E, BlockState> save, BiConsumer<E, BlockState> load)
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
            final BlockState blockState = save.apply(obj);
            if (blockState != null)
            {
                out.store(getSaveKey(), BlockState.CODEC, blockState);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), BlockState.CODEC).ifPresent(blockState -> load.accept(obj, blockState));
        }
    }
}
