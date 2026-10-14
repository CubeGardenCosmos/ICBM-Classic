package icbm.classic.client.render.entity;

import icbm.classic.client.model.ModelChickenHelmet;
import icbm.classic.client.render.entity.layer.LayerChickenHelmet;
import icbm.classic.client.render.entity.state.IcbmChickenRenderState;
import icbm.classic.lib.capability.chicken.CapSpaceChicken;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.world.entity.animal.chicken.Chicken;

/**
 * Chicken renderer that adds the space chicken helmet layer, replacing the vanilla registration for
 * {@code minecraft:chicken} (1.12 added the layer to the vanilla renderer in {@code ClientProxy#init}).
 * <p>
 * 26.3 render layers read the render state, not the entity, so the flag is copied here from the synced
 * {@code icbmclassic:space_chicken} attachment into {@link IcbmChickenRenderState}.
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class IcbmChickenRenderer extends ChickenRenderer
{
    public IcbmChickenRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        //1.12 ClientProxy#init: ((RenderChicken) render).addLayer(new LayerChickenHelmet(render))
        addLayer(new LayerChickenHelmet(this, new ModelChickenHelmet(context.bakeLayer(ModelChickenHelmet.LAYER))));
    }

    @Override
    public ChickenRenderState createRenderState()
    {
        return new IcbmChickenRenderState();
    }

    @Override
    public void extractRenderState(Chicken chicken, ChickenRenderState state, float partialTicks)
    {
        super.extractRenderState(chicken, state, partialTicks);
        if (state instanceof IcbmChickenRenderState chickenState)
        {
            chickenState.spaceChicken = CapSpaceChicken.isSpace(chicken);
        }
    }
}
