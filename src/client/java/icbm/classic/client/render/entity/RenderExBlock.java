package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.ICBMClassicClient;
import icbm.classic.client.render.entity.state.ExBlockRenderState;
import icbm.classic.content.blocks.explosive.ExplosiveBlock;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.reg.ModBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 渲染已点燃的爆炸物方块。
 * <p>
 * 1.12 的 {@code RenderExBlock} 直接渲染 {@code BlockReg.blockExplosive} 的方块状态; 26.3 的方块模型走
 * 提交管线, 所以这里按原版 {@code TntRenderer} 的写法: 在 extract 阶段用 {@code BlockModelResolver}
 * 解析出 {@code BlockModelRenderState}, 在 submit 阶段做变换后交给 {@code TntMinecartRenderer}
 * (它同时负责 1.12 第二遍的白色闪烁立方体绘制)。
 * <p>
 * 平铺化 (ADR 0002) 之后每个爆炸物有自己的方块 ({@code ModBlocks.EXPLOSIVE_BLOCKS}), 1.12 的
 * {@code BlockExplosive.EX_PROP} 方块属性就是方块本身, 因此不再需要设置属性。
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderExBlock extends EntityRenderer<EntityExplosive, ExBlockRenderState>
{
    /** 与 {@code TntRenderer} 同样的方块展示上下文 (方块模型解析用) */
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    /** 上一次因为缺少方块形态而告警的爆炸物 id, 用来避免逐帧刷屏 */
    private @Nullable Identifier missingBlockId;

    /** 是否已经对 {@link #missingBlockId} 告警过 */
    private boolean missingBlockWarned;

    public RenderExBlock(EntityRendererProvider.Context context)
    {
        super(context);
        //1.12 不设置 shadowSize, 26.3 的 shadowRadius 默认也是 0, 保持一致 (不赋值)
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public void extractRenderState(EntityExplosive entity, ExBlockRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        //1.12 RenderExBlock: (float) entity.fuse - partialTicks + 1.0F
        state.fuseRemainingInTicks = entity.getFuse() - partialTicks + 1.0F;

        //1.12: BlockReg.blockExplosive.getDefaultState() + 旋转属性设为 UP + 爆炸物属性 (EX_PROP)
        //26.3: 旋转属性是 ExplosiveBlock.FACING (无对应方向来源, 与 1.12 一样硬编码 Direction.UP);
        //      1.12 的爆炸物属性 (EX_PROP) 在平铺化后就是方块本身, 由爆炸物 id 决定 (ADR 0002)
        final Identifier explosiveId = entity.getExplosiveId();
        final Block block = explosiveId == null ? null : ModBlocks.EXPLOSIVE_BLOCKS.get(explosiveId);
        if (block == null)
        {
            //rot/mutation 这类爆炸物在 26.3 平铺化后没有方块形态, 1.12 也一样没有对应的方块状态可言
            warnMissingBlock(explosiveId);
            return;
        }
        this.missingBlockWarned = false;

        final BlockState blockState = block.defaultBlockState()
                .setValue(ExplosiveBlock.FACING, Direction.UP); //TODO get direction from rotation
        this.blockModelResolver.update(state.blockState, blockState, BLOCK_DISPLAY_CONTEXT);
    }

    @Override
    public void submit(ExBlockRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        //1.12 RenderExBlock#doRender: pushMatrix(); renderBlock(...); popMatrix();
        poseStack.pushPose();

        //1.12: translate((float) x, (float) y + 0.5F, (float) z) —— x/z 由 26.3 的实体渲染分发器平移, 这里只剩下 +0.5F
        poseStack.translate(0.0F, 0.5F, 0.0F);

        //1.12: 爆炸前的膨胀, 指数是 f * f * f (1.12 有意保留的缺陷: 原版 TntRenderer 用的是四次方, 见差异说明)
        if (state.fuseRemainingInTicks < 10.0F)
        {
            float f = 1.0F - state.fuseRemainingInTicks / 10.0F;
            f = Mth.clamp(f, 0.0F, 1.0F);
            final float scale = 1.0F + f * f * f * 0.3F;
            poseStack.scale(scale, scale, scale);
        }

        //1.12: rotate(-90.0F, 0, 1, 0); translate(-0.5F, -0.5F, 0.5F); 末尾的 +90 度是 26.3 提交管线
        // 需要的回正 (同 TntRenderer#submit)
        poseStack.rotateDegrees(Axis.YP, -90.0F);
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, 90.0F);

        if (!state.blockState.isEmpty())
        {
            //1.12 在这里渲染两遍: 实体方块 + 白色闪烁的第二遍默认方块状态; 26.3 由 overlay 白闪承担
            TntMinecartRenderer.submitWhiteSolidBlock(state.blockState, poseStack, submitNodeCollector, state.lightCoords,
                    TntRenderer.isLit(state.fuseRemainingInTicks), state.outlineColor);
        }

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    //1.12: getEntityTexture() 返回 TextureMap.LOCATION_BLOCKS_TEXTURE, 26.3 的贴图由 RenderType 决定

    @Override
    public ExBlockRenderState createRenderState()
    {
        return new ExBlockRenderState();
    }

    /**
     * 记录没有方块形态的爆炸物, 同一个 id 只告警一次 (渲染每帧调用 extract, 直接告警会刷屏)。
     */
    private void warnMissingBlock(@Nullable Identifier explosiveId)
    {
        if (this.missingBlockWarned && Objects.equals(this.missingBlockId, explosiveId))
        {
            return;
        }
        this.missingBlockWarned = true;
        this.missingBlockId = explosiveId;
        ICBMClassicClient.LOGGER.warn("No flat explosive block for explosive({}), the primed explosive entity will not render (see ADR 0002)",
                explosiveId);
    }
}
