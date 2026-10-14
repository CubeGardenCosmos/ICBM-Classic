package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.ChickenRenderState;

/**
 * Chicken render state with the ICBM easter egg flag, used by the space chicken helmet layer.
 * <p>
 * 1.12 read the "in space" flag straight off the entity in the render layer. 26.3 render layers only receive
 * the render state, so the flag has to be copied by a custom chicken renderer (see
 * {@code IcbmChickenRenderer}); the attachment itself is synced to the client (S4.07,
 * {@code ModAttachments#SPACE_CHICKEN}).
 */
public class IcbmChickenRenderState extends ChickenRenderState
{
    /** True while the chicken carries {@code icbmclassic:space_chicken} (1.12 {@code CapSpaceChicken}). */
    public boolean spaceChicken;
}
