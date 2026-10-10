package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeUUID<E> extends SaveNode<E>
{
    private final Function<E, UUID> save;
    private final BiConsumer<E, UUID> load;

    public SaveNodeUUID(final String name, Function<E, UUID> save, BiConsumer<E, UUID> load)
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
            final UUID value = save.apply(obj);
            if (value != null)
            {
                out.store(getSaveKey(), UUIDUtil.CODEC, value);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), UUIDUtil.CODEC).ifPresent(value -> load.accept(obj, value));
        }
    }
}
