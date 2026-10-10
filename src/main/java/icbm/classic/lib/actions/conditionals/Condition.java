package icbm.classic.lib.actions.conditionals;

import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class Condition implements ICondition, ISavable {
    /**
     * Display name
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    private String name;

    @Override
    public Component getDisplayName() {
        if (name != null) {
            return Component.translatable(getTranslationKey() + ".user_defined_name", name);
        }
        return Component.translatable(getTranslationKey());
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<Condition> SAVE_LOGIC = new SaveHandler<Condition>()
        .mainRoot()
        /* */.nodeString("display_name", Condition::getName, Condition::setName)
        .base();
}
