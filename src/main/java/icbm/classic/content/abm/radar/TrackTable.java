package icbm.classic.content.abm.radar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrackTable
{
    private final Map<UUID, RadarTrack> tracks = new LinkedHashMap<>();
    private int nextTrackId;

    public RadarTrack create(UUID targetId, icbm.classic.content.abm.math.Vec3d position,
                             icbm.classic.content.abm.math.Vec3d velocity, long tick)
    {
        RadarTrack track = new RadarTrack(targetId, nextTrackId++, position, velocity, tick);
        update(track);
        return track;
    }

    public void update(RadarTrack track)
    {
        tracks.put(track.targetId, track);
    }

    public RadarTrack get(UUID targetId)
    {
        return tracks.get(targetId);
    }

    public RadarTrack remove(UUID targetId)
    {
        return tracks.remove(targetId);
    }

    public Collection<RadarTrack> values()
    {
        return List.copyOf(tracks.values());
    }

    /** True when the table holds no tracks at all, cheaper than copying {@link #values()} only to check emptiness */
    public boolean isEmpty()
    {
        return tracks.isEmpty();
    }

    /** True when any track is a THREAT, short circuits instead of building and sorting a list (perf-audit F3) */
    public boolean hasThreat()
    {
        for (RadarTrack track : tracks.values())
        {
            if (track.status == TrackStatus.THREAT) return true;
        }
        return false;
    }

    /** True when any track is ENGAGED, short circuits instead of building a list (perf-audit F3) */
    public boolean hasEngaged()
    {
        for (RadarTrack track : tracks.values())
        {
            if (track.status == TrackStatus.ENGAGED) return true;
        }
        return false;
    }

    public List<RadarTrack> threats()
    {
        // Fresh list, so it is already isolated from the table; no need for a second defensive copy (perf-audit F3)
        final List<RadarTrack> threats = new ArrayList<>();
        for (RadarTrack track : tracks.values())
        {
            if (track.status == TrackStatus.THREAT) threats.add(track);
        }
        return threats;
    }

    /** Marks a missing contact LOST; tracks already LOST are removed on this subsequent scan. */
    public void markLost(UUID targetId, long tick)
    {
        RadarTrack track = tracks.get(targetId);
        if (track == null) return;
        if (track.status == TrackStatus.LOST)
        {
            tracks.remove(targetId);
        }
        else if (track.status != TrackStatus.INTERCEPTED)
        {
            track.setStatus(TrackStatus.LOST, tick);
        }
    }

    public void purgeLost(long tick)
    {
        tracks.values().removeIf(track -> track.status == TrackStatus.INTERCEPTED
                && tick - track.statusChangedTick >= 40);
    }

    public void clear()
    {
        tracks.clear();
    }
}
