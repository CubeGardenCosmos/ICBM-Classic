package icbm.classic.lib.actions;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.data.IActionFieldReceiver;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public abstract class ActionDataBase implements IActionData, IActionFieldProvider, IActionFieldReceiver, INBTSerializable<NBTTagCompound> {

    private final Map<ActionField, Object> fieldValueMap = new HashMap<>();

    @Override
    public <VALUE, TAG extends NBTBase> void setValue(ActionField<VALUE, TAG> key, VALUE value) {
       if(this.getFields().contains(key)) {
           this.fieldValueMap.put(key, value);
       }
    }

    @Override
    public <VALUE, TAG extends NBTBase> VALUE getValue(ActionField<VALUE, TAG> key) {
        return key.cast(this.fieldValueMap.get(key));
    }

    @Override
    public NBTTagCompound serializeNBT() {
        final NBTTagCompound tag = new NBTTagCompound();
        if(!fieldValueMap.isEmpty()) {
            final NBTTagList list = new NBTTagList();
            for(Map.Entry<ActionField, Object> entry : fieldValueMap.entrySet()) {
                final NBTTagCompound entryTag = new NBTTagCompound();
                entryTag.setString("key", entry.getKey().getKey()); //save key even if value is null
                //TODO see if we can save ActionField#type
                if(entry.getValue() != null) {
                    NBTBase valueSave = entry.getKey().save(entry.getValue());
                    if(valueSave != null) {
                        entryTag.setTag("value", valueSave);
                    }
                }
                list.appendTag(entryTag);
            }
            tag.setTag("fields", list);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if(nbt.hasKey("fields")) {
            this.fieldValueMap.clear();
            final NBTTagList list = nbt.getTagList("fields", 10);
            for(int i = 0; i < list.tagCount(); i++) {
                final NBTTagCompound entryTag = list.getCompoundTagAt(i);
                final String key = entryTag.getString("key");
                final ActionField actionField = ActionField.find(key, null);
                if(actionField != null && entryTag.hasKey("value")) {
                    this.fieldValueMap.put(actionField, actionField.load(entryTag.getTag("value")));
                }
            }
        }
    }
}
