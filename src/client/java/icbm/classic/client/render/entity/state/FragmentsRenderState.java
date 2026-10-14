package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 渲染状态: 破片实体 ({@code icbmclassic:fragment})。
 * <p>
 * 1.12 渲染器直接在 {@code doRender} 里读实体字段, 26.3 分成 extract/submit 两步, 这里保存渲染所需的
 * 实体快照。字段名沿用 1.12 语义 (见各字段注释)。
 */
public class FragmentsRenderState extends EntityRenderState
{
    /** 1.12 {@code EntityFragments#isAnvil}: 铁砧模式, 渲染铁砧方块而不是破片模型 */
    public boolean anvil;

    /**
     * 水平朝向的插值。
     * <p>
     * 1.12 {@code entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks}
     */
    public float yRot;

    /**
     * 俯仰角的插值。
     * <p>
     * 1.12 {@code entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks}
     */
    public float xRot;

    /**
     * 1.12 {@code EntityFragments#arrowShake} 的原值。
     * <p>
     * 该字段在 1.12 就不是同步字段, 客户端恒为 0, 见渲染器里的差异说明。
     */
    public float arrowShake;

    /**
     * 抖动破片用的 Z 轴旋转角 (角度)。
     * <p>
     * 1.12 在渲染时算 {@code f9 = (float) entity.arrowShake - partialTicks}, 当 {@code f9 > 0} 时
     * {@code rotate(-sin(f9 * 3.0F) * f9, 0, 0, 1)}; 26.3 的 submit 拿不到 partialTicks, 所以在 extract
     * 阶段算好这里保存 ({@code f9 <= 0} 时为 0)。
     */
    public float arrowShakeAngle;

    /**
     * 铁砧模式的方块数据。
     * <p>
     * 1.12 用一个方块状态 + 逐帧随机朝向/损伤直接渲染, 26.3 的移动方块提交需要一个
     * {@link MovingBlockRenderState} (位置/生物群系/光照引擎), 由渲染器在 extract 阶段填好。
     */
    public final MovingBlockRenderState anvilBlock = new MovingBlockRenderState();
}
