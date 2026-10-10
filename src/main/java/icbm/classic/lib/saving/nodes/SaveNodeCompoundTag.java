package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeCompoundTag<E> extends SaveNode<E>
{
    private final Function<E, CompoundTag> save;
    private final BiConsumer<E, CompoundTag> load;

    public SaveNodeCompoundTag(final String name, Function<E, CompoundTag> save, BiConsumer<E, CompoundTag> load)
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
            final CompoundTag tag = save.apply(obj);
            if (tag != null)
            {
                out.store(getSaveKey(), CompoundTag.CODEC, tag);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), CompoundTag.CODEC).ifPresent(tag -> load.accept(obj, tag));
        }
    }
}
