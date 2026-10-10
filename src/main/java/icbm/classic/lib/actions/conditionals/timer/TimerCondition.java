package icbm.classic.lib.actions.conditionals.timer;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.IConditionCause;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.lib.actions.conditionals.Condition;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.saving.SaveHandler;
import lombok.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

@NoArgsConstructor
@AllArgsConstructor
public class TimerCondition extends Condition implements IConditionCause {
    public static final Identifier REG_NAME = ICBMConstants.id("timer");

    @Getter @Setter
    private int target = 0;

    @Getter @Setter(AccessLevel.PRIVATE)
    private int current = 0;

    private IActionStatus statusCache; //TODO cache on action status for reduce churn

    @Override
    public void onTick()
    {
        if(current < target) {
            current++;
            statusCache = null;
        }
    }

    @Override
    public void reset() {
        this.current = 0;
    }

    @Override
    public IActionStatus getCondition() {
        if(current <= 0) {
            return ActionResponses.READY;
        }
        else if(statusCache == null) {
           statusCache = new TimerTickingStatus(current, target);
        }
        return statusCache;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<TimerCondition> SAVE_LOGIC = new SaveHandler<TimerCondition>()
        .mainRoot()
        /* */.nodeInteger("tick_current", TimerCondition::getCurrent, TimerCondition::setCurrent)
        /* */.nodeInteger("tick_target", TimerCondition::getTarget, TimerCondition::setTarget)
        .base();

    @Override
    public IActionCause getCause() {
        return new CauseByTimer(this.getName(), target);
    }
}
