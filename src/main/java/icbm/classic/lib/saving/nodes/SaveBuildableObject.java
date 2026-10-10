package icbm.classic.lib.saving.nodes;

import icbm.classic.api.reg.obj.IBuildableObject;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SaveBuildableObject<E, C extends IBuildableObject> extends SaveNode<E>
{
    private final Supplier<IBuilderRegistry<C>> reg;
    private final Function<E, C> getter;
    private final BiConsumer<E, C> setter;

    public SaveBuildableObject(String name, final Supplier<IBuilderRegistry<C>> reg, Function<E, C> getter, BiConsumer<E, C> setter)
    {
        super(name);
        this.reg = reg;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void save(E obj, ValueOutput out)
    {
        if (obj != null)
        {
            final C object = getter.apply(obj);
            if (object != null)
            {
                reg.get().save(object, out.child(getSaveKey()));
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.child(getSaveKey()).ifPresent(child -> {
                final C object = reg.get().load(child);
                if (object != null)
                {
                    setter.accept(obj, object);
                }
            });
        }
    }
}
