package icbm.classic.config.util;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * A single parsed matcher entry used by {@link ResourceConfigList}.
 *
 * @param <CONTENT> content type being matched
 * @param <VALUE> value produced when the content matches
 */
@Data
public class ResourceConfigEntry<CONTENT, VALUE> implements Function<CONTENT, VALUE> {

    /** Type description, mostly exists for debugging */
    private String type;
    /** Sort order */
    private Integer order;
    /** Function used to match the content input to the value output, returns null if doesn't match */
    private Function<CONTENT, VALUE> function;

    /** Key the entry is grouped under, formatted as {@code namespace:path}. May be null for general entries. */
    @Accessors(chain = true)
    private String key;

    public ResourceConfigEntry(String type, Integer order, Function<CONTENT, VALUE> function) {
        this.type = type;
        this.order = order;
        this.function = function;
    }

    @Override
    public VALUE apply(CONTENT content) {
        return function.apply(content);
    }
}
// probe
