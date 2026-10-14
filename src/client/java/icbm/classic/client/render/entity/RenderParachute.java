package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.render.entity.item.RenderItemImp;
import icbm.classic.client.render.entity.state.IcbmItemRenderState;
import icbm.classic.content.cargo.parachute.EntityParachute;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the parachute (and its cargo) as an item stack, the 26.3 port of 1.12
 * {@code content/cargo/parachute/RenderParachute}.
 * <p>
 * The parachute is always billboarded and scaled by {@code EntityParachute#getRenderScale}.
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderParachute extends RenderItemImp<EntityParachute, IcbmItemRenderState>
{
    public RenderParachute(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public IcbmItemRenderState createRenderState()
    {
        return new IcbmItemRenderState();
    }

    @Override
    protected ItemStack getRenderItem(EntityParachute entity, int index)
    {
        return entity.getRenderItemStack();
    }

    @Override
    public void extractRenderState(EntityParachute entity, IcbmItemRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        //1.12 RenderParachute#scale: scale(e.getRenderScale(), e.getRenderScale(), e.getRenderScale())
        state.scale = entity.getRenderScale();
    }

    /** 1.12 always faced the camera, TODO add wiggle to simulate wind. */
    @Override
    protected void rotate(PoseStack poseStack, IcbmItemRenderState state, CameraRenderState camera, int index)
    {
        poseStack.rotateDegrees(Axis.YP, 180.0F - camera.yRot);
    }

    @Override
    protected void scale(PoseStack poseStack, IcbmItemRenderState state, int index)
    {
        poseStack.scale(state.scale, state.scale, state.scale);
    }
}
