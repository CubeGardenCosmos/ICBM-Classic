package icbm.classic.content.abm.radio;

import icbm.classic.api.radio.IRadioMessage;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class InterceptOrderMessage implements IRadioMessage
{
    private final String channel;
    private final UUID targetId;
    private final BlockPos launcherPos;
    private @Nullable UUID launchedAbmId;

    public InterceptOrderMessage(String channel, UUID targetId, BlockPos launcherPos)
    {
        this.channel = channel;
        this.targetId = targetId;
        this.launcherPos = launcherPos;
    }

    @Override
    public String getChannel() { return channel; }
    public UUID targetId() { return targetId; }
    public BlockPos launcherPos() { return launcherPos; }
    public void markLaunched(UUID abmId) { if (launchedAbmId == null) launchedAbmId = abmId; }
    public boolean launched() { return launchedAbmId != null; }
    public @Nullable UUID launchedAbmId() { return launchedAbmId; }
}
