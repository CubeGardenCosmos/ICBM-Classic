package icbm.classic.lib.buildable;

import icbm.classic.api.reg.obj.IBuildableObject;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Template for creating new buildable object types
 *
 * @param <T> this
 */
public abstract class BuildableObject<T extends IBuildableObject, B extends IBuilderRegistry> implements IBuildableObject, ISavable {

    @NonNull
    private final Identifier registryKey;
    @NonNull
    private final B registry;
    @Nullable
    private final SaveHandler<T> saveHandler;

    /**
     * Required constructor to feed in references.
     *
     * @param registryKey used to register this type with {@link #getRegistry()}, should be static value pass through
     * @param registry used to register the type, should be static value pass through
     * @param saveHandler to use for save/load handling, optional and should be static value pass through
     */
    public BuildableObject(@NonNull Identifier registryKey, @NonNull B registry, @Nullable SaveHandler<T> saveHandler) {
        this.registryKey = registryKey;
        this.registry = registry;
        this.saveHandler = saveHandler;
    }

    @NonNull
    public Identifier getRegistryKey() {
        return registryKey;
    }

    @NonNull
    public B getRegistry() {
        return registry;
    }

    /**
     * Handler used to save/load this object, may be null when the object only needs its registry key.
     */
    @Nullable
    protected SaveHandler<T> getSaveHandler() {
        return saveHandler;
    }

    @Override
    public void save(ValueOutput out) {
        if (saveHandler != null) {
            saveHandler.save((T) this, out);
        }
    }

    @Override
    public void load(ValueInput in) {
        if (saveHandler != null) {
            saveHandler.load((T) this, in);
        }
    }
}
