package icbm.classic.lib.actions.conditionals.timer;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;
import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimerTickingStatus implements IActionStatus, ISavable {

    public static final Identifier REG_NAME = ICBMConstants.id("conditional.timer.ticking");
    public static final ImmutableList<MetaTag> TAGS = ImmutableList.of(ActionStatusTypes.WAITING, ActionStatusTypes.BLOCKING);
    //TODO consider caching common ticks as 0/10 to 10/10 will be reused a lot

    private int tickCurrent = 0;
    private int tickTarget = 0;

    @NonNull
    @Override
    public Collection<MetaTag> getTypeTags() {
        return TAGS;
    }

    @Override
    public Component message() {
        return Component.translatable(getTranslationKey(), tickCurrent, tickTarget);
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

    private static final SaveHandler<TimerTickingStatus> SAVE_LOGIC = new SaveHandler<TimerTickingStatus>()
        .mainRoot()
        /* */.nodeInteger("tick_current", TimerTickingStatus::getTickCurrent, TimerTickingStatus::setTickCurrent)
        /* */.nodeInteger("tick_target", TimerTickingStatus::getTickTarget, TimerTickingStatus::setTickTarget)
        .base();
}
