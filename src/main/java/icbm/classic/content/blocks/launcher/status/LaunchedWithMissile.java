package icbm.classic.content.blocks.launcher.status;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.missiles.IMissile;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NonNull;

/** Launched status retaining a runtime reference to the fired missile. */
@Data
public class LaunchedWithMissile implements IActionStatus {
    public static final Identifier REG_NAME = ICBMConstants.id("launcher.launched.missile_data");

    @Accessors(chain = true)
    private IMissile missile;

    @Override
    public Component message() {
        return LauncherStatus.LAUNCHED.message();
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }
}
