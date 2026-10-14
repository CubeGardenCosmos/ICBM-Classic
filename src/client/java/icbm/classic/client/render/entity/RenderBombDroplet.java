package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.render.entity.item.RenderItemImp;
import icbm.classic.client.render.entity.state.IcbmItemRenderState;
import icbm.classic.content.cluster.bomblet.EntityBombDroplet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the bomblet as its item stack, the 26.3 port of 1.12
 * {@code content/cluster/bomblet/RenderBombDroplet}.
 * <p>
 * 1.12 built a fake {@code EntityItem} and let {@code RenderEntityItem2} draw it; 26.3 extracts the item
 * model directly. The 1.12 offsets are kept: 0.15 above the entity, {@code yaw - 180} / {@code pitch + 90}
 * and the model moved by {@code (0, 0.2, 0)} after the rotation.
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderBombDroplet extends RenderItemImp<EntityBombDroplet, IcbmItemRenderState>
{
    public RenderBombDroplet(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public IcbmItemRenderState createRenderState()
    {
        return new IcbmItemRenderState();
    }

    @Override
    protected ItemStack getRenderItem(EntityBombDroplet entity, int index)
    {
        return entity.toStack();
    }

    @Override
    protected float getYaw(EntityBombDroplet entity, float providedYaw)
    {
        return providedYaw - 180;
    }

    @Override
    protected float getPitch(EntityBombDroplet entity, float providedPitch)
    {
        return providedPitch + 90;
    }

    @Override
    protected void translate(PoseStack poseStack, IcbmItemRenderState state, int index)
    {
        //1.12: translate to center of entity collider
        poseStack.translate(0.0F, 0.15F, 0.0F);
    }

    @Override
    protected void rotate(PoseStack poseStack, IcbmItemRenderState state, CameraRenderState camera, int index)
    {
        poseStack.rotateDegrees(Axis.YP, state.entityYaw);
        poseStack.rotateDegrees(Axis.XP, state.entityPitch);

        //1.12: translate to rotation point of model, TODO extract from model file
        poseStack.translate(0.0F, 0.2F, 0.0F);
    }
}
