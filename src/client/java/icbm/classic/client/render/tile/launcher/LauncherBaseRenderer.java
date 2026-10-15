package icbm.classic.client.render.tile.launcher;

import com.mojang.blaze3d.vertex.PoseStack;
import icbm.classic.client.render.entity.item.MissileItemRender;
import icbm.classic.client.render.tile.LauncherBaseRenderState;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** 26.3 block entity renderer port of legacy {@code TESRLauncherBase}. */
public final class LauncherBaseRenderer implements BlockEntityRenderer<TileLauncherBase, LauncherBaseRenderState>
{
    private static final float MISSILE_OFFSET = 2.0F;
    private final ItemModelResolver itemModelResolver;

    public LauncherBaseRenderer(BlockEntityRendererProvider.Context context)
    {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public LauncherBaseRenderState createRenderState()
    {
        return new LauncherBaseRenderState();
    }

    @Override
    public void extractRenderState(TileLauncherBase launcher, LauncherBaseRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderState.extractBase(launcher, state, breakProgress);
        if (launcher.getLevel() == null) return;

        final Direction direction = launcher.getLaunchDirection();
        state.offsetX = 0.5F + direction.getStepX() * MISSILE_OFFSET;
        state.offsetY = 0.5F + direction.getStepY() * MISSILE_OFFSET;
        state.offsetZ = 0.5F + direction.getStepZ() * MISSILE_OFFSET;
        state.yaw = launcher.getMissileYaw(true);
        state.pitch = launcher.getMissilePitch(true);
        MissileItemRender.extract(itemModelResolver, state.missile, launcher.getMissileStack(), launcher.getLevel(), state.blockPos.hashCode());
    }

    @Override
    public void submit(LauncherBaseRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        MissileItemRender.submit(state.missile, poseStack, collector, state.lightCoords, 0,
                state.offsetX, state.offsetY, state.offsetZ, state.yaw, state.pitch);
    }
}
