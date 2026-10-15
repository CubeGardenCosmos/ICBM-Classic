package icbm.classic.content.abm.radar;

import icbm.classic.content.abm.ConfigAbm;
import icbm.classic.content.abm.EngagementRegistry;
import icbm.classic.content.abm.math.Intercept;
import icbm.classic.content.abm.math.InterceptMath;
import icbm.classic.content.abm.math.Vec3d;
import icbm.classic.content.abm.radio.InterceptOffer;
import icbm.classic.content.abm.radio.InterceptOrderMessage;
import icbm.classic.content.abm.radio.InterceptQueryMessage;
import icbm.classic.content.blocks.radarstation.TileRadarStation;
import icbm.classic.lib.radio.RadioRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;

public final class FireControl
{
    public void tick(ServerLevel level, TileRadarStation radar)
    {
        int interval = Math.max(1, ConfigAbm.FIRE_CONTROL_INTERVAL_TICKS);
        long now = level.getGameTime();
        if (now % interval != 0) return;
        String channel = radar.getRadio().getChannel();
        if (channel == null || channel.isBlank() || ConfigAbm.MAX_INTERCEPTORS_PER_TARGET <= 0) return;

        radar.getTrackTable().threats().stream()
                .sorted(Comparator.comparingInt(track -> track.ticksToImpact))
                .forEach(track -> engage(level, radar, channel, now, track));
    }

    private void engage(ServerLevel level, TileRadarStation radar, String channel, long now, RadarTrack track)
    {
        if (now < track.nextEngageTick) return;
        if (!EngagementRegistry.canEngage(level, track.targetId)) return;

        InterceptQueryMessage query = new InterceptQueryMessage(channel, track.targetId,
                Vectors.to(track.position), Vectors.to(track.velocity), Vectors.to(track.acceleration));
        RadioRegistry.popMessage(level, radar.getRadio(), query);
        if (query.offers().isEmpty()) return;

        InterceptOffer best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (InterceptOffer offer : query.offers())
        {
            Vec3d launchPoint = Vectors.from(offer.launchPoint());
            Intercept intercept = InterceptMath.solve(launchPoint, offer.abmSpeed(), track.position, track.velocity,
                    track.acceleration, ConfigAbm.FUEL_TICKS);
            if (intercept == null) continue;
            double distance = intercept.aim().sub(launchPoint).length();
            if (best == null || Double.compare(distance, bestDistance) < 0
                    || (Double.compare(distance, bestDistance) == 0
                    && comparePos(offer.launcherPos(), best.launcherPos()) < 0))
            {
                best = offer;
                bestDistance = distance;
            }
        }
        if (best == null) return;

        InterceptOrderMessage order = new InterceptOrderMessage(channel, track.targetId, best.launcherPos());
        RadioRegistry.popMessage(level, radar.getRadio(), order);
        if (order.launched())
        {
            EngagementRegistry.register(level, track.targetId, order.launchedAbmId());
            track.setStatus(TrackStatus.ENGAGED, now);
        }
        track.nextEngageTick = now + ConfigAbm.REENGAGE_DELAY_TICKS;
    }

    private static int comparePos(BlockPos first, BlockPos second)
    {
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) return x;
        int y = Integer.compare(first.getY(), second.getY());
        return y != 0 ? y : Integer.compare(first.getZ(), second.getZ());
    }
}
