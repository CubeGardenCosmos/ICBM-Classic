package icbm.classic.content.blocks.emptower;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.radio.messages.ITriggerActionMessage;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.lib.radio.imp.RadioTile;
import icbm.classic.lib.radio.messages.IncomingMissileMessage;
import icbm.classic.lib.radio.messages.TextMessage;
import icbm.classic.prefab.FakeRadioSender;

public class RadioEmpTower extends RadioTile<TileEMPTower> implements IRadioReceiver
{
    public static final String SUCCESS = "tile.emptower.radio.success";

    public RadioEmpTower(TileEMPTower host)
    {
        super(host);
    }

    @Override
    public void onMessage(IRadioSender sender, IRadioMessage packet)
    {
        if (!canReceive(sender, packet)) return;
        final IActionCause cause = sender instanceof FakeRadioSender fake ? new EntityCause(fake.player) : null;
        if (packet instanceof IncomingMissileMessage incoming && incoming.shouldTrigger())
        {
            final IMissile missile = incoming.getMissile();
            if (missile != null && missile.getMissileEntity() != null
                    && Math.sqrt(missile.getVec3().distanceToSqr(host.getBlockPos().getX() + 0.5,
                    host.getBlockPos().getY() + 0.5, host.getBlockPos().getZ() + 0.5)) < host.getRange()
                    && host.fire(cause))
                sender.onMessageCallback(this, new TextMessage(getChannel(), SUCCESS));
        }
        else if (packet instanceof ITriggerActionMessage trigger && trigger.shouldTrigger() && host.fire(cause))
        {
            sender.onMessageCallback(this, new TextMessage(getChannel(), SUCCESS));
        }
    }
}
