package icbm.classic.lib.buildable;

import icbm.classic.ICBMClassic;
import icbm.classic.api.reg.obj.IBuildableObject;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NonNull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class BuildableObjectRegistry<Part extends IBuildableObject> implements IBuilderRegistry<Part>
{
    /** 1.12 used FMLCommonHandler#getModName here, the mod id is fixed by AGENTS.md */
    private static final String MOD_NAME = "icbmclassic";

    private final Map<Identifier, Supplier<Part>> builders = new HashMap<>();
    private boolean locked = false;

    private final String loggerPrefix;
    private final String key;

    @Deprecated
    public BuildableObjectRegistry(String loggerPrefix) {
        this.loggerPrefix = loggerPrefix;
        this.key = loggerPrefix.toLowerCase(Locale.ROOT).replace("_", ".");
    }

    public BuildableObjectRegistry(String loggerPrefix, String key) {
        this.loggerPrefix = loggerPrefix;
        this.key = key;
    }

    @Override
    public void register(@NonNull Identifier key, @NonNull Supplier<Part> builder) {
        if (locked) {
            throw new RuntimeException(this.loggerPrefix + ": mod '" + MOD_NAME + "' attempted to do a late registry");
        }
        if (builders.containsKey(key)) {
            throw new RuntimeException(this.loggerPrefix + ": mod '" + MOD_NAME + "' attempted to override '" + key + "'. " +
                    "This method does not allow replacing existing registries. See implementing class for override call.");
        }
        builders.put(key, builder);
    }

    /**
     * Use this to safely override another mod's content. Make sure to do a dependency on the mod to ensure
     * your mod loads after. Do not wait for the events to complete as the registry locks and will throw errors.
     *
     * @param key of the content to override
     * @param builder to use for save/load
     */
    public void overrideRegistry(Identifier key, Supplier<Part> builder) {
        if (locked) {
            throw new RuntimeException(this.loggerPrefix + ":mod '" + MOD_NAME + "' attempted to do a late registry");
        }
        ICBMClassic.logger().info(this.loggerPrefix + ":'" + key + "' is being overridden by " + MOD_NAME);
        builders.put(key, builder);
    }

    @Override
    public Part getOrBuild(@NonNull Identifier name) {
        return Optional.ofNullable(builders.get(name)).map(Supplier::get).orElse(null);
    }

    @NonNull
    @Override
    public String getUniqueName() {
        return key;
    }

    public void lock() {
        this.locked = true;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }
}
