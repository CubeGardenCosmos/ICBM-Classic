package icbm.classic.content.actions.conditionals;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.actions.status.MissingFieldStatus;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

public class ConditionalImpact implements ICondition, ISavable {
    public static final Identifier REG_NAME = ICBMConstants.id("impact");

    private IActionFieldProvider provider;

    @Getter @Setter @Accessors(chain = true)
    private boolean impactDesired = true;

    @Override
    public void init(IActionFieldProvider provider) {
       this.provider = provider;
    }

    @Override
    public IActionStatus getCondition() {
        if(provider == null || !provider.hasField(ActionFields.IMPACTED)) {
            return new MissingFieldStatus().setSource("ConditionalImpact#provider").setField(ActionFields.IMPACTED.getKey());
        }
        Boolean value = provider.getValue(ActionFields.IMPACTED);
        if(value == null || value != impactDesired) {
            return ActionResponses.WAITING;
        }
        return ActionResponses.READY;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<ConditionalImpact> SAVE_LOGIC = new SaveHandler<ConditionalImpact>()
        .mainRoot()
        /* */.nodeBoolean("impact_desired", ConditionalImpact::isImpactDesired, ConditionalImpact::setImpactDesired)
        .base();
}
