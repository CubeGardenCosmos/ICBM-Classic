package icbm.classic.api.reg.obj;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.function.Supplier;

public interface IBuilderRegistry<Part extends IBuildableObject> {

    /**
     * Registers a new factory for loading the part
     *
     * @param name to register with
     * @param builder to create new instances
     *
     * @throws RuntimeException if registry is locked or name is already used
     */
    void register(@NonNull Identifier name, @NonNull Supplier<Part> builder);

    /**
     * Builds/Get the object. Often for data sources this is
     * a global static version that is never rebuilt.
     *
     * @param name matching registry
     * @return instance or null if not registered
     */
    @Nullable
    Part getOrBuild(@NonNull Identifier name);

    /**
     * Unique name for this registry. Mostly used for translations
     *
     * @return unique builder key
     */
    @NonNull
    String getUniqueName();

    /**
     * Saves a part as "id" plus an optional "data" child. Only the id is required as some objects
     * are constants and need no save info.
     *
     * @param part to save
     * @param out  layer to write into, normally the child/list entry of a {@link icbm.classic.lib.saving.SaveNode}
     */
    default void save(@NonNull Part part, @NonNull ValueOutput out) {
        if (part == null) {
            ICBMClassic.logger().warn("Failed to save part due to null value", new RuntimeException());
            return;
        }
        else if (part.getRegistryKey() == null) {
            ICBMClassic.logger().warn("Failed to save part due to missing registry name: " + part, new RuntimeException());
            return;
        }

        out.store("id", Identifier.CODEC, part.getRegistryKey());

        if (part instanceof ISavable) {
            // Data is optional, only id is required as some objects are constants and need no save info
            final ValueOutput data = out.child("data");
            ((ISavable) part).save(data);
            if (data.isEmpty()) {
                out.discard("data");
            }
        }
    }

    default <C extends Collection<Part>> C load(ValueInput.ValueInputList save, @NonNull C list) {
        for (ValueInput entry : save) {
            final Part part = load(entry);
            if (part != null) {
                list.add(part);
            }
        }
        return list;
    }

    default Part load(@NonNull ValueInput save) {
        final Identifier id = save.read("id", Identifier.CODEC).orElse(null);
        if (id == null) {
            return null;
        }
        final Part part = getOrBuild(id);
        if (part instanceof ISavable) {
            ((ISavable) part).load(save.childOrEmpty("data"));
        }
        return part;
    }

    boolean isLocked();
}
