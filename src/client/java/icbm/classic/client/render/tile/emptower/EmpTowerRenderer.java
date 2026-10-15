package icbm.classic.client.render.tile.emptower;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.render.tile.EmpTowerRenderState;
import icbm.classic.content.blocks.emptower.BlockEmpTower;
import icbm.classic.content.blocks.emptower.PropertyTowerStates;
import icbm.classic.content.blocks.emptower.TileEMPTower;
import icbm.classic.content.blocks.emptower.TileEmpTowerFake;
import icbm.classic.content.reg.ModBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 26.3 block entity renderer port of legacy {@code TESREmpTower}, including its fake multi-block parts. */
public final class EmpTowerRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, EmpTowerRenderState>
{
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver blockModelResolver;

    public EmpTowerRenderer(BlockEntityRendererProvider.Context context)
    {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public EmpTowerRenderState createRenderState()
    {
        return new EmpTowerRenderState();
    }

    @Override
    public void extractRenderState(T entity, EmpTowerRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress)
    {
        BlockEntityRenderState.extractBase(entity, state, breakProgress);
        if (entity.getLevel() == null) return;

        TileEMPTower tower = null;
        int height = 0;
        if (entity instanceof TileEMPTower base)
        {
            tower = base;
        }
        else if (entity instanceof TileEmpTowerFake fake)
        {
            tower = fake.getHost();
            if (tower != null) height = entity.getBlockPos().getY() - tower.getBlockPos().getY();
        }

        float rotation = 0.0F;
        if (tower != null)
        {
            rotation = tower.prevRotation + (tower.rotation - tower.prevRotation) * partialTick;
            state.hasPower = tower.getChargePercentage() > 0.2F;
            if ((height & 1) != 0) rotation += 45.0F;
        }
        state.rotation = rotation;

        final BlockState coil = ModBlocks.EMP_TOWER.defaultBlockState()
                .setValue(BlockEmpTower.TOWER_MODELS, PropertyTowerStates.EnumTowerTypes.COIL);
        final BlockState electric = ModBlocks.EMP_TOWER.defaultBlockState()
                .setValue(BlockEmpTower.TOWER_MODELS, PropertyTowerStates.EnumTowerTypes.ELECTRIC);
        blockModelResolver.update(state.coil, coil, DISPLAY_CONTEXT);
        blockModelResolver.update(state.electric, electric, DISPLAY_CONTEXT);
        state.hasElectricModel = true;
    }

    @Override
    public void submit(EmpTowerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera)
    {
        if (state.coil.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, state.rotation);
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        state.coil.submit(poseStack, collector, state.lightCoords, 0, 0);
        poseStack.popPose();

        if (state.hasPower && state.hasElectricModel && !state.electric.isEmpty())
        {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.rotateDegrees(Axis.YP, state.rotation);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            state.electric.submit(poseStack, collector, state.lightCoords, 0, 0);
            poseStack.popPose();
        }
    }
}
