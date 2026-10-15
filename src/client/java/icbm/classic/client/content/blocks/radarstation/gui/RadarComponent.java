package icbm.classic.client.content.blocks.radarstation.gui;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import net.minecraft.client.Minecraft;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.blocks.radarstation.data.RadarDotType;
import icbm.classic.content.blocks.radarstation.data.RadarRenderData;
import icbm.classic.content.blocks.radarstation.data.RadarRenderDot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class RadarComponent implements IGuiComponent
{
    private static final int MARKER_COLOR = 0xFF00FF00;
    private static final int HOSTILE_COLOR = 0xFFFF0000;
    private static final int INCOMING_COLOR = 0xFF0000FF;
    private static final int TRIGGER_RANGE = 0xFFFFFF00;
    private final TileRadarStation tile;
    private final int x;
    private final int y;
    private GuiContainerBase<?> container;
    private int meterSpacing;

    public RadarComponent(TileRadarStation tile, int x, int y)
    {
        this.tile = tile;
        this.x = x;
        this.y = y;
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        this.container = container;
    }

    @Override
    public void onUpdate()
    {
        meterSpacing = (int) Math.floor((tile.getDetectionRange() / 100f) * 40);
    }

    @Override
    public void drawForegroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        final var dots = tile.getRadarRenderData().getDots();
        final var font = Minecraft.getInstance().font;
        graphics.text(font, Component.literal(meterSpacing + "m"), x + 56, y + 46, MARKER_COLOR);
        graphics.text(font, Component.literal(Long.toString(dots.stream().filter(dot -> dot.type() == RadarDotType.HOSTILE).count())), x + 56, y + 2, HOSTILE_COLOR);
        graphics.text(font, Component.literal(Long.toString(dots.stream().filter(dot -> dot.type() == RadarDotType.INCOMING).count())), x + 56, y + 14, INCOMING_COLOR);
    }

    @Override
    public void drawBackgroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        final int half = RadarRenderData.UV_SIZE / 2;
        final int left = container.getGuiLeft() + x;
        final int top = container.getGuiTop() + y;
        graphics.fill(left, top, left + RadarRenderData.UV_SIZE + 1, top + RadarRenderData.UV_SIZE + 1, 0xFF000000);
        final int gx = left + half;
        final int gy = top + half;
        final int range = Math.max(1, tile.getDetectionRange());
        final int spacingMeters = Math.max(1, meterSpacing);
        final int lineCount = (int) Math.floor(range / (float) spacingMeters) * 2 + 1;
        final float lineSpacing = (spacingMeters / (float) range) * half;
        final int lx = gx - (int) ((lineCount / 2) * lineSpacing);
        final int ly = gy - (int) ((lineCount / 2) * lineSpacing);
        for (int i = 0; i < lineCount; i++)
        {
            final int px = lx + (int) Math.floor(i * lineSpacing);
            graphics.fill(px, gy - half, px + 1, gy + half, MARKER_COLOR);
            final int py = ly + (int) Math.floor(i * lineSpacing);
            graphics.fill(gx - half, py, gx + half, py + 1, MARKER_COLOR);
        }
        final int trigger = (int) Math.ceil((tile.getTriggerRange() / (float) range) * half);
        graphics.fill(gx - trigger, gy + trigger, gx + trigger, gy + trigger + 1, TRIGGER_RANGE);
        graphics.fill(gx - trigger, gy - trigger - 1, gx + trigger, gy - trigger, TRIGGER_RANGE);
        graphics.fill(gx - trigger - 1, gy - trigger, gx - trigger, gy + trigger, TRIGGER_RANGE);
        graphics.fill(gx + trigger, gy - trigger, gx + trigger + 1, gy + trigger, TRIGGER_RANGE);
        for (RadarRenderDot dot : tile.getRadarRenderData().getDots())
        {
            final int px = gx + dot.x();
            final int py = gy + dot.y();
            final int color = dot.type() == RadarDotType.MARKER ? MARKER_COLOR : switch (Byte.toUnsignedInt(dot.status()))
            {
                case 0 -> 0xFF00FF00;
                case 1 -> 0xFFFFFF00;
                case 2 -> 0xFFFF0000;
                case 3 -> 0xFF808080;
                default -> 0;
            };
            if (color != 0)
            {
                final int size = dot.type() == RadarDotType.MARKER ? 1 : 2;
                graphics.fill(px, py, px + size, py + size, color);
            }
        }
    }
}
