package icbm.classic.client.prefab.gui.tooltip;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.lib.transform.region.Rectangle;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Simple tooltip component for showing users additional information, 1.12
 * {@code icbm.classic.prefab.gui.tooltip.TooltipBase} ported onto {@link GuiGraphicsExtractor}.
 * <p>
 * The hover delay and the bounds check are kept as they were. 1.12 collected the text of the hovered component
 * and drew the tooltip background itself from {@code GuiContainerBase}; 26.3 hands the component to the
 * extractor ({@code GuiGraphicsExtractor#setTooltipForNextFrame}) which draws the vanilla background and wraps
 * the text.
 */
public abstract class TooltipBase implements IToolTip, IGuiComponent
{
    /**
     * Bound box, relative to top-left of the container
     */
    @Getter
    private final Rectangle bounds;

    /**
     * Delay in seconds to wait to show tooltip
     */
    @Getter
    private float hoverDelay;

    private GuiContainerBase<?> container;

    /**
     * Is mouse over component
     */
    @Getter
    private boolean isHovering = false;
    /**
     * Current hover tick in seconds
     */
    private float hoveringTicks = 0;

    public TooltipBase(int x, int y, int width, int height)
    {
        this.bounds = new Rectangle(x, y, x + width, y + height);
    }

    public TooltipBase withDelay(float delay)
    {
        this.hoverDelay = delay;
        return this;
    }

    @Override
    public void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        isHovering = isWithin(mouseX, mouseY);
        if (isHovering)
        {
            hoveringTicks += partialTicks;
        }
        else
        {
            hoveringTicks = 0;
        }

        //1.12 drew a debug rectangle here (commented out); the 26.3 counterpart is
        //graphics.fill(bounds.getMin().xi() + container.getGuiLeft(), ...)
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        this.container = container;
    }

    @Override
    public boolean isWithin(int x, int y)
    {
        //1.12: bounds.isWithin(x - container.getGuiLeft(), y - container.getGuiTop())
        return bounds.isWithin(x - container.getGuiLeft(), y - container.getGuiTop());
    }

    @Override
    public final Component getTooltip()
    {
        if (hoveringTicks < hoverDelay)
        {
            return null;
        }
        return getActualTooltip();
    }

    protected abstract Component getActualTooltip();
}
