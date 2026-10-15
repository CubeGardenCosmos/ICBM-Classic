package icbm.classic.client.gui.widget;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import icbm.classic.lib.energy.EnergyFormat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.LongSupplier;

/** Reusable horizontal FE meter for machine container screens. */
public final class EnergyBar implements IGuiComponent, IToolTip
{
    private static final int WIDTH = 16;
    private static final int HEIGHT = 10;
    private static final int BACKGROUND = 0xFF292929;
    private static final int BORDER = 0xFF777777;
    private static final int FILL = 0xFF39C978;

    private final int x;
    private final int y;
    private final LongSupplier stored;
    private final LongSupplier capacity;
    private GuiContainerBase<?> screen;

    public EnergyBar(int x, int y, LongSupplier stored, LongSupplier capacity)
    {
        this.x = x;
        this.y = y;
        this.stored = stored;
        this.capacity = capacity;
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        screen = container;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        final long max = Math.max(0L, capacity.getAsLong());
        final long amount = Math.max(0L, Math.min(stored.getAsLong(), max));
        final int left = screen.getGuiLeft() + x;
        final int top = screen.getGuiTop() + y;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, BORDER);
        graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + HEIGHT - 1, BACKGROUND);
        final int innerWidth = WIDTH - 2;
        final int filled = max == 0 ? 0 : (int) ((double) amount / max * innerWidth);
        if (filled > 0)
        {
            graphics.fill(left + 1, top + 1, left + 1 + filled, top + HEIGHT - 1, FILL);
        }
    }

    @Override
    public boolean isWithin(int mouseX, int mouseY)
    {
        final int relativeX = mouseX - screen.getGuiLeft();
        final int relativeY = mouseY - screen.getGuiTop();
        return relativeX >= x && relativeY >= y && relativeX < x + WIDTH && relativeY < y + HEIGHT;
    }

    @Override
    public Component getTooltip()
    {
        return Component.translatable("icbmclassic.energy.stored",
                EnergyFormat.formatWithCapacity(stored.getAsLong(), capacity.getAsLong()));
    }
}
