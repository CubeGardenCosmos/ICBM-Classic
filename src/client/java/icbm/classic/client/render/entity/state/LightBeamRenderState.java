package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * {@code RenderLightBeam} 的渲染状态（S4.07）。
 *
 * 1.12 的渲染器直接在 {@code doRender} 里读 {@code EntityLightBeam} 的公开渲染字段；26.3 拆成
 * extract/submit 两步后，这些值由渲染器在 {@code extractRenderState} 里拷进来，提交阶段只读本状态。
 * 世界坐标 {@link #y}、存活时间 {@link #ageInTicks} 等由 {@link EntityRenderState} 基类填好。
 */
public class LightBeamRenderState extends EntityRenderState
{
    /** 1.12 {@code EntityLightBeam#red/green/blue}：光柱颜色 */
    public float red = 1F;
    public float green = 0F;
    public float blue = 0F;

    /** 1.12 {@code EntityLightBeam#beamSize}：内层光柱的半径系数 */
    public float beamSize = 0.5F;

    /** 1.12 {@code EntityLightBeam#beamGlowSize}：外层光晕的半径系数 */
    public float beamGlowSize = 1F;

    /**
     * 客户端插值后的进度（1.12 {@code EntityLightBeam#clientBeamProgress}），提交时乘以
     * {@link #beamSize}/{@link #beamGlowSize} 得到实际渲染半径。
     */
    public float beamProgress = 0F;

    /** 1.12: {@code height = 255 - (int) beamEntity.posY}，光柱的渲染高度（格） */
    public int height = 255;
}
