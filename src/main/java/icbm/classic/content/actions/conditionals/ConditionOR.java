package icbm.classic.content.actions.conditionals;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.conditions.IConditionCause;
import icbm.classic.api.actions.conditions.IConditionLayer;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

public class ConditionOR implements IConditionLayer, IConditionCause, ISavable {

    public static final Identifier REG_NAME = ICBMConstants.id("composition.or");

    private final List<ICondition> children = new ArrayList<>();

    @Getter @Setter @Accessors(chain = true)
    private boolean shortCircuit = true;

    @Override
    public IActionStatus getCondition() {
        boolean isBlock = false;
        for(ICondition condition: children) {
            final IActionStatus status = condition.getCondition();

            if(status.isType(ActionStatusTypes.BLOCKING)) {
                isBlock = true;
            }

            if(shortCircuit) {
                return status;
            }
        }
        return isBlock ? ActionResponses.WAITING : ActionResponses.READY; //TODO provide composition status
    }

    @Override
    public void onTick() {
        for(ICondition condition: children) {
            condition.onTick();

            // If not blocking, short-circuit and don't tick next
            final IActionStatus status = condition.getCondition();
            if(shortCircuit && !status.isType(ActionStatusTypes.BLOCKING)) {
               return;
            }
        }
    }

    @Override
    public List<ICondition> getConditions() {
        return children;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public IActionCause getCause() {
        return null; //TODO implement
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<ConditionOR> SAVE_LOGIC = new SaveHandler<ConditionOR>()
        .mainRoot()
        /* */.nodeBuildableObjectList("conditions", () -> ICBMClassicAPI.CONDITION_REGISTRY, ConditionOR::getConditions)
        /* */.nodeBoolean("short_circuit", ConditionOR::isShortCircuit, ConditionOR::setShortCircuit)
        .base();
}
