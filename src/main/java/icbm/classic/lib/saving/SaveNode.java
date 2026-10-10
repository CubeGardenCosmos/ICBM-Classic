package icbm.classic.lib.saving;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Replaces 1.12 {@code NbtSaveNode}. The generic NBT data type parameter is gone as nodes now write
 * into {@link ValueOutput} directly, see {@link ISaveNode}.
 *
 * @param <E> type of object to save
 */
public abstract class SaveNode<E> implements ISaveNode<E>
{
    private final String name;

    public SaveNode(final String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("save key can't be null");
        }
        this.name = name;
    }

    @Override
    public String getSaveKey()
    {
        return name;
    }

    @Override
    public abstract void save(E objectToSave, ValueOutput out);

    @Override
    public abstract void load(E objectToLoad, ValueInput in);
}
