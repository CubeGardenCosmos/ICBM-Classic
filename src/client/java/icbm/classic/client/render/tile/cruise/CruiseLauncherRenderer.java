package icbm.classic.client.render.tile.cruise;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.model.CruiseLauncherTopModel;
import icbm.classic.client.render.entity.item.MissileItemRender;
import icbm.classic.client.render.tile.CruiseLauncherRenderState;
import icbm.classic.content.blocks.launcher.cruise.TileCruiseLauncher;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;

/** 26.3 block entity renderer port of legacy {@code TESRCruiseLauncher}. */
public final class CruiseLauncherRenderer implements BlockEntityRenderer<TileCruiseLauncher, CruiseLauncherRenderState>
{
    private final ItemModelResolver itemModelResolver;
    private final CruiseLauncherTopModel model;

    public CruiseLauncherRenderer(BlockEntityRendererProvider.Context context)
    {
        this.itemModelResolver = context.itemModelResolver();
        this.model = new CruiseLauncherTopModel(context.bakeLayer(CruiseLauncherTopModel.LAYER));
    }

    @Override
    public CruiseLauncherRenderState createRenderState()
    {
        return new CruiseLauncherRenderState();
    }

    @Override
    public void extractRenderState(TileCruiseLauncher launcher, CruiseLauncherRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderState.extractBase(launcher, state, breakProgress);
        if (launcher.getLevel() == null) return;

        state.yaw = (float) launcher.getCurrentYaw();
        state.pitch = (float) launcher.getCurrentPitch();
        MissileItemRender.extract(itemModelResolver, state.missile, launcher.getMissileStack(), launcher.getLevel(), state.blockPos.hashCode());
    }

    @Override
    public void submit(CruiseLauncherRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        poseStack.pushPose();
        poseStack.translate(0.5F, 2.5F, 0.5F);
        poseStack.rotateDegrees(Axis.ZP, 180.0F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        model.setAim((float) -Math.toRadians(state.yaw), (float) -Math.toRadians(state.pitch));
        collector.submitModel(model, state, poseStack, CruiseLauncherTopModel.TEXTURE, state.lightCoords, 0, 0);
        poseStack.popPose();

        MissileItemRender.submit(state.missile, poseStack, collector, state.lightCoords, 0,
                0.5F, 2.0F, 0.5F, state.yaw, state.pitch - 90.0F);
    }
}
