package icbm.classic.client.model;

import icbm.classic.ICBMConstants;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Space chicken helmet, the 26.3 port of 1.12 {@code client/models/ModelChickenHelmet} (Blockbench 4.6.4
 * export of the "voxel chicken helmet" easter egg).
 * <p>
 * 1.12 extended {@code ModelBase} and mutated {@code ModelRenderer#rotateAngleX/Y} in {@code render}. 26.3
 * models are built from a {@link LayerDefinition} and pose the parts in {@code setupAnim} before the base
 * class draws them, so the rotation is applied there and the cube data lives in
 * {@link #createLayerDefinition()}.
 *
 * @author QueenOfMissiles (model), 26.3 port S4.07
 */
public class ModelChickenHelmet extends Model<ChickenRenderState>
{
    /** Model layer id, registered by {@code EntityModelLayerRegistry} in {@code IcbmEntityRenderers}. */
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ICBMConstants.id("space_chicken"), "main");

    /** 1.12 built the model with textureWidth 64 / textureHeight 32. */
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 32;

    /** 1.12 {@code bone.setRotationPoint(0.0F, 15.0F, -4.0F)}. */
    private static final float BONE_ORIGIN_Y = 15.0F;
    private static final float BONE_ORIGIN_Z = -4.0F;

    private final ModelPart bone;

    public ModelChickenHelmet(ModelPart root)
    {
        //1.12 bound the texture itself, 26.3 models carry the render type of their texture
        super(root, RenderTypes::entityCutout);
        this.bone = root.getChild("bone");
    }

    /** Cube data of the helmet, 1.12 {@code ModelBox(bone, 40, 12, -3.0F, -6.5F, -4.5F, 6, 7, 6)}. */
    public static LayerDefinition createLayerDefinition()
    {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bone",
                CubeListBuilder.create().texOffs(40, 12).addBox(-3.0F, -6.5F, -4.5F, 6, 7, 6),
                PartPose.offset(0.0F, BONE_ORIGIN_Y, BONE_ORIGIN_Z));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void setupAnim(ChickenRenderState state)
    {
        //1.12: bone.rotateAngleX = headPitch * 0.017453292F; bone.rotateAngleY = netHeadYaw * 0.017453292F
        this.bone.xRot = state.xRot * 0.017453292F;
        this.bone.yRot = state.yRot * 0.017453292F;

        //1.12: if (isChild) { translate(0.0F, 5.0F * scale, 2.0F * scale); draw; }
        //26.3 has no per-draw matrix here, the offset is applied to the part (5 and 2 model units = the same
        //5/16 and 2/16 blocks the 1.12 render call translated by)
        this.bone.y = BONE_ORIGIN_Y + (state.isBaby ? 5.0F : 0.0F);
        this.bone.z = BONE_ORIGIN_Z + (state.isBaby ? 2.0F : 0.0F);
    }
}
