package icbm.classic.client.content.blocks.launcher.cruise.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/** Cruise launch button with the legacy pre-check driven enabled state. */
public final class LaunchButton
{
    private final Button button;
    private final BooleanSupplier enabledCheck;

    public LaunchButton(int x, int y, Component label, Runnable action, BooleanSupplier enabledCheck)
    {
        this.enabledCheck = enabledCheck;
        this.button = Button.builder(label, ignored -> action.run()).bounds(x, y, 48, 20).build();
        update();
    }

    public Button widget()
    {
        return button;
    }

    public void update()
    {
        button.active = enabledCheck.getAsBoolean();
    }
}
