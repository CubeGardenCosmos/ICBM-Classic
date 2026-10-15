package icbm.classic.content.abm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EngagementRegistry
{
    private static final Map<ResourceKey<Level>, WorldEngagements> WORLDS = new HashMap<>();

    private static final class WorldEngagements
    {
        private final Map<UUID, Set<UUID>> activeByTarget = new HashMap<>();
        private final Set<UUID> intercepted = new HashSet<>();
    }

    public static void register(ServerLevel level, UUID targetId, UUID abmId)
    {
        world(level).activeByTarget.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(abmId);
    }

    public static int activeCount(ServerLevel level, UUID targetId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        if (world == null) return 0;
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

    public static void unregister(ServerLevel level, UUID targetId, UUID abmId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        if (world == null) return;
        Set<UUID> abms = world.activeByTarget.get(targetId);
        if (abms != null)
        {
            abms.remove(abmId);
            if (abms.isEmpty()) world.activeByTarget.remove(targetId);
        }
    }

    public static void markIntercepted(ServerLevel level, UUID targetId)
    {
        world(level).intercepted.add(targetId);
        WORLDS.get(level.dimension()).activeByTarget.remove(targetId);
    }

    public static boolean wasIntercepted(ServerLevel level, UUID targetId)
    {
        WorldEngagements world = WORLDS.get(level.dimension());
        return world != null && world.intercepted.contains(targetId);
    }

    public static void clear(ResourceKey<Level> dimension)
    {
        WORLDS.remove(dimension);
    }

    private static WorldEngagements world(ServerLevel level)
    {
        return WORLDS.computeIfAbsent(level.dimension(), ignored -> new WorldEngagements());
    }

    private EngagementRegistry() { }
}
