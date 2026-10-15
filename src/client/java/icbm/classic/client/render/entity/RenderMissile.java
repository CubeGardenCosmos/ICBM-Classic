package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import icbm.classic.client.render.entity.item.RenderItemImp;
import icbm.classic.client.render.entity.state.IcbmItemRenderState;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.entity.itemstack.EntityHeldItemMissile;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Renders a missile as its item stack, the 26.3 port of 1.12 {@code client/render/entity/RenderMissile}.
 * <p>
 * The offsets and rotations of the 1.12 renderer are kept verbatim: the item is drawn 0.2 above the entity
 * centre, rotated by {@code yaw - 180} and {@code pitch - 90} and then moved to the rotation point of the
 * model ({@code (0, -0.8, 0)}). A held item missile additionally draws its payload at index 1, rotated by an
 * extra 10 degrees and moved by {@code (0, 0.27, -0.12)}.
 *
 * @param <T> missile entity
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderMissile<T extends EntityMissile<T>> extends RenderItemImp<T, IcbmItemRenderState>
{
    public RenderMissile(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public IcbmItemRenderState createRenderState()
    {
        return new IcbmItemRenderState();
    }

    @Override
    protected ItemStack getRenderItem(T entity, int index)
    {
        if (index == 1 && entity instanceof EntityHeldItemMissile heldItemMissile)
        {
            return heldItemMissile.getHeldItem();
        }
        return entity.getRenderStack();
    }

    @Override
    protected void extractItems(T entity, IcbmItemRenderState state)
    {
        super.extractItems(entity, state);
        //1.12 RenderMissile#doRenderItem: the held item missile draws a second item
        if (entity instanceof EntityHeldItemMissile)
        {
            state.hasSecondItem = true;
            extractItem(entity, state.secondItem, 1);
        }
    }

    /** 1.12 {@code getYaw}: the model faces the other way round. */
    @Override
    protected float getYaw(T entity, float providedYaw)
    {
        return providedYaw - 180;
    }

    /** 1.12 {@code getPitch}: the model is authored upright. */
    @Override
    protected float getPitch(T entity, float providedPitch)
    {
        return providedPitch - 90;
    }

    @Override
    protected void translate(PoseStack poseStack, IcbmItemRenderState state, int index)
    {
        if (index == 0)
        {
            //1.12: translate to center of entity collider
            poseStack.translate(0.0F, 0.2F, 0.0F);
        }
    }

    @Override
    protected void rotate(PoseStack poseStack, IcbmItemRenderState state, CameraRenderState camera, int index)
    {
        applyMissileRotation(poseStack, state.entityYaw, state.entityPitch + (index == 1 ? 10.0F : 0.0F), index);
    }

    /**
     * Shared 1.12 missile item transform used by entity and block-entity renderers.
     * The initial 0.2 vertical offset is applied by the caller, matching {@link #translate}.
     */
    public static void applyMissileRotation(PoseStack poseStack, float yaw, float pitch, int index)
    {
        poseStack.rotateDegrees(Axis.YP, yaw);
        poseStack.rotateDegrees(Axis.XP, pitch);

        if (index == 0)
        {
            poseStack.translate(0.0F, -0.8F, 0.0F);
        }
        else
        {
            poseStack.translate(0.0F, 0.27F, -0.12F);
        }
    }

    @Override
    protected void scale(PoseStack poseStack, IcbmItemRenderState state, int index)
    {
        //1.12: if (index == 1 && !itemstack.getItem().isFull3D()) scale(0.5, 0.5, 0.5)
        //26.3 has no isFull3D, the payload is scaled by a fixed 0.5 (see the S4.07 evidence)
        if (index == 1)
        {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    /**
     * 1.12 {@code EntityMissile#getRenderBoundingBox} expanded the box by 5 blocks so the missile is not
     * culled while its model sticks out of the collider.
     */
    @Override
    protected AABB getBoundingBoxForCulling(T entity, float partialTicks)
    {
        return entity.getBoundingBox().inflate(5.0D);
    }
}
