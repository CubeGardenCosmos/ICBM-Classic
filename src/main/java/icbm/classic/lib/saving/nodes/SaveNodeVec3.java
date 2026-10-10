package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeVec3<E> extends SaveNode<E>
{
    private final Function<E, Vec3> save;
    private final BiConsumer<E, Vec3> load;

    public SaveNodeVec3(final String name, Function<E, Vec3> save, BiConsumer<E, Vec3> load)
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
            final Vec3 pos = save.apply(obj);
            if (pos != null)
            {
                out.store(getSaveKey(), Vec3.CODEC, pos);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), Vec3.CODEC).ifPresent(pos -> load.accept(obj, pos));
        }
    }
}
