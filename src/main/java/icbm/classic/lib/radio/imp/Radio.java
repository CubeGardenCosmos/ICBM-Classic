package icbm.classic.lib.radio.imp;

import icbm.classic.api.radio.IRadio;
import icbm.classic.api.radio.IRadioChannelAccess;
import icbm.classic.lib.saving.ISavable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.RandomStringUtils;


public abstract class Radio implements IRadio, ISavable, IRadioChannelAccess {

    private String channel;

    @Getter @Setter
    private boolean isDisabled = false;

    @Override
    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        if(channel == null) {
            this.channel = RandomStringUtils.random(4, true, true);
        }
        return channel;
    }

    @Override
    public void save(ValueOutput out) {
        if(channel != null) {
            out.putString("channel", channel);
        }
        out.putBoolean("disabled", isDisabled);
    }

    @Override
    public void load(ValueInput in) {
        this.channel = in.getStringOr("channel", "");
        this.isDisabled = in.getBooleanOr("disabled", false);
    }
}
