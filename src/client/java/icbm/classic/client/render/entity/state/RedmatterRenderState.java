package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Per frame values copied off {@link icbm.classic.content.blast.redmatter.EntityRedmatter} for its renderer.
 * <p>
 * 1.12 kept the animated values on the client side of the entity: {@code RedmatterClientLogic#visualSize}
 * (lerped by the renderer, see {@code EntityRedmatter#clientVisualSize}) and {@code ticksExisted}, which
 * drives every animation in the renderer.
 */
public class RedmatterRenderState extends EntityRenderState
{
    /** 1.12 {@code RedmatterClientLogic#visualSize} as it was when the renderer drew this frame. */
    public float visualSize;

    /** 1.12 {@code Entity#ticksExisted}, 26.3 reads {@code Entity#tickCount}. */
    public int ticksExisted;
}
