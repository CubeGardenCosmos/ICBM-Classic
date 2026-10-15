package icbm.classic.client.prefab.gui.textbox;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.IGuiComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Base text field of the ICBM GUIs, 1.12 {@code icbm.classic.prefab.gui.textbox.GuiTextFieldBase} ported onto
 * the vanilla {@link EditBox} (porting-guide.md §7: use the vanilla field, it handles the 26.3 SDL text input
 * and its focus notification itself).
 * <p>
 * What is left of the 1.12 class: the host reference. The 1.12 overrides have 26.3 counterparts in the vanilla
 * widget: {@code drawTextBox} is {@code EditBox#extractWidgetRenderState}, {@code onKeyTyped} is the
 * {@code keyPressed}/{@code charTyped} dispatch of the screen and {@code onMouseClick} is
 * {@link EditBox#onClick}. The component id of the 1.12 constructor is gone with {@code GuiTextField}
 * (26.3 widgets are identified by their instance), the field text is {@code getValue}/{@code setValue}.
 */
public class GuiTextFieldBase extends EditBox implements IGuiComponent
{
    protected GuiContainerBase<?> container;

    public GuiTextFieldBase(Font font, int x, int y, int width, int height, Component hint)
    {
        super(font, x, y, width, height, hint);
    }

    @Override
    public void onAddedToHost(GuiContainerBase<?> container)
    {
        this.container = container;
    }
}
