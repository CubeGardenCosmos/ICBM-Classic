package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12 {@code icbm.classic.content.entity.flyingblock.RenderEntityBlock} 的 26.3 版本（S4.07）。
 *
 * 26.3 渲染拆成了两步：{@link #extractRenderState} 在游戏线程把实体数据拷进渲染状态，
 * {@link #submit} 只负责往 {@link SubmitNodeCollector} 里提交几何（真正的绘制是延迟的）。
 * 因此这里直接复用原版 {@link FallingBlockRenderState}，方块模型的提交完全照
 * {@code net.minecraft.client.renderer.entity.FallingBlockRenderer} 的写法。
 *
 * <p>移植要点：
 * <ul>
 *     <li>方块状态：1.12 的渲染器直接读实体上的 {@code getBlockData().getBlockState()}（服务器侧逻辑用的
 *     字段）；26.3 的渲染状态在客户端提取，只能读 {@link EntityFlyingBlock#getBlockState()}（S3.03 把捕获的
 *     方块状态同步进 {@code DATA_BLOCK_STATE}）。</li>
 *     <li>实体旋转：1.12 的渲染器根本不使用 {@code entityYaw}/{@code entityPitch}（只有实体自己在 tick 里改
 *     朝向，视觉上没有效果），26.3 同样不应用实体旋转，保持一致。</li>
 *     <li>1.12 的 {@code failedBlocks} 回退表（渲染抛异常时改用橡木树叶/石头）没有 26.3 的等价触发点：
 *     方块模型的解析发生在提交之后的 feature 阶段（{@code MovingBlockFeatureRenderer} 用
 *     {@code blockStateModelSet().get()} 取模型，缺失时给占位模型，不会抛异常），见移植报告「差异」。</li>
 * </ul>
 */
public class RenderEntityBlock extends EntityRenderer<EntityFlyingBlock, FallingBlockRenderState>
{
    public RenderEntityBlock(EntityRendererProvider.Context context)
    {
        super(context);
        // 1.12 RenderEntityBlock 构造器里的 shadowSize = 0.5F
        this.shadowRadius = 0.5F;
    }

    @Override
    public FallingBlockRenderState createRenderState()
    {
        return new FallingBlockRenderState();
    }

    @Override
    public void extractRenderState(EntityFlyingBlock entity, FallingBlockRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);

        // 与原版 FallingBlockRenderer 一致：取实体包围盒顶端所在的方块，供移动方块模型的
        // 生物群系着色与光照查询使用
        final BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());

        // 原版 FallingBlockEntity 有“开始下落的位置”（getStartPos）用作方块模型的随机种子；1.12 的
        // EntityFlyingBlock 没有这个概念（EntityProjectile#sourceOfProjectile 只存在于服务器侧且
        // FlyingBlock 生成路径不写它，客户端拿不到），这里退回实体当前所在方块
        state.movingBlockRenderState.randomSeedPos = entity.blockPosition();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = entity.getBlockState();
        if (entity.level() instanceof ClientLevel clientLevel)
        {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    @Override
    public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        // 1.12 把方块交给 BlockRendererDispatcher#renderBlockBrightness，只有 MODEL 形状的方块会画出东西，
        // 原版 FallingBlockRenderer 用同样的判断
        if (state.movingBlockRenderState.blockState.getRenderShape() == RenderShape.MODEL)
        {
            //TODO consider using item render instead?
            // 1.12 的变换是「先平移 (x, y + 0.5, z)、再绕 Y 转 -90°、再平移 (-0.5, -0.5, 0.5)」，
            // 换算到 26.3 的方块网格空间后只剩 (-0.5, 0, -0.5) 的平移（推导见 S4.07 移植报告），
            // 与原版 FallingBlockRenderer 提交移动方块模型时用的偏移相同
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);
            submitNodeCollector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
            poseStack.popPose();

            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }
}
