package icbm.classic.content.blocks.launcher.screen;

import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.radio.messages.IIncomingMissileMessage;
import icbm.classic.api.radio.messages.ITargetMessage;
import icbm.classic.api.radio.messages.ITriggerActionMessage;
import icbm.classic.content.abm.radio.InterceptOrderMessage;
import icbm.classic.content.abm.radio.InterceptQueryMessage;
import icbm.classic.content.blocks.launcher.network.LauncherEntry;
import icbm.classic.content.blocks.launcher.base.TileLauncherBase;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.lib.radio.imp.RadioTile;
import icbm.classic.lib.radio.messages.RadioTranslations;
import icbm.classic.lib.radio.messages.TextMessage;
import net.minecraft.world.phys.Vec3;

/** Radio receiver attached to the launcher screen, preserving 1.12 target and trigger handling. */
public class RadioScreen extends RadioTile<TileLauncherScreen> implements IRadioReceiver
{
    public RadioScreen(TileLauncherScreen host)
    {
        super(host);
    }

    @Override
    public void onMessage(IRadioSender sender, IRadioMessage packet)
    {
        if (!canReceive(sender, packet)) return;
        if (packet instanceof IIncomingMissileMessage) return;
        if (packet instanceof InterceptQueryMessage query)
        {
            for (LauncherEntry entry : host.getLaunchersInGroup())
            {
                if (entry.getHost() instanceof TileLauncherBase baseLauncher)
                    baseLauncher.missileLauncher.offerIntercept(query);
            }
            return;
        }
        if (packet instanceof InterceptOrderMessage order)
        {
            for (LauncherEntry entry : host.getLaunchersInGroup())
            {
                if (entry.getHost() instanceof TileLauncherBase baseLauncher
                        && order.launcherPos().equals(baseLauncher.getBlockPos()))
                {
                    baseLauncher.missileLauncher.launchIntercept(order);
                    return;
                }
            }
            return;
        }

        // Target first: laser detonators can set a target and trigger from the same message.
        if (packet instanceof ITargetMessage targetMessage)
        {
            final double velocity = host.getLaunchersInGroup().stream()
                    .mapToDouble(entry -> entry.getLauncher().getPayloadVelocity()).max()
                    .orElse(ConfigMissile.DIRECT_FLIGHT_SPEED);
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
            if (host.fireAllLaunchers(false))
            {
                final Vec3 target = host.getTarget();
                final double distance = Math.sqrt(target.distanceToSqr(host.getBlockPos().getX() + 0.5,
                        host.getBlockPos().getY() + 0.5, host.getBlockPos().getZ() + 0.5));
                sender.onMessageCallback(this, new TextMessage(getChannel(), RadioTranslations.RADIO_LAUNCH_SUCCESS,
                        target.x, target.y, target.z, distance));
            }
            else
            {
                sender.onMessageCallback(this, new TextMessage(getChannel(), RadioTranslations.RADIO_LAUNCH_FAILED));
            }
        }
    }
}
