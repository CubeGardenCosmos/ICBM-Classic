package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icbm.classic.client.render.entity.state.LightBeamRenderState;
import icbm.classic.content.entity.EntityLightBeam;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * 1.12 {@code icbm.classic.client.render.entity.RenderLightBeam} 的 26.3 版本（S4.07）。
 *
 * 光束的几何完全照 1.12：轴对齐（不旋转）的两层方管，内层贴图 uv 随时间滚动、外层光晕 uv 固定。
 * 1.12 由固定的渲染状态开关（混合/深度写/剔除）控制这两层，26.3 改成两层各用一个
 * {@link RenderTypes#beaconBeam(Identifier, boolean)}（原版信标光柱用的 RenderType，负责混合、深度写、
 * 贴图采样方式），顶点通过 {@link SubmitNodeCollector#submitCustomGeometry} 提交。
 *
 * <p>移植要点：
 * <ul>
 *     <li>贴图用原版信标光柱贴图；26.3 的资源路径是 {@code textures/entity/beacon/beacon_beam.png}
 *     （1.12 是 {@code textures/entity/beacon_beam.png}，该路径在 26.3 不存在），见移植报告「差异」。</li>
 *     <li>1.12 在 doRender 里把 {@code clientBeamProgress} 往同步过来的 {@code getBeamProgress()} 上插值；
 *     26.3 放在 {@link #extractRenderState}（每帧、每个被渲染的实体各调用一次），插值公式照抄。</li>
 *     <li>1.12 的 {@code getRenderBoundingBox()} 返回无限高，实体不会被视锥剔除；26.3 用
 *     {@link #shouldRender} 恒真做等价映射。</li>
 * </ul>
 */
public class RenderLightBeam extends EntityRenderer<EntityLightBeam, LightBeamRenderState>
{
    /** 1.12 {@code RenderLightBeam.TEXTURE_BEACON_BEAM}，26.3 的信标光柱贴图路径 */
    public static final Identifier TEXTURE_BEACON_BEAM = Identifier.withDefaultNamespace("textures/entity/beacon/beacon_beam.png");

    /** 1.12 顶点色写的 alpha 是 0.125F，26.3 顶点色是打包好的 ARGB（0.125 * 255 ≈ 32） */
    private static final int BEAM_ALPHA = 32;

    /** 光柱自发光：1.12 顶点没有光照信息，26.3 用全亮光照值 0xF000F0 */
    private static final int FULL_BRIGHT = 15728880;

    /** 1.12 {@code renderBeamSegment} 的 textureScale 参数，唯一调用处传的是 1 */
    private static final double TEXTURE_SCALE = 1.0D;

    /** 1.12 {@code renderBeamSegment(x, y - 5, z, ...)}：光柱从实体下方 5 格开始 */
    private static final float BEAM_Y_OFFSET = -5.0F;

    public RenderLightBeam(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public LightBeamRenderState createRenderState()
    {
        return new LightBeamRenderState();
    }

    @Override
    public void extractRenderState(EntityLightBeam entity, LightBeamRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);

        // 1.12 doRender 先把客户端进度往同步过来的目标进度上拉（每帧最多 beamGrowthRate * partialTicks）
        final float beamGrowthRate = entity.beamGrowthRate * partialTicks;
        if (entity.clientBeamProgress < entity.getBeamProgress())
        {
            entity.clientBeamProgress = Math.min(entity.getBeamProgress(), entity.clientBeamProgress + beamGrowthRate);
        }
        //Decrease size slowly
        else if (entity.clientBeamProgress > entity.getBeamProgress())
        {
            entity.clientBeamProgress = Math.max(entity.getBeamProgress(), entity.clientBeamProgress - beamGrowthRate);
        }

        state.red = entity.red;
        state.green = entity.green;
        state.blue = entity.blue;
        state.beamSize = entity.beamSize;
        state.beamGlowSize = entity.beamGlowSize;
        state.beamProgress = entity.clientBeamProgress;
        // 1.12: height = 255 - (int) beamEntity.posY；state.y 是同一世界坐标（基类已按 partialTicks 插值）
        state.height = 255 - (int) state.y;
    }

    /**
     * 1.12 的 {@code getRenderBoundingBox()} 是无限高的盒子，光柱永远不会被剔除；26.3 的默认实现用实体
     * 包围盒做视锥剔除（光柱实体只有 1×1 的盒子），这里覆写成恒真来保留 1.12 的行为。
     */
    @Override
    public boolean shouldRender(EntityLightBeam entity, Frustum culler, double camX, double camY, double camZ, float partialTicks)
    {
        return true;
    }

    @Override
    public void submit(LightBeamRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        //TODO instead of -5 raytrace to ground
        poseStack.pushPose();
        poseStack.translate(0.0F, BEAM_Y_OFFSET, 0.0F);

        final float beamRadius = Math.max(0.0F, state.beamProgress * state.beamSize);
        final float beamGlowRadius = Math.max(0.0F, state.beamProgress * state.beamGlowSize);
        //26.3 的信标光柱着色器把顶点颜色整体乘进采样色（包括 alpha），而内层用的是不混合的 opaque 管线，
        //所以内层必须给全 alpha，否则整根柱子会被压到 1/8 亮度；1.12 的 0.125 alpha 只加在外层（见差异说明）
        final int coreColor = ARGB.color(255,
            ARGB.as8BitChannel(state.red), ARGB.as8BitChannel(state.green), ARGB.as8BitChannel(state.blue));
        final int glowColor = ARGB.color(BEAM_ALPHA,
            ARGB.as8BitChannel(state.red), ARGB.as8BitChannel(state.green), ARGB.as8BitChannel(state.blue));

        // 内层：uv 随时间滚动（1.12 的 uv_animation），不混合、深度写开
        final double time = state.ageInTicks;
        final double uvAnimation = Mth.frac(time * 0.2D - Mth.floor(time * 0.1D));
        final double innerUvBottom = -1.0D + uvAnimation;
        final double innerUvTop = state.height * TEXTURE_SCALE + innerUvBottom;
        submitBeamTube(poseStack, submitNodeCollector, RenderTypes.beaconBeam(TEXTURE_BEACON_BEAM, false),
            coreColor, state.height, beamRadius, innerUvBottom, innerUvTop);

        // 外层：uv 不动画（1.12 外层固定从 -1 开始），alpha 混合、深度写关
        final double outerUvTop = state.height * TEXTURE_SCALE - 1.0D;
        submitBeamTube(poseStack, submitNodeCollector, RenderTypes.beaconBeam(TEXTURE_BEACON_BEAM, true),
            glowColor, state.height, beamGlowRadius, -1.0D, outerUvTop);

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    /**
     * 提交一层光柱：1.12 {@code renderBeamTube} 的四个侧面，角点相对实体中心偏移 0.5±半径（轴对齐）。
     *
     * @param radius 该层的半径（1.12 的 beamRadius 或 beamGlowRadius）
     */
    private static void submitBeamTube(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, RenderType renderType,
                                       int color, int height, float radius, double uvBottom, double uvTop)
    {
        final float min = 0.5F - radius;
        final float max = 0.5F + radius;
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
        {
            // 北面
            addQuad(pose, buffer, color, height, min, min, max, min, uvBottom, uvTop);
            // 南面
            addQuad(pose, buffer, color, height, max, max, min, max, uvBottom, uvTop);
            // 东面
            addQuad(pose, buffer, color, height, max, min, max, max, uvBottom, uvTop);
            // 西面
            addQuad(pose, buffer, color, height, min, max, min, min, uvBottom, uvTop);
        });
    }

    /** 1.12 每个侧面的顶点顺序：第一个交角在顶部（u = 1）→ 底部（u = 1）→ 第二个交角底部（u = 0）→ 顶部（u = 0）。 */
    private static void addQuad(PoseStack.Pose pose, VertexConsumer buffer, int color, float height,
                                float x0, float z0, float x1, float z1, double uvBottom, double uvTop)
    {
        addVertex(pose, buffer, color, x0, height, z0, 1.0F, uvTop);
        addVertex(pose, buffer, color, x0, 0.0F, z0, 1.0F, uvBottom);
        addVertex(pose, buffer, color, x1, 0.0F, z1, 0.0F, uvBottom);
        addVertex(pose, buffer, color, x1, height, z1, 0.0F, uvTop);
    }

    private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, int color, float x, float y, float z, float u, double v)
    {
        buffer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(u, (float) v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
