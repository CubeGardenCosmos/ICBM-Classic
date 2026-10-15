package icbm.classic.client.prefab.gui;

import com.mojang.blaze3d.platform.InputConstants;
import icbm.classic.ICBMConstants;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;


import java.util.ArrayList;
import java.util.List;

/**
 * Base screen of the ICBM machines, 1.12 {@code icbm.classic.prefab.gui.GuiContainerBase} ported onto
 * {@link AbstractContainerScreen} in the client source set (porting-guide.md §7).
 * <p>
 * The 26.3 render entry points of a container screen are {@code extractBackground} (the dim layer, 1.12
 * {@code drawDefaultBackground}), {@code extractContents} (the GUI panel, the labels and the slots),
 * {@code extractLabels} (1.12 {@code drawGuiContainerForegroundLayer}) and {@code extractRenderState}; the
 * porting-guide skeleton called the panel method {@code extractBackground}, javap (
 * {@code AbstractContainerScreen} of 26.3) shows it is {@code extractContents} that vanilla overrides for the
 * panel, so the panel is drawn from {@link #extractBackground} here, before the slots, exactly like 1.12.
 * <p>
 * Kept from 1.12: the component list with {@link #addComponent(IGuiComponent)}, the background texture of
 * {@link #getBackground()}, the string helpers, {@code drawRectWithScaledWidth} and the insert key toggle of
 * the slot ids.
 * <p>
 * Changed by 26.3:
 * <ul>
 *     <li>{@code xSize}/{@code ySize} are {@code imageWidth}/{@code imageHeight} of
 *     {@link AbstractContainerScreen} and {@code guiLeft}/{@code guiTop} are {@code leftPos}/{@code topPos}
 *     (accessors added because the components live in another package),</li>
 *     <li>drawing goes through {@link GuiGraphicsExtractor}, no OpenGL calls (golden rule),</li>
 *     <li>{@code Keyboard.enableRepeatEvents} in {@code onGuiClosed} is gone, 26.3 handles key repeat in the
 *     input layer,</li>
 *     <li>the manual tooltip rendering ({@code currentTooltipText} + {@code drawTooltip}) is replaced by
 *     {@code GuiGraphicsExtractor#setTooltipForNextFrame},</li>
 *     <li>components that are real widgets ({@code AbstractWidget} subclasses: buttons and text fields) are
 *     registered as renderable children, so vanilla draws them and feeds them the input; the component
 *     callbacks are only used for the components that are not widgets.</li>
 * </ul>
 */
public abstract class GuiContainerBase<T extends AbstractContainerMenu> extends AbstractContainerScreen<T>
{
    /** Texture sheet of the shared components (buttons, energy bar), 1.12 {@code COMPONENTS_TEXTURE} */
    public static final Identifier COMPONENTS_TEXTURE = ICBMConstants.id(ICBMConstants.GUI_DIRECTORY + "gui_components.png");

    /** Size of the GUI sheets, 1.12's {@code drawTexturedModalRect} assumed a 256x256 texture */
    public static final int GUI_TEXTURE_SIZE = 256;

    protected final List<IGuiComponent> components = new ArrayList<>();

    /** Debug toogle to render text for the ID and inventory ID for a slot */
    public boolean renderSlotDebugIDs = false;

    public GuiContainerBase(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
    public GuiContainerBase(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight)
    {
        super(menu, playerInventory, title, imageWidth, imageHeight);
    }

    /** Background texture of the GUI, 1.12 {@code getBackground()} */
    public abstract Identifier getBackground();

    /** 1.12 {@code GuiContainer#getGuiLeft()} */
    public int getGuiLeft()
    {
        return this.leftPos;
    }

    /** 1.12 {@code GuiContainer#getGuiTop()} */
    public int getGuiTop()
    {
        return this.topPos;
    }

    /** 1.12 {@code initGui()} cleared the button list, 26.3 {@code rebuildWidgets()} clears the widgets */
    @Override
    protected void init()
    {
        super.init();
        this.components.clear();
    }

    /**
     * Adds a component to the GUI.
     * <p>
     * 1.12 added buttons to {@code buttonList} so vanilla drew and clicked them and kept everything else in the
     * component list, which this method does with {@link #addRenderableWidget} for the 26.3 widgets.
     *
     * @param field to add
     * @param <C>   type of the component
     * @return the component, for chaining
     */
    protected <C extends IGuiComponent> C addComponent(C field)
    {
        if (field instanceof AbstractWidget widget)
        {
            this.addRenderableWidget(widget);
        }
        components.add(field);
        field.onAddedToHost(this);
        return field;
    }

    /** 1.12 {@code addButton(GuiButton)} */
    @Deprecated
    protected <C extends AbstractWidget> C addButton(C button)
    {
        if (button instanceof IGuiComponent component)
        {
            addComponent(component);
        }
        else
        {
            this.addRenderableWidget(button);
        }
        return button;
    }

    protected void drawString(GuiGraphicsExtractor graphics, Component text, int x, int y, int color)
    {
        //1.12: Minecraft.getMinecraft().fontRenderer.drawString(str, x, y, color)
        graphics.text(this.font, text, x, y, color);
    }

    protected void drawStringCentered(GuiGraphicsExtractor graphics, Component text, int x, int y, int color)
    {
        //1.12: x - (Minecraft.getMinecraft().fontRenderer.getStringWidth(str) / 2)
        drawString(graphics, text, x - (this.font.width(text) / 2), y, color);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // rendering
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 1.12 {@code drawGuiContainerBackgroundLayer}: the vanilla background, then the panel of the machine and
     * the component background layer.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks); //1.12: drawDefaultBackground()

        //1.12: bindTexture(getBackground()) + drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
        graphics.blit(RenderPipelines.GUI_TEXTURED, getBackground(), this.leftPos, this.topPos, 0.0F, 0.0F,
            this.imageWidth, this.imageHeight, GUI_TEXTURE_SIZE, GUI_TEXTURE_SIZE);

        components.forEach(component -> component.drawBackgroundLayer(graphics, mouseX, mouseY, partialTicks));
    }

