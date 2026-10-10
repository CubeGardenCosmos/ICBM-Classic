package icbm.classic.lib.explosive.reg;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.EntityTickFunction;
import icbm.classic.api.data.WorldEntityIntSupplier;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.reg.content.IExMinecartRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Content type for bomb carts. Holds the per explosive fuse time and fuse tick callback.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
@Deprecated
public class ExMinecartContentReg extends ExplosiveContentRegistry implements IExMinecartRegistry
{
    private final Map<Identifier, WorldEntityIntSupplier> fuseSetSupplier = new HashMap<>();
    private final Map<Identifier, EntityTickFunction> fuseTickCallback = new HashMap<>();

    public ExMinecartContentReg()
    {
        super(ICBMClassicAPI.EX_MINECART);
    }

    @Override
    public ItemStack getDeviceStack(Identifier regName)
    {
        // TODO(S2.05) return the flat "<ex>_bomb_cart" item once ModItems exists
        return ItemStack.EMPTY;
    }

    @Override
    public void setFuseSupplier(Identifier exName, WorldEntityIntSupplier fuseTimer)
    {
        fuseSetSupplier.put(exName, fuseTimer);
    }

    @Override
    public void setFuseTickListener(Identifier exName, EntityTickFunction function)
    {
        fuseTickCallback.put(exName, function);
    }

    @Override
    public void tickFuse(Entity entity, IExplosiveData data, int ticksExisted)
    {
        final EntityTickFunction function = fuseTickCallback.get(data == null ? null : data.getRegistryKey());
        if (function != null)
        {
            function.onTick(entity, ticksExisted);
        }
    }

    @Override
    public int getFuseTime(Entity entity, IExplosiveData data)
    {
        final WorldEntityIntSupplier function = fuseSetSupplier.get(data == null ? null : data.getRegistryKey());
        if (function != null)
        {
            return function.get(entity);
        }
        return 100;
    }
}
