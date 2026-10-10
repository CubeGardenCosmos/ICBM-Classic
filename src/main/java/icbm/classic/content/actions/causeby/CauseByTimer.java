package icbm.classic.content.actions.causeby;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.content.missile.logic.source.cause.ActionCause;
import icbm.classic.lib.actions.conditionals.timer.TimerCondition;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

/**
 * Cause metadata to note a timer was used such as {@link TimerCondition}
 */
@NoArgsConstructor
public class CauseByTimer extends ActionCause implements IActionCause {

    public static final Identifier REG_NAME = ICBMConstants.id("conditional.timer");

    @Setter @Getter
    private int timer;

    public CauseByTimer(String name, int timer) {
        setName(name);
        this.timer = timer;
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

    private static final SaveHandler<CauseByTimer> SAVE_LOGIC = new SaveHandler<CauseByTimer>()
        .mainRoot()
        /* */.nodeInteger("timer", CauseByTimer::getTimer, CauseByTimer::setTimer)
        .base();
}
