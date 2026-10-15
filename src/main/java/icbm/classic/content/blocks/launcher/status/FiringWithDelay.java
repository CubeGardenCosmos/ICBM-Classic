package icbm.classic.content.blocks.launcher.status;

import com.google.common.collect.ImmutableList;
import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;
import java.util.Collection;

public class FiringWithDelay implements IActionStatus, ISavable {

    public static final Identifier REG_NAME = ICBMConstants.id("firing.delayed");
    private static final ImmutableList<MetaTag> TAGS = ImmutableList.of(ActionStatusTypes.BLOCKING, ActionStatusTypes.WAITING);

    private int delay;

    public FiringWithDelay() {
    }

    public FiringWithDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public Collection<MetaTag> getTypeTags() {
        return TAGS;
    }

    @Override
    public Component message() {
        return Component.translatable(LauncherLangs.STATUS_FIRING_DELAYED, delay);
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void save(ValueOutput out) {
        out.putInt("delay", delay);
    }

    @Override
    public void load(ValueInput in) {
        delay = in.getIntOr("delay", 0);
    }
}
