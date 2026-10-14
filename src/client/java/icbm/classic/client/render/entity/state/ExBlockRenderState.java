package icbm.classic.client.render.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * 渲染状态: 已点燃的爆炸物方块 ({@code icbmclassic:primed_explosive})。
 * <p>
 * 对应原版 {@code TntRenderState} (TNT 实体与点燃的爆炸物方块渲染方式一致)。
 */
public class ExBlockRenderState extends EntityRenderState
{
    /** 剩余引信 (含 partialTicks), 1.12 {@code EntityExplosive#fuse} */
    public float fuseRemainingInTicks;

    /** 待渲染的方块模型, 由 {@code BlockModelResolver} 在 extract 阶段填好; 为空表示没有方块形态 */
    public final BlockModelRenderState blockState = new BlockModelRenderState();
}
