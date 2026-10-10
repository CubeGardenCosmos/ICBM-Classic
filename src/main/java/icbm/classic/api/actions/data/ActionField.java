package icbm.classic.api.actions.data;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata about dynamic fields used by {@link IActionFieldProvider} and {@link IActionFieldReceiver}.
 * <p>
 * 1.12 stored fields as a value plus a raw NBT tag produced by a {@code Function<VALUE, TAG>}. 26.3 has no
 * per-value NBT tag factory; instead save/load now operate directly on the given key inside
 * {@link ValueOutput}/{@link ValueInput}. Each field therefore carries a {@link ValueWriter} and a
 * {@link ValueReader} which are allowed to be null when the field is runtime-only (e.g. an entity reference).
 *
 * @param <VALUE> type of value stored in the field
 */
public final class ActionField<VALUE> {

    private static final Map<String, ActionField<?>> FIELDS = new HashMap<>();

    private final String key;
    private final Type type;
    private final ValueWriter<VALUE> save;
    private final ValueReader<VALUE> load;

    private ActionField(String key, Type type, ValueWriter<VALUE> save, ValueReader<VALUE> load) {
        this.key = key;
        this.type = type;
        this.save = save;
        this.load = load;
    }

    /**
     * Gets or creates a field for the given key. Requesting the same key with a different type is an error.
     *
     * @param key  unique lookup key
     * @param type java type of the value, used to catch duplicate keys
     * @param save writer, null when the field should not be saved
     * @param load reader, null when the field should not be loaded
     * @return field stored against the key
     */
    @SuppressWarnings("unchecked")
    public static <VALUE> ActionField<VALUE> getOrCreate(String key, Type type, @Nullable ValueWriter<VALUE> save, @Nullable ValueReader<VALUE> load) {
        final ActionField<?> field = FIELDS.get(key);
        if (field != null && field.type != type) {
            throw new IllegalArgumentException("Key " + key + " was requested with different type " + type + " when it already uses " + field.type);
        }
        if (field != null) {
            return (ActionField<VALUE>) field;
        }
        final ActionField<VALUE> created = new ActionField<>(key, type, save, load);
        FIELDS.put(key, created);
        return created;
    }

    /**
     * Finds a previously created field.
     *
     * @param key  to match to field
     * @param type to match, can be null to get the first type match
     * @return field or null if not created
     */
    @SuppressWarnings("unchecked")
    public static <VALUE> ActionField<VALUE> find(String key, @Nullable Type type) {
        final ActionField<?> field = FIELDS.get(key);
        if (field != null && (type == null || field.type == type)) {
            return (ActionField<VALUE>) field;
        }
        return null;
    }

    public String getKey() {
        return key;
    }

    public Type getType() {
        return type;
    }

    /**
     * Casts a raw map value back to the field type. Unchecked by design, matching 1.12 behavior.
     */
    @SuppressWarnings("unchecked")
    public VALUE cast(Object v) {
        return (VALUE) v;
    }

    /**
     * Saves the value into the layer using this field's key. Does nothing when the field is not savable.
     */
    public void save(ValueOutput out, VALUE value) {
        if (save != null) {
            save.write(out, key, value);
        }
    }

    /**
     * Loads the value from the layer using this field's key.
     *
     * @return value read, or null when the field is not loadable or the key is missing
     */
    @Nullable
    public VALUE load(ValueInput in) {
        return load != null ? load.read(in, key) : null;
    }

    @FunctionalInterface
    public interface ValueWriter<V> {
        void write(ValueOutput out, String key, V value);
    }

    @FunctionalInterface
    public interface ValueReader<V> {
        V read(ValueInput in, String key);
    }
}
