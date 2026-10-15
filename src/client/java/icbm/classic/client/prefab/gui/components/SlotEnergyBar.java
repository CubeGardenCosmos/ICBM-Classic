package icbm.classic.client.prefab.gui.components;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Energy bar of the machine GUIs, 1.12 {@code icbm.classic.prefab.gui.components.SlotEnergyBar} ported onto
 * {@link GuiGraphicsExtractor}.
 * <p>
 * The bar is sampled from the background texture of the GUI like in 1.12 ({@code 256 - ENERGY_BAR_WIDTH, 0}),
 * and the tooltip texts are the S2.10 names of the 1.12 keys ({@code icbmclassic.gui.energy} and its variants,
 * already present in {@code en_us.json}).
 */
public class SlotEnergyBar implements IGuiComponent, IToolTip
{
    //UV
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 2;

    //1.12 keys gui.icbmclassic:energy(.withCost/.withAction/.withCostAndAction), renamed by S2.10
    private static final String TOOLTIP_FORMAT = "icbmclassic.gui.energy";
    private static final String TOOLTIP_FORMAT_COST = "icbmclassic.gui.energy.withCost";
    private static final String TOOLTIP_FORMAT_ACTION = "icbmclassic.gui.energy.withAction";
    private static final String TOOLTIP_FORMAT_COST_ACTION = "icbmclassic.gui.energy.withCostAndAction";

    private final int x;
    private final int y;

    /** Energy getter */
    private final Supplier<Integer> energyGetter;
    /** Max energy getter */
    private final Supplier<Integer> energyMaxGetter;
    /** Cost per tick getter */
    private Supplier<Integer> tickingCostGetter;
    /** Cost per action getter */
    private Supplier<Integer> actionCostGetter;

    /** Parent */
    private GuiContainerBase<?> container;

    private float energyPercent = 0;

    private int prevEnergy = 0;
    private int prevMaxEnergy = 0;
    private boolean prevShift = false;

    private Component tooltip;


    public SlotEnergyBar(int x, int y, Supplier<Integer> energyGetter, Supplier<Integer> energyMaxGetter)
    {
        this.x = x;
        this.y = y;
        this.energyGetter = energyGetter;
        this.energyMaxGetter = energyMaxGetter;
    }

    public SlotEnergyBar withTickingCost(Supplier<Integer> getter)
    {
        this.tickingCostGetter = getter;
        return this;
    }

    public SlotEnergyBar withActionCost(Supplier<Integer> getter)
    {
        this.actionCostGetter = getter;
        return this;
    }

    @Override
    public void onUpdate()
    {
        // Cached data to avoid redoing each frame render
        final int energy = energyGetter.get();
        final int maxEnergy = energyMaxGetter.get();
        final int tickingCost = Optional.ofNullable(tickingCostGetter).map(Supplier::get).orElse(0);
        final int actionCost = Optional.ofNullable(actionCostGetter).map(Supplier::get).orElse(0);
        //1.12: GuiScreen.isShiftKeyDown()
        final boolean shift = Minecraft.getInstance().hasShiftDown();

        if (energy != prevEnergy || maxEnergy != prevMaxEnergy || shift != prevShift)
        {
            prevEnergy = energy;
            prevMaxEnergy = maxEnergy;
            prevShift = shift;

            energyPercent = maxEnergy == 0 ? 0 : energy / (float) maxEnergy;

            String translationToUse = TOOLTIP_FORMAT;
            if (tickingCost > 0)
            {
                translationToUse = TOOLTIP_FORMAT_COST;
                if (actionCost > 0)
                {
                    translationToUse = TOOLTIP_FORMAT_COST_ACTION;
                }
            }
            else if (actionCost > 0)
            {
                translationToUse = TOOLTIP_FORMAT_ACTION;
            }

            if (shift)
            {
                tooltip = Component.translatable(translationToUse, energy, maxEnergy, -tickingCost, -actionCost);
            }
            else
            {
                tooltip = Component.translatable(translationToUse, formatEnergy(energy), formatEnergy(maxEnergy), formatEnergy(-tickingCost), formatEnergy(-actionCost));
            }
        }
    }

    private static String formatEnergy(final int energy)
    {
        final boolean neg = energy < 0;
        int number = Math.abs(energy);
        String type = "";
        // Giga
        if (number >= 1_000_000_000)
        {
            number = number / 1_000_000_000;
            type = "G";
        }
        // Mega
        else if (number >= 1_000_000)
        {
            number = number / 1_000_000;
            type = "M";
        }
        // Kilo
        else if (number >= 1_000)
        {
            number = number / 1_000;
            type = "k";
        }
        return String.format("%s%d%s", neg ? "-" : "", number, type); //TODO add decimal place
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        this.container = container;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        // Calculate bar ratio
        final float barRatio = (float) Math.floor(ENERGY_BAR_WIDTH * energyPercent);

        // Calculate bar width
        final int minBar = energyPercent > 0 ? 1 : 0;
        final int renderWidth = (int) Math.min(Math.max(minBar, barRatio), ENERGY_BAR_WIDTH);

        //1.12: bindTexture(container.getBackground()) + drawTexturedModalRect(guiLeft + x, guiTop + y,
        //      256 - ENERGY_BAR_WIDTH, 0, renderWidth, ENERGY_BAR_HEIGHT)
        graphics.blit(RenderPipelines.GUI_TEXTURED, container.getBackground(), container.getGuiLeft() + x, container.getGuiTop() + y,
            256 - ENERGY_BAR_WIDTH, 0, renderWidth, ENERGY_BAR_HEIGHT,
            GuiContainerBase.GUI_TEXTURE_SIZE, GuiContainerBase.GUI_TEXTURE_SIZE);
    }

    @Override
    public boolean isWithin(int mouseX, int mouseY)
    {
        final int cursorX = mouseX - container.getGuiLeft();
        final int cursorY = mouseY - container.getGuiTop();
        return cursorX >= this.x && cursorY >= this.y && cursorX < this.x + ENERGY_BAR_WIDTH && cursorY < this.y + ENERGY_BAR_HEIGHT;
    }

    @Override
    public Component getTooltip()
    {
        return tooltip;
    }
}
