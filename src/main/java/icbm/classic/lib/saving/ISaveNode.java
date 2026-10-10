package icbm.classic.lib.saving;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Replaces 1.12 {@code INbtSaveNode}. Nodes write straight into the {@link ValueOutput} they are
 * handed instead of returning an NBT tag, which removes the need for a data type parameter.
 *
 * @param <E> type of object to save
 */
public interface ISaveNode<E>
{
    /**
     * Key this node is stored under
     *
     * @return save key
     */
    String getSaveKey();

    /**
     * Called to save data
     *
     * @param objectToSave to pull data from, never null when invoked by a save handler
     * @param out           layer to write into, already scoped to the owning root
     */
    void save(E objectToSave, ValueOutput out);

    /**
     * Called to load save data
     *
     * @param objectToLoad to push data back into, never null when invoked by a save handler
     * @param in           layer to read from, already scoped to the owning root
     */
    void load(E objectToLoad, ValueInput in);
}