    /**
     * 1.12 {@code drawGuiContainerForegroundLayer}, the extractor is already translated to the GUI origin and
     * the mouse position is absolute exactly like 1.12.
     */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        components.forEach(component -> component.drawForegroundLayer(graphics, mouseX, mouseY));
    }

    /**
     * 1.12 {@code drawScreen}: the components drawn after the container itself, and the tooltip of the hovered
     * component handed to the extractor (vanilla draws it, the hovered item slot still wins because the
     * container tooltip is extracted after this method).
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        components.forEach(component -> component.draw(graphics, mouseX, mouseY, partialTicks));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.extractContents(graphics, mouseX, mouseY, partialTicks);
        extractComponentTooltip(graphics, mouseX, mouseY);
    }

    private void extractComponentTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        if (components.isEmpty())
        {
            return;
        }
        for (IGuiComponent component : components)
        {
            if (component instanceof IToolTip toolTip && toolTip.isWithin(mouseX, mouseY))
            {
                final Component tooltip = toolTip.getTooltip();
                if (tooltip != null)
                {
                    graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }

    /**
     * Draws a rectangle with an increased or decreased width value
     * <p>
     * This works by duplicating the middle (3, width - 3) of the rectangle
     * <p>
     * 26.3 change: the texture has to be given to the extractor, 1.12 painted with whatever texture was bound.
     *
     * @param graphics  extractor to draw with
     * @param texture   texture sheet to sample
     * @param x         - render pos
     * @param y         - render pos
     * @param u         - x pos of the texture in it's texture sheet
     * @param v         - y pos of the texture in it's texture sheet
     * @param width     - width of the texture
     * @param height    - height of the texture
     * @param newWidth  - new width to render the rectangle, minimal size of 6
     */
    protected void drawRectWithScaledWidth(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height, int newWidth)
    {
        if (width > 0)
        {
            //If both widths are the same redirect to original call
            if (newWidth <= 0 || width == newWidth)
            {
                blit(graphics, texture, x, y, u, v, width, height);
            }

            //Size of the middle section of the image
            final int midWidth = width - 6;

            //Start cap of image rect
            blit(graphics, texture, x, y, u, v, 3, height);
            x += 3;

            //only render middle if it is larger than 6
            if (newWidth > 6)
            {
                //Loop over number of sections that need to be rendered
                int loops = newWidth / width;
                while (loops > 0)
                {
                    blit(graphics, texture, x, y, u + 3, v, midWidth, height);
                    x += midWidth;
                    loops -= 1;
                }

                //Check if there is a remainder that still needs rendered
                loops = newWidth % width;
                if (loops != 0)
                {
                    blit(graphics, texture, x, y, u + 3, v, loops, height);
                    x += loops;
                }
            }

            if (width > 3)
            {
                //End cap of image rect
                blit(graphics, texture, x, y, u + width - 3, v, 3, height);
            }
        }
    }

    /** 1.12 {@code drawTexturedModalRect(x, y, u, v, w, h)} on the 256x256 GUI sheet */
    protected void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, GUI_TEXTURE_SIZE, GUI_TEXTURE_SIZE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // input + tick
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** 1.12 {@code updateScreen()} */
    @Override
    protected void containerTick()
    {
        super.containerTick();
        components.forEach(IGuiComponent::onUpdate);
    }

    /**
     * 1.12 {@code keyTyped}: the insert key toggled the slot id debug rendering, everything else went to the
     * components (the text fields of 1.12 were not real widgets). 26.3 widgets receive their input from the
     * screen, so only the components that are not widgets are asked here.
     */
    @Override
    public boolean keyPressed(KeyEvent key)
    {
        //Key for debug render
        if (key.key() == InputConstants.KEY_INSERT)
        {
            renderSlotDebugIDs = !renderSlotDebugIDs;
            return true;
        }

        for (IGuiComponent component : components)
        {
            if (!(component instanceof AbstractWidget) && component.onKeyTyped(key))
            {
                return true;
            }
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean charTyped(CharacterEvent character)
    {
        for (IGuiComponent component : components)
        {
            if (!(component instanceof AbstractWidget) && component.onCharTyped(character))
            {
                return true;
            }
        }
        return super.charTyped(character);
    }

    /**
     * 1.12 {@code mouseClicked}: the widgets are clicked by vanilla, the rest of the components are notified
     * when the container itself did not use the click.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (super.mouseClicked(event, doubleClick))
        {
            return true;
        }
        components.forEach(component -> {
            if (!(component instanceof AbstractWidget))
            {
                component.onMouseClick(event, doubleClick);
            }
        });
        return false;
    }
}
