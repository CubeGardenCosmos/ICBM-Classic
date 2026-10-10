package icbm.classic.lib.saving;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Replaces the 1.12 Forge {@code INBTSerializable} used by content that needs to push save data
 * through a {@link SaveHandler}. Objects implementing this are handed the current layer of the save
 * by {@link SaveRoot#nodeSavable(String, java.util.function.Function)} and the registry helpers in
 * {@link icbm.classic.api.reg.obj.IBuilderRegistry}.
 *
 * @see SaveHandler
 */
public interface ISavable
{
    /**
     * Called to save data
     *
     * @param out layer of the save to write into, owned by the caller
     */
    void save(ValueOutput out);

    /**
     * Called to load save data
     *
     * @param in layer of the save to read from
     */
    void load(ValueInput in);
}
