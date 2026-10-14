package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icbm.classic.ICBMConstants;
import icbm.classic.client.render.entity.state.FragmentsRenderState;
import icbm.classic.content.entity.EntityFragments;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 渲染破片实体 ({@code icbmclassic:fragment})。
 * <p>
 * 两种形态, 与 1.12 的 {@code RenderFragments} 一致:
 * <ul>
 *     <li>铁砧 ({@code EntityFragments#isAnvil}): 随机朝向/损伤的铁砧方块, 走移动方块提交管线;</li>
 *     <li>破片: 一个手写四边形模型 (尾羽 + 四片翼), 顶点/UV 逐字照抄 1.12。</li>
 * </ul>
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderFragments extends EntityRenderer<EntityFragments, FragmentsRenderState>
{
    /** 1.12 从这里构造贴图 id ({@code ICBMConstants.DOMAIN} + 路径), 26.3 同理 */
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ICBMConstants.DOMAIN, "textures/entity/fragments/fragment.png");

    /** 1.12 渲染破片时关掉了光照, 26.3 的等价物是全亮顶点光照 */
    private static final int FULL_BRIGHT = 15728880;

    /**
     * 铁砧损伤的方块变体。
     * <p>
     * 1.12 只有一个 {@code Blocks.ANVIL} 方块 + {@code BlockAnvil.DAMAGE} 整数属性 (0..2), 26.3 平铺成
     * 三个方块 (damage 0/1/2)。1.12 用的是 {@code rand.nextInt(2)}, 从来取不到 damage 2, 这里按原样保留。
     */
    private static final Block[] ANVIL_DAMAGE_VARIANTS = {Blocks.ANVIL, Blocks.CHIPPED_ANVIL};

    /**
     * 铁砧的水平朝向候选。
     * <p>
     * 1.12 写的是 {@code Direction.Plane.HORIZONTAL.facings()[rand.nextInt(3)]}, 而 {@code facings()} 是按
     * 枚举声明顺序过滤出水平方向 (NORTH, SOUTH, WEST, EAST), 所以 {@code nextInt(3)} 取不到 EAST。这里按
     * 该顺序保留同样的"第四个方向取不到"的行为。
     */
    private static final Direction[] ANVIL_FACINGS = {Direction.NORTH, Direction.SOUTH, Direction.WEST};

    public RenderFragments(EntityRendererProvider.Context context)
    {
        super(context);
        //1.12 不设置 shadowSize, 26.3 的 shadowRadius 默认也是 0, 保持一致 (不赋值)
    }

    @Override
    public void extractRenderState(EntityFragments entity, FragmentsRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        state.anvil = entity.isAnvil();
        //1.12: entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks
        state.yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        //1.12: entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks
        state.xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        //1.12: float f9 = (float) entity.arrowShake - partialTicks;
        //      if (f9 > 0.0F) { float f10 = -Mth.sin(f9 * 3.0F) * f9; ... }
        state.arrowShake = entity.arrowShake;
        final float f9 = entity.arrowShake - partialTicks;
        state.arrowShakeAngle = f9 > 0.0F ? -Mth.sin(f9 * 3.0F) * f9 : 0.0F;

        if (state.anvil)
        {
            extractAnvilBlock(entity, state);
        }
    }

    @Override
    public void submit(FragmentsRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        if (state.anvil)
        {
            submitAnvil(state, poseStack, submitNodeCollector);
        }
        else
        {
            submitFragment(state, poseStack, submitNodeCollector);
        }

        //1.12: super.doRender(entity, x, y, z, entityYaw, partialTicks)
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public FragmentsRenderState createRenderState()
    {
        return new FragmentsRenderState();
    }

    /**
     * 铁砧分支的方块数据。
     * <p>
     * 1.12 在 {@code doRender} 里每帧随机一次朝向/损伤, 这里放在 extract (同样每帧一次), 见差异说明。
     */
    private static void extractAnvilBlock(EntityFragments entity, FragmentsRenderState state)
    {
        //1.12: Blocks.ANVIL + 损伤属性 (26.3 是三个方块变体) 取 rand.nextInt(2)
        //       Blocks.ANVIL + 朝向属性取 Direction.Plane.HORIZONTAL.facings()[rand.nextInt(3)]
        //TODO store rotation and damage in entity to reduce random nature
        final RandomSource random = entity.level().getRandom();
        final Block block = ANVIL_DAMAGE_VARIANTS[random.nextInt(ANVIL_DAMAGE_VARIANTS.length)];
        final Direction facing = ANVIL_FACINGS[random.nextInt(ANVIL_FACINGS.length)];
        final BlockState blockState = block.defaultBlockState().setValue(AnvilBlock.FACING, facing);

        //1.12 直接把方块画在实体位置上 (translate(x, y + 0.5F, z)), 方块位置取同一个点
        final BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() + 0.5D, entity.getZ());
        final MovingBlockRenderState movingBlock = state.anvilBlock;
        movingBlock.randomSeedPos = pos;
        movingBlock.blockPos = pos;
        movingBlock.blockState = blockState;
        if (entity.level() instanceof ClientLevel clientLevel)
        {
            //同原版 FallingBlockRenderer#extractRenderState: 移动方块需要生物群系/主方向光照/光照引擎
            movingBlock.biome = clientLevel.getBiome(pos);
            movingBlock.cardinalLighting = clientLevel.cardinalLighting();
            movingBlock.lightEngine = clientLevel.getLightEngine();
        }
    }

    /**
     * 铁砧形态: 1.12 {@code RenderFragments#doRender} 的 {@code entity.isAnvil} 分支。
     * <p>
     * 1.12 写的是 {@code translate(x, y + 0.5F, z) + rotate(-90, Y) + translate(-0.5F, -0.5F, 0.5F)}, 与
     * {@code RenderEntityBlock} 逐字相同; 这个矩阵只在 1.12 的方块渲染空间里把方块放到实体所在格,
     * 在 26.3 的移动方块管线里 {@code rotate(-90)} 会把方块整体推到实体西侧一格 (推导见 S4.07 证据)。
     * 这里采用原版 {@code FallingBlockRenderer} 的放置方式, 与 {@link RenderEntityBlock} 保持同一套映射。
     */
    private static void submitAnvil(FragmentsRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
    {
        poseStack.pushPose();
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        submitNodeCollector.submitMovingBlock(poseStack, state.anvilBlock, state.outlineColor);
        poseStack.popPose();
    }

    /**
     * 普通破片形态: 1.12 {@code RenderFragments#doRender} 的 {@code else} 分支。
     * <p>
     * 1.12 用立即模式顶点缓冲画两片尾羽 + 四片翼, 26.3 提交管线需要每片四边形单独提交一次 (提交时会把当前
     * 姿态复制一份), 所以 4 次循环里的 {@code rotate(90, X)} 累积在 poseStack 上, 与 1.12 一致。
     */
    private static void submitFragment(FragmentsRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
    {
        final RenderType renderType = RenderTypes.entityCutout(TEXTURE);

        poseStack.pushPose();
        //1.12 这里 disableLighting + color(1.0F, 1.0F, 1.0F, 1.0F), 26.3 用全亮顶点光 + 白色顶点色
        //1.12: translate((float) x, (float) y, (float) z) —— x/y/z 由分发器完成
        poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
        poseStack.rotateDegrees(Axis.ZP, state.xRot);
        if (state.arrowShakeAngle != 0.0F)
        {
            poseStack.rotateDegrees(Axis.ZP, state.arrowShakeAngle);
        }
        poseStack.rotateDegrees(Axis.XP, 45.0F);
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        //1.12: glNormal3f(0.05625F, 0.0F, 0.0F) + 尾羽面 (x = -7)
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
        {
            addVertex(pose, buffer, 0.05625F, 0.0F, 0.0F, -7.0F, -2.0F, -2.0F, 0.0F, 0.15625F);
            addVertex(pose, buffer, 0.05625F, 0.0F, 0.0F, -7.0F, -2.0F, 2.0F, 0.15625F, 0.15625F);
            addVertex(pose, buffer, 0.05625F, 0.0F, 0.0F, -7.0F, 2.0F, 2.0F, 0.15625F, 0.3125F);
            addVertex(pose, buffer, 0.05625F, 0.0F, 0.0F, -7.0F, 2.0F, -2.0F, 0.0F, 0.3125F);
        });
        //1.12: glNormal3f(-0.05625F, 0.0F, 0.0F) + 反向环绕的同一条尾羽面
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
        {
            addVertex(pose, buffer, -0.05625F, 0.0F, 0.0F, -7.0F, 2.0F, -2.0F, 0.0F, 0.15625F);
            addVertex(pose, buffer, -0.05625F, 0.0F, 0.0F, -7.0F, 2.0F, 2.0F, 0.15625F, 0.15625F);
            addVertex(pose, buffer, -0.05625F, 0.0F, 0.0F, -7.0F, -2.0F, 2.0F, 0.15625F, 0.3125F);
            addVertex(pose, buffer, -0.05625F, 0.0F, 0.0F, -7.0F, -2.0F, -2.0F, 0.0F, 0.3125F);
        });

        //1.12: for (int j = 0; j < 4; ++j) { rotate(90.0F, 1.0F, 0.0F, 0.0F); 画一片翼 }
        for (int j = 0; j < 4; ++j)
        {
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
            {
                addVertex(pose, buffer, 0.0F, 0.0F, 0.05625F, -8.0F, -2.0F, 0.0F, 0.0F, 0.0F);
                addVertex(pose, buffer, 0.0F, 0.0F, 0.05625F, 8.0F, -2.0F, 0.0F, 0.5F, 0.0F);
                addVertex(pose, buffer, 0.0F, 0.0F, 0.05625F, 8.0F, 2.0F, 0.0F, 0.5F, 0.15625F);
                addVertex(pose, buffer, 0.0F, 0.0F, 0.05625F, -8.0F, 2.0F, 0.0F, 0.0F, 0.15625F);
            });
        }

        poseStack.popPose();
    }

    /**
     * 1.12: {@code bufferbuilder.pos(x, y, z).tex(u, v).endVertex()}。
     * <p>
     * 1.12 的顶点格式是 POSITION_TEX (无颜色/光照), 光照被显式关闭; 26.3 的实体 RenderType 带颜色/光照,
     * 所以这里写白色顶点色 + 全亮光照 + {@code NO_OVERLAY}, 与 1.12 的观感等价。
     */
    private static void addVertex(PoseStack.Pose pose, VertexConsumer buffer, float normalX, float normalY, float normalZ,
                                 float x, float y, float z, float u, float v)
    {
        buffer.addVertex(pose, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
