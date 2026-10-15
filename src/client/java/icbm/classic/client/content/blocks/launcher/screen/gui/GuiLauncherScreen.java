package icbm.classic.client.content.blocks.launcher.screen.gui;

import icbm.classic.ICBMConstants;
import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.TextInput;
import icbm.classic.client.prefab.gui.button.DisableButton;
import icbm.classic.client.prefab.gui.tooltip.TooltipTranslations;
import icbm.classic.client.gui.widget.EnergyBar;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import icbm.classic.content.blocks.launcher.screen.TileLauncherScreen;
import icbm.classic.content.blocks.launcher.screen.gui.ContainerLaunchScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen for target selection, radio settings and firing. */
public class GuiLauncherScreen extends GuiContainerBase<ContainerLaunchScreen> implements MenuAccess<ContainerLaunchScreen>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/gui/gui_silo_screen.png");

    public GuiLauncherScreen(ContainerLaunchScreen menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, 175, 166);
    }

    @Override
    public Identifier getBackground()
    {
        return TEXTURE;
    }

    @Override
    protected void init()
    {
        super.init();
        final TileLauncherScreen tile = menu.getTile();
        addComponent(TextInput.vec3dField(font, 18, 17, 100, 12, tile::getTarget, tile::setTarget,
                value -> TileLauncherScreen.PACKET_TARGET.sendToServer(tile)));
        addComponent(TextInput.textField(font, 135, 17, 34, 12, tile.radioCap::getChannel,
                tile.radioCap::setChannel, value -> TileLauncherScreen.PACKET_RADIO_HZ.sendToServer(tile)));
        addComponent(new DisableButton(leftPos + 119, topPos + 16, tile.radioCap::isDisabled)
                .setAction(() -> TileLauncherScreen.PACKET_RADIO_DISABLE.sendToServer(tile)));
        addComponent(new TooltipTranslations(119, 16, 14, 14, LauncherLangs.TRANSLATION_TOOLTIP_RADIO).withDelay(1));
        addComponent(new TooltipTranslations(2, 16, 14, 14, LauncherLangs.TRANSLATION_TOOLTIP_TARGET).withDelay(1));
        addRenderableWidget(Button.builder(Component.translatable("icbmclassic.gui.launcher_screen.launch"),
                button -> TileLauncherScreen.PACKET_LAUNCH.sendToServer(tile))
                .bounds(leftPos + 24, topPos + 38, 48, 20).build());
        addComponent(new EnergyBar(141, 66, menu::getEnergyStored, menu::getEnergyCapacity));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("icbmclassic.gui.launcher_screen"), 30, 6, 0xFF555555);
        graphics.text(font, Component.translatable("icbmclassic.gui.launcher_screen.inaccuracy",
                String.format(java.util.Locale.ROOT, "%.2f", menu.getTile().getLauncherInaccuracy())),
                60, 32, 0xFF555555);
        graphics.text(font, LauncherLangs.TRANSLATION_TOOLTIP_TARGET, 2, 16, 0xFF555555);
    }
}
