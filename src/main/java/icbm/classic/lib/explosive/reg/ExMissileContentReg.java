package icbm.classic.lib.explosive.reg;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.EntityInteractionFunction;
import icbm.classic.api.events.MissileEvent;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.reg.content.IExMissileRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
    public ExMissileContentReg()
    {
        super(ICBMClassicAPI.EX_MISSILE);
    }

    @Override
    public ItemStack getDeviceStack(Identifier regName)
    {
        // TODO(S2.05) return the flat "<ex>_missile" item once ModItems exists
        return ItemStack.EMPTY;
    }

    @Override
    public void setLaunchListener(Identifier exName, Consumer<IMissile> eventCallback)
    {
        // TODO(S4.02) 1.12 kept this as an empty stub
    }

    @Override
    public void setMissileUpdateListener(Identifier exName, Consumer<IMissile> eventCallback)
    {
        // TODO(S4.02) 1.12 kept this as an empty stub
    }

    @Override
    public void setInteractionListener(Identifier exName, EntityInteractionFunction function)
    {
        // TODO(S4.02) 1.12 kept this as an empty stub
    }

    @Override
    public void triggerLaunch(IMissile missile)
    {
        // 1.12: MinecraftForge.EVENT_BUS.post(new MissileEvent.PostLaunch(missile, missile.getMissileEntity()))
        MissileEvent.PostLaunch.EVENT.invoker().onPostLaunch(new MissileEvent.PostLaunch(missile, missile.getMissileEntity()));
    }

    @Override
    public void triggerFlightUpdate(IMissile missile)
    {
        // 1.12 kept this as an empty stub
    }

    @Override
    public boolean onInteraction(Entity entity, Player player, InteractionHand hand)
    {
        return false;
    }
}
