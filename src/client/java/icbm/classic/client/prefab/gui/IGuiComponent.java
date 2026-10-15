package icbm.classic.client.prefab.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Component of an ICBM container screen, 1.12 {@code icbm.classic.prefab.gui.IGuiComponent} ported onto the
 * 26.3 GUI API.
 * <p>
 * The 1.12 callbacks took raw primitives and drew through {@code Gui}; 26.3 draws through
 * {@link GuiGraphicsExtractor} (no OpenGL, porting-guide.md §7) and hands input over as event records:
 * {@code onKeyTyped(char, int)} became {@link #onKeyTyped(KeyEvent)} plus {@link #onCharTyped(CharacterEvent)}
 * and {@code onMouseClick(int, int, int)} became {@link #onMouseClick(MouseButtonEvent, boolean)}.
 * <p>
 * Screen coordinates: the background layer and the free {@link #draw} are in screen space (like 1.12's
 * {@code drawGuiContainerBackgroundLayer} and {@code drawScreen}), the foreground layer is translated to the
 * top left corner of the GUI (like 1.12's {@code drawGuiContainerForegroundLayer}), the mouse position stays
 * absolute in both cases.
 */
public interface IGuiComponent
{
    /**
     * Callback for when this component is added to the container UI
     *
     * @param container added to
     */
    default void onAddedToHost(GuiContainerBase<?> container)
    {

    }

    /**
     * Called each UI tick
     */
    default void onUpdate()
    {

    }

    /**
     * Called by {@link GuiContainerBase#extractRenderState(GuiGraphicsExtractor, int, int, float)}
     *
     * @param graphics     extractor to draw with
     * @param mouseX       position
     * @param mouseY       position
     * @param partialTicks delta between frame renders
     */
    default void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {

    }

    /**
     * Draws in the container's foreground layer
     *
     * @param graphics extractor to draw with, translated to the GUI origin
     * @param mouseX   position
     * @param mouseY   position
     */
    default void drawForegroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {

    }

    /**
     * Draws in the container's background layer
     *
     * @param graphics     extractor to draw with
     * @param mouseX       position
     * @param mouseY       position
     * @param partialTicks delta between frame renders
     */
    default void drawBackgroundLayer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {

    }


    /**
     * Called when a key is pressed, only for components that are not {@code AbstractWidget}s (real widgets
     * receive their input from the screen itself)
     *
     * @param key event with the key code and modifiers
     * @return true if consumed to prevent other interaction from firing
     */
    default boolean onKeyTyped(KeyEvent key)
    {
        return false;
    }

    /**
     * Called when a character is typed, the 26.3 split of 1.12's {@code onKeyTyped(char, int)}
     *
     * @param character event with the typed code point
     * @return true if consumed to prevent other interaction from firing
     */
    default boolean onCharTyped(CharacterEvent character)
    {
        return false;
    }

    /**
     * Called when the mouse is clicked, only for components that are not {@code AbstractWidget}s
     *
     * @param event        mouse event with position and button
     * @param doubleClick  true when the click is part of a double click
     */
    default void onMouseClick(MouseButtonEvent event, boolean doubleClick)
    {

    }
}
