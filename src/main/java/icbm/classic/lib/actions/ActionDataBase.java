package icbm.classic.lib.actions;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.data.IActionFieldReceiver;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;

public abstract class ActionDataBase implements IActionData, IActionFieldProvider, IActionFieldReceiver, ISavable {

    private final Map<ActionField<?>, Object> fieldValueMap = new HashMap<>();

    @Override
    public <VALUE> void setValue(ActionField<VALUE> key, VALUE value) {
       if(this.getFields().contains(key)) {
           this.fieldValueMap.put(key, value);
       }
    }

    @Override
    public <VALUE> VALUE getValue(ActionField<VALUE> key) {
        return key.cast(this.fieldValueMap.get(key));
    }

    @Override
    public void save(ValueOutput out) {
        if(!fieldValueMap.isEmpty()) {
            final ValueOutput.ValueOutputList list = out.childrenList("fields");
            for(Map.Entry<ActionField<?>, Object> entry : fieldValueMap.entrySet()) {
                final ValueOutput entryOut = list.addChild();
                entryOut.putString("key", entry.getKey().getKey()); //save key even if value is null
                //TODO see if we can save ActionField#type
                if(entry.getValue() != null) {
                    saveValue(entry.getKey(), entryOut, entry.getValue());
                }
            }
        }
    }

    @Override
    public void load(ValueInput in) {
        in.childrenList("fields").ifPresent(list -> {
            this.fieldValueMap.clear();
            for (ValueInput entry : list) {
                final String key = entry.getStringOr("key", "");
                final ActionField<?> actionField = ActionField.find(key, null);
                if(actionField != null) {
                    this.fieldValueMap.put(actionField, loadValue(actionField, entry));
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void saveValue(ActionField field, ValueOutput out, Object value) {
        field.save(out, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object loadValue(ActionField field, ValueInput in) {
        return field.load(in);
    }
}
