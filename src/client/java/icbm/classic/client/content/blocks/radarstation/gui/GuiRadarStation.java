package icbm.classic.client.content.blocks.radarstation.gui;

import icbm.classic.ICBMConstants;
import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.TextInput;
import icbm.classic.client.prefab.gui.button.DisableButton;
import icbm.classic.client.prefab.gui.button.RedstoneButton;
import icbm.classic.client.gui.widget.EnergyBar;
import icbm.classic.client.prefab.gui.tooltip.TooltipTranslations;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.content.blocks.radarstation.gui.ContainerRadarStation;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GuiRadarStation extends GuiContainerBase<ContainerRadarStation> implements MenuAccess<ContainerRadarStation>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/gui/gui_radar.png");

    public GuiRadarStation(ContainerRadarStation menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, 175, 184);
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
        final TileRadarStation tile = menu.getTile();
        addComponent(TextInput.textField(font, 135, 17, 34, 12, tile.getRadio()::getChannel, tile.getRadio()::setChannel,
                value -> TileRadarStation.PACKET_RADIO_HZ.sendToServer(tile)));
        addComponent(TextInput.intField(font, 18, 77, 29, 12, tile::getTriggerRange, tile::setTriggerRange,
                value -> TileRadarStation.PACKET_TRIGGER_RANGE.sendToServer(tile)));
        addComponent(TextInput.intField(font, 49, 77, 29, 12, tile::getDetectionRange, tile::setDetectionRange,
                value -> TileRadarStation.PACKET_DETECTION_RANGE.sendToServer(tile)));
        addComponent(new RedstoneButton(leftPos + 160, topPos + 3, tile::isOutputRedstone)
                .setAction(() -> TileRadarStation.PACKET_REDSTONE_OUTPUT.sendToServer(tile))
                .setTooltip(() -> Component.translatable(tile.isOutputRedstone()
                        ? TileRadarStation.TRANSLATION_TOOLTIP_REDSTONE_ON : TileRadarStation.TRANSLATION_TOOLTIP_REDSTONE_OFF)));
        addComponent(new DisableButton(leftPos + 119, topPos + 16, tile.getRadio()::isDisabled)
                .setAction(() -> TileRadarStation.PACKET_RADIO_DISABLE.sendToServer(tile)));
        addComponent(new EnergyBar(141, 66, menu::getEnergyStored, menu::getEnergyCapacity));
        addComponent(new RadarComponent(tile, 5, 18));
        addComponent(new TooltipTranslations(4, 76, 14, 14,
                Component.translatable(TileRadarStation.TRANSLATION_TOOLTIP_RANGE))
                .withShift(Component.translatable(TileRadarStation.TRANSLATION_TOOLTIP_RANGE_SHIFT)));
        addComponent(new TooltipTranslations(119, 16, 14, 14,
                Component.translatable("icbmclassic.gui.tooltip.radio.channel")));
    }

    @Override
    protected void extractLabels(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("icbmclassic.gui.radar.name"), 30, 6, 0xFF555555);
        graphics.text(font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 4, 0xFF555555);
    }
}
