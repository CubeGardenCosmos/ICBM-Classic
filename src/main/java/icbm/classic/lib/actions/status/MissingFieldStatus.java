package icbm.classic.lib.actions.status;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MissingFieldStatus extends ImmutableStatus implements ISavable {

    public static final Identifier REG_NAME = ICBMConstants.id("missing.field");

    @Setter @Getter @Accessors(chain = true)
    private String source;
    @Setter @Getter @Accessors(chain = true)
    private String field;

    public MissingFieldStatus() {
        super(REG_NAME, ActionStatusTypes.BLOCKING, ActionStatusTypes.ERROR);
    }

    public MissingFieldStatus(Identifier regName, String source, String field) {
        super(regName, ActionStatusTypes.BLOCKING, ActionStatusTypes.ERROR);
        this.source = source;
        this.field = field;
    }

    @Override
    public Component message() {
        if(textComponent == null) {
            textComponent = Component.translatable(translationKey, source, field);
        }
        return textComponent;
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<MissingFieldStatus> SAVE_LOGIC = new SaveHandler<MissingFieldStatus>()
        .mainRoot()
        /* */.nodeString("source", MissingFieldStatus::getSource, MissingFieldStatus::setSource)
        /* */.nodeString("field", MissingFieldStatus::getField, MissingFieldStatus::setField)
        .base();
}
