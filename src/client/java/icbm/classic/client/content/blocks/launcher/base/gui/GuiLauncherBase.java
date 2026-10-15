package icbm.classic.client.content.blocks.launcher.base.gui;

import icbm.classic.ICBMConstants;
import icbm.classic.client.prefab.gui.GuiContainerBase;
import icbm.classic.client.prefab.gui.TextInput;
import icbm.classic.client.prefab.gui.button.FaceRotationButton;
import icbm.classic.client.prefab.gui.components.SlotEnergyBar;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.content.blocks.launcher.base.gui.ContainerLaunchBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen for launcher base settings and inventory. */
public class GuiLauncherBase extends GuiContainerBase<ContainerLaunchBase> implements MenuAccess<ContainerLaunchBase>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/gui/gui_silo_base.png");

    public GuiLauncherBase(ContainerLaunchBase menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, 176, 166);
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
        final TileLauncherBase tile = menu.getTile();
        addComponent(TextInput.intField(font, 17, 17, 30, 12, tile::getLockHeight, tile::setLockHeight,
                value -> TileLauncherBase.PACKET_LOCK_HEIGHT.sendToServer(tile)));
        addComponent(TextInput.intField(font, 82, 17, 30, 12, tile::getGroupId, tile::setGroupId,
                value -> TileLauncherBase.PACKET_GROUP_ID.sendToServer(tile)));
        addComponent(TextInput.intField(font, 127, 17, 30, 12, tile::getGroupIndex, tile::setGroupIndex,
                value -> TileLauncherBase.PACKET_GROUP_INDEX.sendToServer(tile)));
        addComponent(TextInput.intField(font, 17, 33, 30, 12, tile::getFiringDelay, tile::setFiringDelay,
                value -> TileLauncherBase.PACKET_FIRING_DELAY.sendToServer(tile)));
        addComponent(new FaceRotationButton(leftPos + 157, topPos + 3, tile::getLaunchDirection,
                tile::getSeatSide, tile::setSeatSide, () -> TileLauncherBase.PACKET_SEAT_ROTATION.sendToServer(tile)));
        addComponent(new SlotEnergyBar(141, 66, () -> (int) tile.energyStorage.getStored(),
                () -> (int) tile.energyStorage.getCapacity()).withActionCost(tile::getFiringCost));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("icbmclassic.gui.launcher_base"), 30, 6, 0xFF555555);
    }
}
