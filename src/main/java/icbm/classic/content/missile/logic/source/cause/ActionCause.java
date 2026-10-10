package icbm.classic.content.missile.logic.source.cause;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class ActionCause implements IActionCause, ISavable {

    private IActionCause parent;

    @Setter @Getter
    private String name;

    public IActionCause setPreviousCause(IActionCause parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public IActionCause getPreviousCause() {
        return parent;
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<ActionCause> SAVE_LOGIC = new SaveHandler<ActionCause>()
        .mainRoot()
        /* */.nodeString("name", ActionCause::getName, ActionCause::setName)
        /* */.nodeBuildableObject("parent", () -> ICBMClassicAPI.ACTION_CAUSE_REGISTRY, ActionCause::getPreviousCause, ActionCause::setPreviousCause)
        .base();
}
