package icbm.classic.client.prefab.gui.button;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.function.Supplier;

/**
 * Icon that covers another widget to show it is disabled, 1.12
 * {@code icbm.classic.prefab.gui.button.DisableButton} ported onto {@link GuiButtonBase}.
 * <p>
 * The icon is sampled from {@code gui_components.png} like in 1.12 ({@code drawButton} drew the icon instead of
 * the vanilla button), and the click is only accepted while alt is held (1.12 {@code mousePressed}).
 */
public class DisableButton extends GuiButtonBase<DisableButton> implements IGuiComponent, IToolTip
{
    // Icon this designed to cover is 13x13
    private static final int WIDTH = 15;
    private static final int HEIGHT = 15;

    private static final int UV_Y = 241;
    private static final int UV_X = 0;

    /** Checks if disable status should render, not the same as #enabled or #visble. Even though this causes it to stop rendering is true */
    private final Supplier<Boolean> shouldShowAsDisabled;

    public DisableButton(int x, int y, Supplier<Boolean> shouldShowAsDisabled)
    {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.shouldShowAsDisabled = shouldShowAsDisabled;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            //1.12: hovered = mouse over the button, then the icon is drawn when alt is held or when the state
            //says the covered widget is disabled. The vanilla button sprite of GuiButtonBase is not drawn.
            final boolean hovered = this.isMouseOver(mouseX, mouseY);
            if (hovered && Minecraft.getInstance().hasAltDown())
            {
                blitIcon(graphics, UV_X + WIDTH, UV_Y);
            }
            else if (shouldShowAsDisabled.get())
            {
                blitIcon(graphics, UV_X, UV_Y);
            }
        }
    }

    private void blitIcon(GuiGraphicsExtractor graphics, int u, int v)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GuiContainerBase.COMPONENTS_TEXTURE, getX(), getY(),
            u, v, this.width, this.height, GuiContainerBase.GUI_TEXTURE_SIZE, GuiContainerBase.GUI_TEXTURE_SIZE);
    }

    /** 1.12 {@code mousePressed} required the alt key */
    @Override
    protected boolean isValidClickButton(MouseButtonInfo button)
    {
        return super.isValidClickButton(button) && Minecraft.getInstance().hasAltDown();
    }
}
