package icbm.classic.client.prefab.gui.tooltip;

import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Simple tooltip component for showing users additional information, 1.12
 * {@code icbm.classic.prefab.gui.tooltip.Tooltip} with {@code ITextComponent} replaced by {@link Component}.
 */
public class Tooltip extends TooltipBase
{
    /**
     * Supplier for getting tooltip in real time
     */
    private final Supplier<Component> tooltip;

    public Tooltip(int x, int y, int width, int height, Supplier<Component> tooltip)
    {
        super(x, y, width, height);
        this.tooltip = tooltip;
    }

    @Override
    protected Component getActualTooltip()
    {
        return tooltip.get();
    }
}
