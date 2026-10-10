package icbm.classic.lib.saving.nodes;

import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeItemStack<E> extends SaveNode<E>
{
    private final Function<E, ItemStack> getter;
    private final BiConsumer<E, ItemStack> setter;

    public SaveNodeItemStack(final String name, Function<E, ItemStack> getter, BiConsumer<E, ItemStack> setter)
    {
        super(name);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void save(E obj, ValueOutput out)
    {
        if (obj != null)
        {
            final ItemStack itemStack = getter.apply(obj);
            if (itemStack != null && !itemStack.isEmpty())
            {
                out.store(getSaveKey(), ItemStack.OPTIONAL_CODEC, itemStack);
            }
        }
    }

    @Override
    public void load(E obj, ValueInput in)
    {
        if (obj != null)
        {
            in.read(getSaveKey(), ItemStack.OPTIONAL_CODEC).ifPresent(itemStack -> setter.accept(obj, itemStack));
        }
    }
}
