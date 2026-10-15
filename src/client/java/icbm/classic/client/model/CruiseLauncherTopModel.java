package icbm.classic.client.model;

import icbm.classic.ICBMConstants;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * 26.3 model port of the legacy {@code CruiseLauncherTopModel}, originally a Blockbench 4.6.4
 * {@code ModelICBM} with independent yaw and pitch joints.
 */
public final class CruiseLauncherTopModel extends ModelICBM
{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ICBMConstants.id("cruise_launcher_top"), "main");
    public static final Identifier TEXTURE = ICBMConstants.id("textures/models/cruise_launcher_top.png");

    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;

    private final ModelPart yaw;
    private final ModelPart pitch;

    public CruiseLauncherTopModel(ModelPart root)
    {
        super(root);
        this.yaw = root.getChild("yaw");
        this.pitch = yaw.getChild("pitch");
    }

    public static LayerDefinition createLayerDefinition()
    {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        final PartDefinition yaw = root.addOrReplaceChild("yaw",
                CubeListBuilder.create().texOffs(0, 56).addBox(-8.0F, -2.0F, -3.0F, 16, 2, 6),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        yaw.addOrReplaceChild("pitch", CubeListBuilder.create()
                        .texOffs(0, 13).addBox(4.0F, -2.0F, -7.0F, 2, 5, 14)
                        .texOffs(30, 13).addBox(-6.0F, -2.0F, -7.0F, 2, 5, 14)
                        .texOffs(3, 0).addBox(-4.0F, 1.0F, -6.0F, 8, 2, 4)
                        .texOffs(27, 0).addBox(-4.0F, 1.0F, 2.0F, 8, 2, 4),
                PartPose.offset(0.0F, -15.0F, 0.0F));
        yaw.addOrReplaceChild("arm1", CubeListBuilder.create()
                        .texOffs(6, 33).addBox(6.0F, -17.0F, -3.0F, 2, 5, 6)
                        .texOffs(2, 44).addBox(6.0F, -12.0F, -3.0F, 2, 10, 2)
                        .texOffs(18, 44).addBox(6.0F, -12.0F, 1.0F, 2, 10, 2),
                PartPose.ZERO);
        yaw.addOrReplaceChild("arm2", CubeListBuilder.create()
                        .texOffs(6, 33).addBox(6.0F, -17.0F, -3.0F, 2, 5, 6)
                        .texOffs(2, 44).addBox(6.0F, -12.0F, -3.0F, 2, 10, 2)
                        .texOffs(18, 44).addBox(6.0F, -12.0F, 1.0F, 2, 10, 2),
                PartPose.offset(-14.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** Applies the same negative degree-to-radian yaw/pitch model rotations as the 1.12 render method. */
    public void setAim(float yawRadians, float pitchRadians)
    {
        this.yaw.yRot = yawRadians;
        this.pitch.xRot = pitchRadians;
    }
}
