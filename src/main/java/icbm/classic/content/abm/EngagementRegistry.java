package icbm.classic.content.abm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which ABMs are engaging which target, plus the targets that were intercepted recently.
 * <p>
 * Every engagement is owned by the ABM id, not by the target it happens to fly at: {@link #register} moves an
 * ABM off whatever target it was registered to before, and {@link #unregister} releases it without needing to
 * know the current target. The target id therefore never has to be duplicated outside the entity, and an ABM
 * that switches targets (reacquire after losing a lock) cannot leave a stale registration behind.
 * <p>
 * Interception history is bounded: a record is only trusted for {@link #INTERCEPT_HISTORY_TICKS} ticks after
 * the interception, and recording a new interception drops every record that left that window.
 */
public final class EngagementRegistry
{
    /**
     * Ticks an interception is remembered after it happened. Must stay above the lifetime of the radar's
     * INTERCEPTED track (TrackTable.purgeLost drops such a track after 40 ticks) so a track that already
     * reacted to an interception keeps its status while it is displayed.
     */
    public static final int INTERCEPT_HISTORY_TICKS = 200;

    private static final Map<ResourceKey<Level>, WorldEngagements> WORLDS = new HashMap<>();

    private static final class WorldEngagements
    {
        private final Map<UUID, Set<UUID>> activeByTarget = new HashMap<>();
        private final Map<UUID, Long> intercepted = new HashMap<>();
    }

    /**
     * Registers {@code abmId} as engaging {@code targetId}. Registering an ABM that currently engages another
     * target moves the registration instead of adding a second one (target switch).
     */
    public static void register(ServerLevel level, UUID targetId, UUID abmId)
    {
        WorldEngagements world = world(level);
        release(world, abmId);
        world.activeByTarget.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(abmId);
    }

    /** Releases the engagement held by {@code abmId}, whatever target it is currently registered against. */
    public static void unregister(ServerLevel level, UUID abmId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        if (world != null) release(world, abmId);
    }

    public static int activeCount(ServerLevel level, UUID targetId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        if (world == null) return 0;
        sweep(world, level.getGameTime());
        Set<UUID> abms = world.activeByTarget.get(targetId);
        if (abms == null) return 0;
        abms.removeIf(abmId -> level.getEntity(abmId) == null);
        if (abms.isEmpty())
        {
            world.activeByTarget.remove(targetId);
            return 0;
        }
        return abms.size();
    }

    /**
     * Whether another interceptor may be committed to {@code targetId}. Single home of the in-flight limit so
     * radar fire control and an ABM picking a new target apply the same rule.
     * {@code MAX_INTERCEPTORS_PER_TARGET <= 0} disables engagement, mirroring FireControl.
     */
    public static boolean canEngage(ServerLevel level, UUID targetId)
    {
        return ConfigAbm.MAX_INTERCEPTORS_PER_TARGET > 0
                && activeCount(level, targetId) < ConfigAbm.MAX_INTERCEPTORS_PER_TARGET;
    }

    public static void markIntercepted(ServerLevel level, UUID targetId)
    {
        WorldEngagements world = world(level);
        final long now = level.getGameTime();
        world.intercepted.put(targetId, now);
        world.activeByTarget.remove(targetId);
        sweep(world, now);
    }

    public static boolean wasIntercepted(ServerLevel level, UUID targetId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        if (world == null) return false;
        Long at = world.intercepted.get(targetId);
        return at != null && level.getGameTime() - at <= INTERCEPT_HISTORY_TICKS;
    }

    public static void clear(ResourceKey<Level> dimension)
    {
        WORLDS.remove(dimension);
    }

    /** Drops interception records that left the retention window, keeping the history bounded. */
    private static void sweep(WorldEngagements world, long now)
    {
        world.intercepted.values().removeIf(at -> now - at > INTERCEPT_HISTORY_TICKS);
    }

    private static void release(WorldEngagements world, UUID abmId)
    {
        world.activeByTarget.values().removeIf(abms -> {
            abms.remove(abmId);
            return abms.isEmpty();
        });
    }

    private static WorldEngagements world(ServerLevel level)
    {
        return WORLDS.computeIfAbsent(level.dimension(), ignored -> new WorldEngagements());
    }

    private EngagementRegistry() { }
}
