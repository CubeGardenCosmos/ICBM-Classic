package icbm.classic.client.prefab.gui;

import icbm.classic.client.prefab.gui.textbox.GuiTextFieldBase;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import icbm.classic.lib.colors.ColorHelper;
import icbm.classic.lib.transform.region.Rectangle;
import icbm.classic.prefab.gui.GuiFormatHelpers;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Text field of the machine GUIs that writes into the machine and reports the change to it, 1.12
 * {@code icbm.classic.prefab.gui.TextInput} ported onto the vanilla {@link net.minecraft.client.gui.components.EditBox}.
 * <p>
 * Kept from 1.12: the value watcher (the field follows the machine while it does not have the focus), the
 * parse on focus loss with error feedback shown as a tooltip, and the network callback that tells the host
 * about the new value.
 * <p>
 * Changed by 26.3:
 * <ul>
 *     <li>{@code getText}/{@code setText} are {@code getValue}/{@code setValue}, {@code drawTextBox} is
 *     {@code extractWidgetRenderState} and {@code drawHorizontalLine} is
 *     {@link GuiGraphicsExtractor#horizontalLine(int, int, int, int)},</li>
 *     <li>text input focus: the field is a vanilla widget, {@code EditBox#setFocused} tells the 26.3 SDL text
 *     input about the focus change ({@code Minecraft#onTextInputFocusChange}) and this override keeps calling
 *     it (porting-guide.md §7),</li>
 *     <li>1.12's focus handler was guarded by {@code isFocusedIn == this.isFocused()} which is always true
 *     right after the super call, so the handler never ran; the port compares against the state before the
 *     call, which is the behaviour the 1.12 code was written for (commit on blur),</li>
 *     <li>1.12 formatted vectors with the (unported) {@code LanguageUtility#posFormatted}, the port uses
 *     {@link #formatVector(Vec3)}.</li>
 * </ul>
 */
public class TextInput<Output> extends GuiTextFieldBase implements IToolTip, IGuiComponent
{
    private static final int ERROR_COLOR = ColorHelper.toARGB(255, 1, 1, 255);

    // Selection change handling
    @Setter
    private Consumer<Boolean> focusChangedCallback;

    // Output to text handling
    @Setter
    private Function<Output, String> parseOutput;

    // Error handling
    private Component errorFeedback = null;

    // Data watcher to know when we update our display text
    @Setter
    private Supplier<Output> sourceWatcher;
    @Setter
    private Consumer<Output> onSourceChange;
    private Output previousData;

    private String previousText;

    @Deprecated
    private final Rectangle boundBox;

    public TextInput(Font font, int x, int y, int width, int height)
    {
        super(font, x, y, width, height, Component.empty());
        boundBox = new Rectangle(x, y, x + width + 1, y + height + 1); //TODO replace with internal check using data stored
    }

    public static TextInput<Vec3> vec3dField(Font font, int x, int y, int width, int height,
                                             Supplier<Vec3> getter, Consumer<Vec3> setter, Consumer<Vec3> network)
    {
        final TextInput<Vec3> fieldTarget = new TextInput<Vec3>(font, x, y, width, height);
        fieldTarget.simpleHandler(getter, setter, GuiFormatHelpers::parseVec3d);
        fieldTarget.setParseOutput(TextInput::formatVector);
        fieldTarget.setOnSourceChange(network);
        return fieldTarget;
    }

    public static TextInput<Integer> intField(Font font, int x, int y, int width, int height,
                                              Supplier<Integer> getter, Consumer<Integer> setter, Consumer<Integer> network)
    {
        final TextInput<Integer> fieldTarget = new TextInput<Integer>(font, x, y, width, height);
        fieldTarget.simpleHandler(getter, setter, GuiFormatHelpers::parseInt);
        fieldTarget.setOnSourceChange(network);
        return fieldTarget;
    }

    public static TextInput<String> textField(Font font, int x, int y, int width, int height,
                                              Supplier<String> getter, Consumer<String> setter, Consumer<String> network)
    {
        final TextInput<String> fieldTarget = new TextInput<String>(font, x, y, width, height);
        fieldTarget.stringHandler(getter, setter);
        fieldTarget.setOnSourceChange(network);
        return fieldTarget;
    }

    /** 1.12 {@code LanguageUtility#posFormatted} (that class is not ported, S2.04 dropped its users) */
    public static String formatVector(Vec3 vector)
    {
        if (vector == null)
        {
            return "";
        }
        return String.format("%.2f, %.2f, %.2f", vector.x, vector.y, vector.z);
    }

    @Override
    public void onUpdate()
    {
        if (!isFocused() && sourceWatcher != null)
        {
            detectForChange();

            // Handle string changes, but source didn't change... reset to source
            //  this likely happens when user inputs data but the source didn't take it
            //  examples of this could be numeric values greater than allowed
            if (!Objects.equals(previousText, getValue()))
            {
                setTextFromWatcher();
            }
        }
    }

    protected boolean detectForChange()
    {
        final Output output = sourceWatcher.get();
        if (!Objects.equals(output, previousData))
        {
            previousData = output;
            return true;
        }
        return false;
    }

    protected void setTextFromWatcher()
    {
        if (parseOutput != null)
        {
            previousText = parseOutput.apply(previousData);
        }
        else
        {
            previousText = Optional.ofNullable(previousData).map(Object::toString).orElse("");
        }
        setValue(previousText);
    }

    @Override
    public void setFocused(boolean focused)
    {
        final boolean wasFocused = this.isFocused();
        //26.3 SDL text input: EditBox#setFocused hands the focus change to Minecraft#onTextInputFocusChange,
        //keeping that call is mandatory for a text field to receive characters (porting-guide.md §7)
        super.setFocused(focused);

        if (focused == wasFocused)
        {
            return;
        }

        // Reset error feedback when we de-select
        if (!focused)
        {
            errorFeedback = null;
        }

        // Handle focus change
        //  often used for setting value from user input into host
        //  can set error feedback
        if (focusChangedCallback != null)
        {
            focusChangedCallback.accept(focused);
        }

        // Handle source change caused by user
        if (!focused && detectForChange() && onSourceChange != null)
        {
            // Notify systems of the change, this often triggers network calls
            onSourceChange.accept(previousData);
        }
    }

    public TextInput<Output> simpleHandler(Supplier<Output> getter, Consumer<Output> setter, BiFunction<String, Consumer<Output>, String> validator)
    {
        sourceWatcher = getter;
        focusChangedCallback = (state) -> {
            if (!state)
            {
                // Parse input from user and store into tile client side
                final String error = validator.apply(getValue(), setter);
                if (error != null)
                {
                    errorFeedback = Component.translatable(error);
                }
            }
        };
        return this;
    }

    @SuppressWarnings("unchecked")
    public TextInput<String> stringHandler(Supplier<String> getter, Consumer<String> setter)
    {
        sourceWatcher = (Supplier<Output>) getter;
        focusChangedCallback = (state) -> {
            if (!state)
            {
                setter.accept(getValue());
            }
        };
        return (TextInput<String>) this;
    }

    /** 1.12 {@code drawTextBox()} */
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);
        if (this.isVisible() && this.isBordered() && isErrored())
        {
            //1.12: drawHorizontalLine(x, x + width, y + height, ERROR_COLOR)
            graphics.horizontalLine(this.getX(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), ERROR_COLOR);
        }
    }

    @Override
    public boolean isWithin(int cursorX, int cursorY)
    {
        return boundBox.isWithin(cursorX - container.getGuiLeft(), cursorY - container.getGuiTop());
    }

    @Override
    public Component getTooltip()
    {
        return getErrorFeedback();
    }

    public Component getErrorFeedback()
    {
        return errorFeedback;
    }

    public void setError(Component error)
    {
        this.errorFeedback = error;
    }

    public boolean isErrored()
    {
        return errorFeedback != null;
    }
}
