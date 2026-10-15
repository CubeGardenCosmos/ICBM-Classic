package icbm.classic.client.render.entity.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.render.entity.state.IcbmItemRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * EntityItem style render, the 26.3 port of 1.12 {@code client/render/entity/item/RenderItemImp}.
 * <p>
 * 1.12 drew an entity by pushing matrix transforms and calling {@code RenderItem#renderItem(stack, model)}
 * with the immediate mode API. 26.3 extracts the item model into an {@link ItemStackRenderState} during
 * {@code extractRenderState} and submits it in {@code submit}; the placement hooks keep the 1.12 shape
 * ({@code translate}/{@code rotate}/{@code scale}, once per index) so the subclasses read the same.
 * <p>
 * Differences from 1.12 (documented in the S4.07 evidence):
 * <ul>
 *     <li>The 1.12 default {@code translate} offset the stack by the {@code GROUND} scale of the baked
 *     model; 26.3 draws through {@link ItemDisplayContext#NONE} so there is no ground transform to read, that
 *     offset is dropped.</li>
 *     <li>{@code Item#isFull3D} (the "draw a tool at full size" switch of the payload) does not exist in
 *     26.3, the payload of the held item missile is scaled by a fixed 0.5.</li>
 * </ul>
 *
 * @param <T> entity to render
 * @param <S> render state of the entity
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public abstract class RenderItemImp<T extends Entity, S extends IcbmItemRenderState> extends EntityRenderer<T, S>
{
    protected final ItemModelResolver itemModelResolver;

    /**
     * 1.12 {@code BACKUP_RENDER_STACK}: an empty stack is drawn as fire instead of nothing. 26.3's fire block
     * has no item, the blaze powder is the closest fire themed item.
     * <p>
     * Created on first use, not as a static field: renderers are built during the resource reload, which runs
     * before the item components are bound, so building an {@link ItemStack} in a static initializer of a
     * renderer throws {@code Components not bound yet} and kills the render thread (the black screen of the
     * first S4.07 build).
     */
    private ItemStack backupRenderStack;

    /** 1.12 {@code billboard}: rotate to face the camera instead of using the entity yaw. */
    protected boolean billboard;

    protected RenderItemImp(EntityRendererProvider.Context context)
    {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        //1.12 RenderItemImp: shadowSize 0.15F, shadowOpaque 0.75F
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    public RenderItemImp<T, S> setBillboard(boolean billboard)
    {
        this.billboard = billboard;
        return this;
    }

    /** Item drawn for the given index, 1.12 {@code getRenderItem}. */
    protected abstract ItemStack getRenderItem(T entity, int index);

    /** Display context of the given index, 1.12 {@code getTransformType} ({@code NONE} for the entity itself). */
    protected ItemDisplayContext getTransformType(int index)
    {
        return index == 1 ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.NONE;
    }

    /** 1.12 {@code getYaw}. */
    protected float getYaw(T entity, float providedYaw)
    {
        return providedYaw;
    }

    /** 1.12 {@code getPitch}. */
    protected float getPitch(T entity, float providedPitch)
    {
        return providedPitch;
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        //1.12: the yaw handed in by the render manager is already lerped, the pitch is lerped here
        state.entityYaw = getYaw(entity, Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        state.entityPitch = getPitch(entity, Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));
        extractItems(entity, state);
    }

    /** Copies every item model the entity draws, 1.12 {@code doRenderItem}. */
    protected void extractItems(T entity, S state)
    {
        extractItem(entity, state.item, 0);
    }

    protected void extractItem(T entity, ItemStackRenderState target, int index)
    {
        ItemStack stack = getRenderItem(entity, index);
        if (stack == null || stack.isEmpty())
        {
            stack = getBackupRenderStack();
        }
        extractItemModel(itemModelResolver, target, stack, getTransformType(index), entity.level(), entity, entity.getId());
    }

    /** Shared item-model extraction primitive for entities and block entities. */
    public static void extractItemModel(ItemModelResolver resolver, ItemStackRenderState target, ItemStack stack,
                                        ItemDisplayContext context, net.minecraft.world.level.Level level,
                                        net.minecraft.world.entity.ItemOwner owner, int seed)
    {
        target.clear();
        if (stack != null && !stack.isEmpty())
        {
            resolver.updateForTopItem(target, stack, context, level, owner, seed);
        }
    }

    /** Shared item-model submission primitive for entities and block entities. */
    public static void submitItemModel(ItemStackRenderState item, PoseStack poseStack, SubmitNodeCollector collector,
                                       int lightCoords, int outlineColor)
    {
        if (!item.isEmpty())
        {
            item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, outlineColor);
        }
    }

    /** Lazily built fallback of {@link #extractItem}, see the field javadoc. */
    private ItemStack getBackupRenderStack()
    {
        if (backupRenderStack == null)
        {
            backupRenderStack = new ItemStack(Items.BLAZE_POWDER);
        }
        return backupRenderStack;
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        submitItem(state, poseStack, submitNodeCollector, camera, state.item, 0);
        if (state.hasSecondItem)
        {
            submitItem(state, poseStack, submitNodeCollector, camera, state.secondItem, 1);
        }

        //1.12: super.doRender(entity, x, y, z, entityYaw, partialTicks) - the base class draws the name tag
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private void submitItem(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera,
                            ItemStackRenderState item, int index)
    {
        if (item.isEmpty())
        {
            return;
        }

        poseStack.pushPose();
        //1.12: translate + rotate + scale, then the item model
        translate(poseStack, state, index);
        rotate(poseStack, state, camera, index);
        scale(poseStack, state, index);
        submitItemModel(item, poseStack, submitNodeCollector, state.lightCoords, state.outlineColor);
        poseStack.popPose();
    }

    /** 1.12 {@code translate(entity, itemstack, model, x, y, z, partialTicks, index)}. */
    protected void translate(PoseStack poseStack, S state, int index)
    {
        //26.3 already translated the pose to the interpolated entity position; the dropped 1.12 default
        //offset is explained in the class javadoc
    }

    /** 1.12 {@code rotate(entity, itemstack, entityYaw, entityPitch, partialTicks, index)}. */
    protected void rotate(PoseStack poseStack, S state, CameraRenderState camera, int index)
    {
        if (billboard)
        {
            //1.12: rotate(180.0F - renderManager.playerViewY) around the Y axis
            poseStack.rotateDegrees(Axis.YP, 180.0F - camera.yRot);
        }
        else
        {
            poseStack.rotateDegrees(Axis.YP, state.entityYaw);
        }
    }

    /** 1.12 {@code scale(entity, itemstack, partialTicks, index)}. */
    protected void scale(PoseStack poseStack, S state, int index)
    {
        //1.12 default did nothing
    }
}
