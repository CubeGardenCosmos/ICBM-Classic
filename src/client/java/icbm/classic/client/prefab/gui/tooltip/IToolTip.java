package icbm.classic.client.prefab.gui.tooltip;

import net.minecraft.network.chat.Component;

/**
 * Component that shows a tooltip while the mouse is inside its bounds, 1.12
 * {@code icbm.classic.prefab.gui.tooltip.IToolTip} with {@code ITextComponent} replaced by {@link Component}.
 */
public interface IToolTip
{
    /**
     * Checks if the tooltip contains the cursor
     *
     * @param x of the mouse relative to the screen
     * @param y of the mouse relative to the screen
     * @return true if within component bounds
     */
    boolean isWithin(int x, int y);

    /**
     * Tooltip to display
     *
     * @return tooltip or null to ignore
     */
    Component getTooltip();
}
