package icbm.classic.client.content.blocks.launcher.cruise.gui;

import icbm.classic.ICBMConstants;
import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.TextInput;
import icbm.classic.client.prefab.gui.button.DisableButton;
import icbm.classic.client.prefab.gui.components.SlotEnergyBar;
import icbm.classic.client.prefab.gui.tooltip.TooltipTranslations;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import icbm.classic.content.blocks.launcher.cruise.gui.ContainerCruiseLauncher;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client cruise launcher target, radio and firing screen. */
public class GuiCruiseLauncher extends GuiContainerBase<ContainerCruiseLauncher> implements MenuAccess<ContainerCruiseLauncher>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/gui/gui_cruise_launcher.png");
    private LaunchButton launchButton;

    public GuiCruiseLauncher(ContainerCruiseLauncher menu, Inventory playerInventory, Component title)
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
        final TileCruiseLauncher tile = menu.getTile();
        addComponent(TextInput.vec3dField(font, 18, 17, 100, 12, tile::getTarget, tile::setTarget,
                value -> TileCruiseLauncher.PACKET_TARGET.sendToServer(tile)));
        addComponent(TextInput.textField(font, 135, 17, 34, 12, tile.radio::getChannel, tile.radio::setChannel,
                value -> TileCruiseLauncher.PACKET_RADIO_HZ.sendToServer(tile)));
        launchButton = new LaunchButton(leftPos + 24, topPos + 38,
                Component.translatable("icbmclassic.gui.cruise_launcher.launch"),
                () -> TileCruiseLauncher.PACKET_LAUNCH.sendToServer(tile),
                () -> !tile.launcher.preCheckLaunch(new icbm.classic.content.missile.logic.targeting.BasicTargetData(tile.getTarget()), null).isBlocking());
        addRenderableWidget(launchButton.widget());
        addComponent(new SlotEnergyBar(141, 66, () -> (int) tile.energyStorage.getStored(),
                () -> (int) tile.energyStorage.getCapacity()).withActionCost(tile::getFiringCost));
        addComponent(new TooltipTranslations(119, 16, 14, 14, LauncherLangs.TRANSLATION_TOOLTIP_RADIO).withDelay(1));
        addComponent(new DisableButton(leftPos + 119, topPos + 16, tile.radio::isDisabled)
                .setAction(() -> TileCruiseLauncher.PACKET_RADIO_DISABLE.sendToServer(tile)));
        addComponent(new TooltipTranslations(2, 16, 14, 14, LauncherLangs.TRANSLATION_TOOLTIP_TARGET).withDelay(1));
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        if (launchButton != null) launchButton.update();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("icbmclassic.gui.cruise_launcher"), 52, 6, 0xFF555555);
        graphics.text(font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 4, 0xFF555555);
    }
}
