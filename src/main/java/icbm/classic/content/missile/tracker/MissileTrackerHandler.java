package icbm.classic.content.missile.tracker;

import icbm.classic.api.events.MissileEvent;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles static entry points and dimension caching for missile simulation outside active chunks (S4.03).
 */
public class MissileTrackerHandler
{
    private static final Map<ResourceKey<Level>, MissileTrackerWorld> levelToHandlerMap = new ConcurrentHashMap<>();

    /**
     * Attempts to simulate the missile outside active chunks.
     *
     * @param missile explosive missile entity to simulate
     * @return true if missile entered simulation queue, false if blocked or ineligible
     */
    public static boolean simulateMissile(EntityExplosiveMissile missile)
    {
        if (missile != null && !missile.isRemoved() && !missile.isPassenger() && !EntityMissile.hasPlayerRiding(missile))
        {
            if (missile.level() instanceof ServerLevel serverLevel && missile.getMissileCapability().getTargetData() != null)
            {
                // Fire event to allow listeners to cancel simulation
                final MissileEvent.EnteringSimQueue event = new MissileEvent.EnteringSimQueue(missile.getMissileCapability(), missile);
                if (!MissileEvent.EnteringSimQueue.EVENT.invoker().onEnteringSimQueue(event))
                {
                    return false;
                }

                final MissileTrackerWorld tracker = getOrCreateHandler(serverLevel, true);
                if (tracker != null)
                {
                    tracker.simulateMissile(missile);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets or creates the tracker saved data for the given level.
     */
    public static MissileTrackerWorld getOrCreateHandler(ServerLevel level, boolean create)
    {
        if (level == null)
        {
            return null;
        }

        final ResourceKey<Level> dim = level.dimension();
        final MissileTrackerWorld existing = levelToHandlerMap.get(dim);
        if (existing != null)
        {
            return existing;
        }

        if (create)
        {
            final MissileTrackerWorld instance = level.getDataStorage().computeIfAbsent(MissileTrackerWorld.TYPE);
            levelToHandlerMap.put(dim, instance);
            return instance;
        }

        return null;
    }

    public static void onLevelLoad(ServerLevel level)
    {
        if (level != null)
        {
            getOrCreateHandler(level, true);
        }
    }

    public static void onLevelUnload(ServerLevel level)
    {
        if (level != null)
        {
            final MissileTrackerWorld handler = levelToHandlerMap.remove(level.dimension());
            if (handler != null)
            {
                handler.destroy(level);
            }
        }
    }

    public static void onLevelTick(ServerLevel level)
    {
        if (level != null)
        {
            final MissileTrackerWorld handler = getOrCreateHandler(level, false);
            if (handler != null)
            {
                handler.onLevelTick(level);
            }
        }
    }

    public static void clearAll()
    {
        levelToHandlerMap.clear();
    }
}
