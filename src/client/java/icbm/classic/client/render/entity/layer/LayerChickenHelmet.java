package icbm.classic.client.render.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import icbm.classic.ICBMConstants;
import icbm.classic.client.model.ModelChickenHelmet;
import icbm.classic.client.render.entity.state.IcbmChickenRenderState;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws the space chicken helmet on a chicken flagged with {@code icbmclassic:space_chicken}, the 26.3 port
 * of 1.12 {@code client/render/entity/layer/LayerChickenHelmet}.
 * <p>
 * 1.12 could read the entity capability in the layer; 26.3 layers only see the render state, so the flag is
 * carried by {@link IcbmChickenRenderState} (set by {@code IcbmChickenRenderer}).
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class LayerChickenHelmet extends RenderLayer<ChickenRenderState, ChickenModel>
{
    public static final Identifier TEXTURE = ICBMConstants.id("textures/entity/space_chicken.png");

    private final ModelChickenHelmet helmet;

    public LayerChickenHelmet(RenderLayerParent<ChickenRenderState, ChickenModel> parent, ModelChickenHelmet helmet)
    {
        super(parent);
        this.helmet = helmet;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, ChickenRenderState state,
                       float limbSwing, float limbSwingAmount)
    {
        if (!(state instanceof IcbmChickenRenderState chickenState) || !chickenState.spaceChicken)
        {
            return;
        }

        //1.12: bindTexture + color(1.0F, 1.0F, 1.0F, 1.0F) + blend with alphaFunc(1/255), which the cutout
        //render type of the model covers in 26.3
        renderColoredCutoutModel(helmet, TEXTURE, poseStack, submitNodeCollector, light, state, -1, OverlayTexture.NO_OVERLAY);
    }
}
