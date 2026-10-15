package icbm.classic.client.prefab.gui.button;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import icbm.classic.lib.transform.region.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Button of the ICBM GUIs with an action and a tooltip, 1.12
 * {@code icbm.classic.prefab.gui.button.GuiButtonBase} ported onto {@link Button}.
 * <p>
 * 1.12 extended Forge's {@code GuiButton} which drew the vanilla button sprite and the label; 26.3
 * {@code AbstractButton} keeps the sprite ({@code extractDefaultSprite}) and leaves the label to the subclass,
 * which is what {@link #extractContents} does here. The 1.12 {@code buttonId} is not kept, 26.3 widgets are
 * identified by their instance. The 1.12 {@code actionPerformed} dispatch of the container became
 * {@link #onPress(InputWithModifiers)}.
 *
 * @param <B> type of the subclass, for chaining
 */
public class GuiButtonBase<B extends GuiButtonBase<B>> extends Button implements IGuiComponent, IToolTip
{
    private ActionTrigger action;

    private final Rectangle bounds;
    private Supplier<Component> tooltip;

    private GuiContainerBase<?> container;

    public GuiButtonBase(int x, int y, int width, int height, Component message)
    {
        //1.12 passed the button text; the action is set afterwards through setAction() and dispatched by
        //onPress(...), the no op handler of the super constructor is never used
        super(x, y, width, height, message, button -> {}, Button.DEFAULT_NARRATION);
        bounds = new Rectangle(x, y, x + width, y + height);
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        this.container = container;
    }

    public B setAction(ActionTrigger action)
    {
        this.action = action;
        return (B) this;
    }

    public B setTooltip(Supplier<Component> tooltip)
    {
        this.tooltip = tooltip;
        return (B) this;
    }

    /** 1.12 {@code GuiContainerBase#actionPerformed} called this */
    public void triggerAction()
    {
        if (action != null)
        {
            action.trigger();
        }
    }

    @Override
    public void onPress(InputWithModifiers input)
    {
        triggerAction();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        //1.12 GuiButton#drawButton: the vanilla sprite, then the label
        extractDefaultSprite(graphics);
        drawLabel(graphics);
    }

    /** The label of the vanilla button, 1.12 {@code GuiButton#drawCenteredString} */
    private void drawLabel(GuiGraphicsExtractor graphics)
    {
        final Component message = getMessage();
        if (message == null || message.getString().isEmpty())
        {
            return;
        }
        final Font font = Minecraft.getInstance().font;
        final int x = getX() + getWidth() / 2 - font.width(message) / 2;
        final int y = getY() + (getHeight() - 8) / 2;
        graphics.text(font, message, x, y, this.active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
    }

    @Override
    public boolean isWithin(int x, int y)
    {
        return bounds.isWithin(x, y);
    }

    @Override
    public Component getTooltip()
    {
        if (tooltip != null)
        {
            return tooltip.get();
        }
        return null;
    }

    @FunctionalInterface
    public interface ActionTrigger
    {
        void trigger();
    }
}
