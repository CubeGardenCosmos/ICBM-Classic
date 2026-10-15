package icbm.classic.content.abm.radio;

import icbm.classic.api.radio.IRadioMessage;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class InterceptQueryMessage implements IRadioMessage
{
    private final String channel;
    private final UUID targetId;
    private final Vec3 targetPos;
    private final Vec3 targetVel;
    private final Vec3 targetAcc;
    private final List<InterceptOffer> offers = new ArrayList<>();

    public InterceptQueryMessage(String channel, UUID targetId, Vec3 targetPos, Vec3 targetVel, Vec3 targetAcc)
    {
        this.channel = channel;
        this.targetId = targetId;
        this.targetPos = targetPos;
        this.targetVel = targetVel;
        this.targetAcc = targetAcc;
    }

    @Override
    public String getChannel() { return channel; }
    public UUID targetId() { return targetId; }
    public Vec3 targetPos() { return targetPos; }
    public Vec3 targetVel() { return targetVel; }
    public Vec3 targetAcc() { return targetAcc; }
    public void offer(InterceptOffer offer) { offers.add(offer); }
    public List<InterceptOffer> offers() { return Collections.unmodifiableList(offers); }
}
