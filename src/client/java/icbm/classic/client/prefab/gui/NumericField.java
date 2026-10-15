package icbm.classic.client.prefab.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Text field that only accepts numbers, 1.12 {@code icbm.classic.prefab.gui.NumericField} ported onto the
 * vanilla {@link EditBox} (porting-guide.md §7).
 * <p>
 * 1.12 filtered inside {@code writeText} with two static char lists; 26.3 filters inside
 * {@link EditBox#insertText(String)} with the same character sets. The character to text methods keep their
 * 1.12 names and behaviour ({@code getText} → {@link #getValue()}, {@code setText} → {@link #setValue(String)})
 * including the "reset to 0 when the text is not a number" fallback.
 */
public class NumericField extends EditBox
{
    /** Characters accepted by a whole number field, 1.12 {@code charsA} */
    private static final String INT_CHARS = "0123456789+-";
    /** Characters accepted by a decimal field, 1.12 {@code charsB} */
    private static final String DECIMAL_CHARS = "0123456789+-.";

    private NumericType type = NumericType.INT;

    public NumericField(Font font, int x, int y, int width, int height, Component hint)
    {
        this(font, NumericType.DOUBLE, x, y, width, height, hint);
    }

    public NumericField(Font font, NumericType type, int x, int y, int width, int height, Component hint)
    {
        super(font, x, y, width, height, hint);
        this.type = type;
    }

    @Override
    public void insertText(String input)
    {
        //1.12 writeText(String) checked every char against the accepted set and dropped the whole input when
        //one of them was not allowed
        final String accepted = type == NumericType.INT ? INT_CHARS : DECIMAL_CHARS;
        for (int i = 0; i < input.length(); i++)
        {
            if (accepted.indexOf(input.charAt(i)) < 0)
            {
                return;
            }
        }
        super.insertText(input);
    }

    public int getTextAsInt()
    {
        final String text = this.getValue();
        if (text != null && !text.isEmpty())
        {
            try
            {
                return Integer.parseInt(text);
            }
            catch (NumberFormatException e)
            {
                this.setValue("" + 0);
            }
        }
        return 0;
    }

    public double getTextAsDouble()
    {
        final String text = this.getValue();
        if (text != null && !text.isEmpty())
        {
            try
            {
                return Double.parseDouble(text);
            }
            catch (NumberFormatException e)
            {
                this.setValue("" + 0);
            }
        }
        return 0;
    }

    public float getTextAsFloat()
    {
        final String text = this.getValue();
        if (text != null && !text.isEmpty())
        {
            try
            {
                return Float.parseFloat(text);
            }
            catch (NumberFormatException e)
            {
                this.setValue("" + 0);
            }
        }
        return 0;
    }

    public enum NumericType
    {
        INT,
        DOUBLE,
        FLOAT
    }
}
