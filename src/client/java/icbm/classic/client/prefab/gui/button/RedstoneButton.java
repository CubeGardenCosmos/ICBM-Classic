package icbm.classic.client.prefab.gui.button;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Icon that shows the redstone output state of a machine, 1.12
 * {@code icbm.classic.prefab.gui.button.RedstoneButton} ported onto {@link GuiButtonBase}.
 * <p>
 * The state and hover offsets of the 1.12 {@code drawButton} are kept; {@code enabled} is 26.3
 * {@code active} and {@code hovered} is {@code AbstractWidget#isMouseOver}.
 */
public class RedstoneButton extends GuiButtonBase<RedstoneButton>
{
    private final Supplier<Boolean> redstoneState;

    public RedstoneButton(int x, int y, Supplier<Boolean> redstoneState)
    {
        super(x, y, 12, 12, Component.empty());
        this.redstoneState = redstoneState;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            // Get stater
            final boolean redstoneEnabled = redstoneState.get();

            // Handle state
            int offset = 0;
            if (!this.active)
            {
                offset = 1;
            }
            else if (this.isMouseOver(mouseX, mouseY))
            {
                offset = 2;
            }

            //1.12: drawTexturedModalRect(x, y, (!redstoneEnabled ? 12 : 0), offset * 12, 12, 12)
            graphics.blit(RenderPipelines.GUI_TEXTURED, GuiContainerBase.COMPONENTS_TEXTURE, getX(), getY(),
                (!redstoneEnabled ? 12 : 0), offset * 12, 12, 12,
                GuiContainerBase.GUI_TEXTURE_SIZE, GuiContainerBase.GUI_TEXTURE_SIZE);
        }
    }
}
