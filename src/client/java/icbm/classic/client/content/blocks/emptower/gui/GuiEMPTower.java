package icbm.classic.client.content.blocks.emptower.gui;
import icbm.classic.ICBMConstants;

import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.TextInput;
import icbm.classic.client.prefab.gui.button.DisableButton;
import icbm.classic.client.prefab.gui.components.SlotEnergyBar;
import icbm.classic.client.prefab.gui.tooltip.TooltipTranslations;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.emptower.gui.ContainerEMPTower;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GuiEMPTower extends GuiContainerBase<ContainerEMPTower> implements MenuAccess<ContainerEMPTower>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/gui/gui_emp_tower.png");

    public GuiEMPTower(ContainerEMPTower menu, Inventory playerInventory, Component title)
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
        final TileEMPTower tile = menu.getTile();
        addComponent(TextInput.intField(font, 18, 17, 40, 12, tile::getRange, tile::setRange,
                value -> TileEMPTower.PACKET_RADIUS.sendToServer(tile)));
        addComponent(TextInput.textField(font, 135, 17, 34, 12, tile.getRadio()::getChannel, tile.getRadio()::setChannel,
                value -> TileEMPTower.PACKET_RADIO_HZ.sendToServer(tile)));
        addComponent(new DisableButton(leftPos + 119, topPos + 16, tile.getRadio()::isDisabled)
                .setAction(() -> TileEMPTower.PACKET_RADIO_DISABLE.sendToServer(tile)));
        addComponent(new SlotEnergyBar(141, 66, () -> (int) tile.energyStorage.getStored(),
                () -> (int) tile.energyStorage.getCapacity()).withTickingCost(tile::getTickingCost).withActionCost(tile::getFiringCost));
        addComponent(new TooltipTranslations(119, 16, 14, 14,
                Component.translatable("icbmclassic.gui.tooltip.radio.channel")));
        addComponent(new TooltipTranslations(2, 16, 14, 14,
                Component.translatable("icbmclassic.gui.emp_tower.range")));
        addRenderableWidget(Button.builder(Component.translatable("icbmclassic.gui.emp_tower.fire"),
                        button -> TileEMPTower.PACKET_FIRE.sendToServer(tile))
                .bounds(leftPos + 24, topPos + 38, 48, 20).build());
    }

    @Override
    protected void extractLabels(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("icbmclassic.gui.emp_tower.name"), 52, 6, 0xFF555555);
        graphics.text(font, Component.translatable("icbmclassic.gui.emp_tower.max_range", menu.getTile().getMaxRadius()), 62, 19, 0xFF555555);
    }
}
