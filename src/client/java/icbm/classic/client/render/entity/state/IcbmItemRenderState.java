package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * Render state of every entity the 1.12 renderers drew as an item stack ({@code RenderItemImp} and its
 * subclasses, 26.3 port S4.07).
 * <p>
 * 26.3 requires the renderer to copy everything it draws into a state object every frame
 * ({@code EntityRenderer#extractRenderState}), the item models cannot be read from the entity there.
 */
public class IcbmItemRenderState extends EntityRenderState
{
    /** Item of index 0, the entity itself (1.12 {@code getRenderItem(entity, 0)}). */
    public final ItemStackRenderState item = new ItemStackRenderState();

    /** Item of index 1, the payload of the missile (1.12 {@code getRenderItem(entity, 1)}). */
    public final ItemStackRenderState secondItem = new ItemStackRenderState();

    /** True when index 1 should be drawn, only the held item missile uses it. */
    public boolean hasSecondItem;

    /** Entity yaw after the renderer hook, 1.12 {@code getYaw} (already interpolated). */
    public float entityYaw;

    /** Entity pitch after the renderer hook, 1.12 {@code getPitch} (already interpolated). */
    public float entityPitch;

    /** Extra scale of index 0 (1.12 {@code scale}, used by the parachute). */
    public float scale = 1.0F;
}
