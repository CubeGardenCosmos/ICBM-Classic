package icbm.classic.content.blocks.launcher.cruise;

import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.radio.messages.ITargetMessage;
import icbm.classic.api.radio.messages.ITriggerActionMessage;
import icbm.classic.api.radio.messages.IIncomingMissileMessage;
import icbm.classic.content.abm.radio.InterceptOrderMessage;
import icbm.classic.content.abm.radio.InterceptQueryMessage;
import icbm.classic.api.radio.IRadioTargetAssignable;
import icbm.classic.content.blocks.launcher.FiringPackage;
import icbm.classic.content.blocks.launcher.LauncherLangs;
import icbm.classic.content.blocks.launcher.status.LaunchedWithMissile;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.missile.logic.targeting.BasicTargetData;
import icbm.classic.lib.radio.imp.RadioTile;
import icbm.classic.lib.radio.messages.RadioTranslations;
import icbm.classic.lib.radio.messages.TextMessage;
import icbm.classic.prefab.FakeRadioSender;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/** Cruise launcher radio endpoint, preserving the legacy target and trigger messages. */
public class RadioCruise extends RadioTile<TileCruiseLauncher> implements IRadioReceiver
{
    public RadioCruise(TileCruiseLauncher host)
    {
        super(host);
    }

    @Override
    public void onMessage(IRadioSender sender, IRadioMessage packet)
    {
        if (!canReceive(sender, packet))
        {
            return;
        }
        if (packet instanceof IIncomingMissileMessage) return;
        if (packet instanceof InterceptQueryMessage query)
        {
            host.launcher.offerIntercept(query);
            return;
        }
        if (packet instanceof InterceptOrderMessage order)
        {
            host.launcher.launchIntercept(order);
            return;
        }
        if (packet instanceof ITargetMessage targetMessage)
        {
            final double velocity = host.launcher.getPayloadVelocity();
            final Vec3 target = targetMessage.getIntercept(host.getBlockPos().getX() + 0.5,
                    host.getBlockPos().getY() + 0.5, host.getBlockPos().getZ() + 0.5, velocity);
            if (target != null)
            {
                host.setTarget(target);
                if (!(packet instanceof ITriggerActionMessage))
                {
                    sender.onMessageCallback(this, new TextMessage(getChannel(), RadioTranslations.RADIO_TARGET_SET,
                            target.x, target.y, target.z));
                }
            }
            else
            {
                sender.onMessageCallback(this, new TextMessage(getChannel(), RadioTranslations.RADIO_TARGET_NULL));
            }
        }
        if (packet instanceof ITriggerActionMessage trigger && trigger.shouldTrigger())
        {
            if (host.getFiringPackage() != null)
            {
                sender.onMessageCallback(this, new TextMessage(getChannel(), LauncherLangs.ERROR_MISSILE_QUEUED));
                return;
            }
            host.setFiringPackage(getFiringPackage(sender, trigger));
            sender.onMessageCallback(this, new TextMessage(getChannel(), RadioTranslations.RADIO_LAUNCH_TRIGGERED));
        }
    }

    private @NonNull FiringPackage getFiringPackage(IRadioSender sender, ITriggerActionMessage packet)
    {
        final FiringPackage firingPackage = sender instanceof FakeRadioSender fake
                ? new FiringPackage(new BasicTargetData(host.getTarget()), new EntityCause(fake.player), 0)
                : new FiringPackage(new BasicTargetData(host.getTarget()), null, 0);
        firingPackage.setOnTriggerCallback(status -> {
            if (status instanceof LaunchedWithMissile launched
                    && launched.getMissile().getMissileEntity() instanceof IRadioTargetAssignable assignable)
            {
                packet.onTriggerCallback(assignable);
            }
            else
            {
                packet.onTriggerCallback(null);
            }
        });
        return firingPackage;
    }
}
