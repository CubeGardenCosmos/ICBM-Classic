package icbm.classic.client.prefab.gui.button;

import icbm.classic.client.prefab.gui.IGuiComponent;
import icbm.classic.client.prefab.gui.tooltip.IToolTip;
import icbm.classic.lib.data.FaceRotations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Button that rotates a direction around the face of the block, 1.12
 * {@code icbm.classic.prefab.gui.button.FaceRotationButton} ported onto {@link GuiButtonBase}.
 * <p>
 * Kept from 1.12: alt or shift is required for the click, shift rotates counter clockwise, and the label of the
 * button is the first letter of the current rotation. The label is not translated, it is the single letter of
 * {@link Direction#name()} exactly like the 1.12 {@code displayString} (S8 audit item).
 */
public class FaceRotationButton extends GuiButtonBase<FaceRotationButton> implements IGuiComponent, IToolTip
{
    /** 1.12 key {@code gui.icbmclassic:button.face.tooltip}, renamed by S2.10 */
    public static final Component TOOLTIP = Component.translatable("icbmclassic.gui.button.face.tooltip");

    // Icon this designed to cover is 13x13
    private static final int WIDTH = 15;
    private static final int HEIGHT = 15;

    private final Supplier<Direction> currentRotationGetter;
    private final Supplier<Direction> blockFaceGetter;
    private Direction prevSide;

    public FaceRotationButton(int x, int y, Supplier<Direction> blockFaceGetter, Supplier<Direction> currentRotationGetter, Consumer<Direction> setter, Runnable networkCall)
    {
        super(x, y, WIDTH, HEIGHT, Component.literal("-"));
        this.blockFaceGetter = blockFaceGetter;
        this.currentRotationGetter = currentRotationGetter;
        this.setAction(() -> {
            setter.accept(rotate());
            networkCall.run();
        });
        this.setTooltip(() -> TOOLTIP);
    }

    protected Direction rotate()
    {
        final Direction currentRotation = currentRotationGetter.get();
        final Direction blockFace = blockFaceGetter.get();
        //1.12: GuiContainer.isShiftKeyDown()
        if (Minecraft.getInstance().hasShiftDown())
        {
            return FaceRotations.rotateLeft(blockFace, currentRotation);
        }
        return FaceRotations.rotateRight(blockFace, currentRotation);
    }

    @Override
    public void onUpdate()
    {
        final Direction side = currentRotationGetter.get();
        if (prevSide != side)
        {
            prevSide = side;
            //1.12: displayString = side.name().toUpperCase().substring(0, 1)
            setMessage(Component.literal(side.name().toUpperCase(Locale.ROOT).substring(0, 1)));
        }
    }

    /** 1.12 {@code mousePressed} required alt or shift */
    @Override
    protected boolean isValidClickButton(MouseButtonInfo button)
    {
        //1.12: GuiContainer.isAltKeyDown() || GuiContainer.isShiftKeyDown()
        return super.isValidClickButton(button)
            && (Minecraft.getInstance().hasAltDown() || Minecraft.getInstance().hasShiftDown());
    }
}
