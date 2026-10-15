package icbm.classic.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/**
 * Client model base corresponding to the legacy {@code ModelICBM extends ModelBase}; the S5.07 cruise
 * launcher model is its only user. Modern models receive their baked {@link ModelPart} and texture pipeline.
 */
public abstract class ModelICBM extends Model<BlockEntityRenderState>
{
    protected ModelICBM(ModelPart root)
    {
        super(root, RenderTypes::entityCutout);
    }
}
