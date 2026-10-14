package icbm.classic.lib.explosive.reg;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.EntityInteractionFunction;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.events.MissileEvent;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.reg.content.IExMissileRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import icbm.classic.content.reg.ModItems;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Content type for missiles.
 * <p>
 * The launch/update/interaction listeners were already empty stubs in 1.12 (only the ender missile
 * registered an interaction callback, which then did nothing). They are kept as stubs so the public
 * API shape is unchanged; implementing them belongs to the missile steps (S4.02).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
@Deprecated
public class ExMissileContentReg extends ExplosiveContentRegistry implements IExMissileRegistry
{
    private final Map<Identifier, Consumer<IMissile>> launchCallbacks = new HashMap<>();
    private final Map<Identifier, Consumer<IMissile>> updateCallbacks = new HashMap<>();
    private final Map<Identifier, EntityInteractionFunction> interactionCallbacks = new HashMap<>();

    public ExMissileContentReg()
    {
        super(ICBMClassicAPI.EX_MISSILE);
    }

    @Override
    public ItemStack getDeviceStack(Identifier regName)
    {
        final Item item = ModItems.EXPLOSIVE_MISSILES.get(regName);
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public void setLaunchListener(Identifier exName, Consumer<IMissile> eventCallback)
    {
        launchCallbacks.put(exName, eventCallback);
    }

    @Override
    public void setMissileUpdateListener(Identifier exName, Consumer<IMissile> eventCallback)
    {
        updateCallbacks.put(exName, eventCallback);
    }

    @Override
    public void setInteractionListener(Identifier exName, EntityInteractionFunction function)
    {
        interactionCallbacks.put(exName, function);
    }

    @Override
    public void triggerLaunch(IMissile missile)
    {
        // 1.12: MinecraftForge.EVENT_BUS.post(new MissileEvent.PostLaunch(missile, missile.getMissileEntity()))
        MissileEvent.PostLaunch.EVENT.invoker().onPostLaunch(new MissileEvent.PostLaunch(missile, missile.getMissileEntity()));
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(missile.getMissileEntity());
        if (explosive != null && explosive.getExplosiveData() != null)
        {
            final Consumer<IMissile> callback = launchCallbacks.get(explosive.getExplosiveData().getRegistryKey());
            if (callback != null)
            {
                callback.accept(missile);
            }
        }
    }

    @Override
    public void triggerFlightUpdate(IMissile missile)
    {
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(missile.getMissileEntity());
        if (explosive != null && explosive.getExplosiveData() != null)
        {
            final Consumer<IMissile> callback = updateCallbacks.get(explosive.getExplosiveData().getRegistryKey());
            if (callback != null)
            {
                callback.accept(missile);
            }
        }
    }

    @Override
    public boolean onInteraction(Entity entity, Player player, InteractionHand hand)
    {
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(entity);
        if (explosive != null && explosive.getExplosiveData() != null)
        {
            final EntityInteractionFunction callback = interactionCallbacks.get(explosive.getExplosiveData().getRegistryKey());
            if (callback != null)
            {
                return callback.onInteraction(entity, player, hand);
            }
        }
        return false;
    }
}
