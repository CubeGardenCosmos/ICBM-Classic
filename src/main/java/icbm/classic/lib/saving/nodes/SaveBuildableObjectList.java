package icbm.classic.lib.saving.nodes;

import icbm.classic.api.reg.obj.IBuildableObject;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public class SaveBuildableObjectList<E, BUILDABLE extends IBuildableObject> extends SaveNode<E>
{
    private final Supplier<IBuilderRegistry<BUILDABLE>> reg;
    private final Function<E, Collection<BUILDABLE>> getter;

    public SaveBuildableObjectList(String name, final Supplier<IBuilderRegistry<BUILDABLE>> reg, Function<E, Collection<BUILDABLE>> getter)
    {
        super(name);
        this.reg = reg;
        this.getter = getter;
    }

    @Override
    public void save(E obj, ValueOutput out)
    {
        if (obj != null)
        {
            final Collection<BUILDABLE> list = getter.apply(obj);
            if (list != null && !list.isEmpty())
            {
                final ValueOutput.ValueOutputList children = out.childrenList(getSaveKey());
                for (BUILDABLE object : list)
                {
                    reg.get().save(object, children.addChild());
                }
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            final Collection<BUILDABLE> list = getter.apply(obj);
            if (list != null)
            {
                in.childrenList(getSaveKey()).ifPresent(children -> {
                    list.clear();
                    reg.get().load(children, list);
                });
            }
        }
    }
}
