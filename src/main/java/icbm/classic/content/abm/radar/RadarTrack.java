package icbm.classic.content.abm.radar;

import icbm.classic.content.abm.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class RadarTrack
{
    public final UUID targetId;
    public final int trackId;
    public Vec3d position;
    public Vec3d velocity;
    public Vec3d acceleration = new Vec3d(0, 0, 0);
    public @Nullable Vec3d predictedImpact;
    public int ticksToImpact = Integer.MAX_VALUE;
    public TrackStatus status = TrackStatus.DETECTED;
    public long lastSeenTick;
    public long nextEngageTick;
    public long statusChangedTick;

    public RadarTrack(UUID targetId, int trackId, Vec3d position, Vec3d velocity, long lastSeenTick)
    {
        this.targetId = targetId;
        this.trackId = trackId;
        this.position = position;
        this.velocity = velocity;
        this.lastSeenTick = lastSeenTick;
    }

    public void setStatus(TrackStatus status, long tick)
    {
        if (this.status != status)
        {
            this.status = status;
            this.statusChangedTick = tick;
        }
    }
}
