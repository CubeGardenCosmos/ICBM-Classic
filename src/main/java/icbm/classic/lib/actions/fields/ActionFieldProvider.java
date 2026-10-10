package icbm.classic.lib.actions.fields;

import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.IActionFieldProvider;

import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ActionFieldProvider implements IActionFieldProvider {
    private final Map<ActionField<?>, Supplier<?>> fieldAccessors = new HashMap<>();

    public <VALUE> ActionFieldProvider field(ActionField<VALUE> field, Supplier<VALUE> accessor) {
        fieldAccessors.put(field, accessor);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <VALUE> VALUE getValue(ActionField<VALUE> key) {
        Supplier<?> supplier = fieldAccessors.get(key);
        if(supplier == null) {
            return null;
        }
        return (VALUE) supplier.get();
    }

    @Override
    public boolean hasField(ActionField<?> key) {
        return fieldAccessors.containsKey(key);
    }

    @NonNull
    public Collection<ActionField<?>> getFields() {
        return fieldAccessors.keySet();
    }
}
