package icbm.classic.client.render.entity.item;

import icbm.classic.client.render.entity.state.IcbmItemRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/**
 * Renders an entity as a single item stack, the 26.3 port of 1.12 {@code RenderAsItem}.
 * <p>
 * 1.12 used it for the grenade (billboard) and the balloon; S4.07 registers it for both.
 *
 * @author Calclavia (1.12), 26.3 port S4.07
 */
public class RenderAsItem<T extends Entity> extends RenderItemImp<T, IcbmItemRenderState>
{
    private final Function<T, ItemStack> itemAccessor;

    public RenderAsItem(EntityRendererProvider.Context context, Function<T, ItemStack> itemAccessor)
    {
        super(context);
        this.itemAccessor = itemAccessor;
    }

    @Override
    protected ItemStack getRenderItem(T entity, int index)
    {
        return itemAccessor.apply(entity);
    }

    @Override
    public IcbmItemRenderState createRenderState()
    {
        return new IcbmItemRenderState();
    }
}
