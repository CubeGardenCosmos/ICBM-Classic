package icbm.classic.client.render.entity.item;

import com.mojang.blaze3d.vertex.PoseStack;
import icbm.classic.client.render.entity.RenderMissile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared extraction and submission for missile item models held by entities and machine block entities. */
public final class MissileItemRender
{
    private MissileItemRender() {}

    public static void extract(ItemModelResolver resolver, ItemStackRenderState state, ItemStack stack, Level level, int seed)
    {
        RenderItemImp.extractItemModel(resolver, state, stack, ItemDisplayContext.NONE, level, null, seed);
    }

    public static void submit(ItemStackRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                              int lightCoords, int outlineColor, float x, float y, float z, float yaw, float pitch)
    {
        if (state.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // RenderMissile#translate and #rotate, kept shared so machine-held missiles match entity missiles.
        poseStack.translate(0.0F, 0.2F, 0.0F);
        RenderMissile.applyMissileRotation(poseStack, yaw - 180.0F, pitch - 90.0F, 0);
        RenderItemImp.submitItemModel(state, poseStack, collector, lightCoords, outlineColor);
        poseStack.popPose();
    }
}
