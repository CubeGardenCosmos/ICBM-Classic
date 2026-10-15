package icbm.classic.client.prefab.gui.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Tooltip that shows a second text while shift is held, 1.12
 * {@code icbm.classic.prefab.gui.tooltip.TooltipTranslations} with {@code ITextComponent} replaced by
 * {@link Component} and {@code GuiScreen.isShiftKeyDown()} by {@code Minecraft#hasShiftDown()} (26.3 moved the
 * modifier state off {@code Screen}).
 */
public class TooltipTranslations extends TooltipBase
{
    private final Component normalTooltip;
    private Component shiftTooltip;

    public TooltipTranslations(int x, int y, int width, int height, Component tooltip)
    {
        super(x, y, width, height);
        this.normalTooltip = tooltip;
    }

    public TooltipTranslations withShift(Component shiftTooltip)
    {
        this.shiftTooltip = shiftTooltip;
        return this;
    }

    @Override
    protected Component getActualTooltip()
    {
        if (Minecraft.getInstance().hasShiftDown() && shiftTooltip != null)
        {
            return shiftTooltip;
        }
        return normalTooltip;
    }
}
