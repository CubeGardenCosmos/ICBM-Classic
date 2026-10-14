package icbm.classic.lib.actions;

import icbm.classic.api.explosion.IBlast;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Simple handler to track blasts in order to disable or remove
 * <p>
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 4/9/2018.
 */
public class WorkTickingActionHandler //TODO create interface that is related to actions but isn't an action to replace this (IWorldTicking)
{
    /**
     * Blasts that are alive and ticking in the world.
     * <p>
     * 1.12 used a plain ArrayList, the port uses a copy on write list so the world tick, the worker
     * threads and the commands can not corrupt it.
     */
    public static final CopyOnWriteArrayList<IBlast> activeBlasts = new CopyOnWriteArrayList<>();

    public static void add(IBlast blast)
    {
        activeBlasts.add(blast);
    }

    public static void remove(IBlast blast)
    {
        activeBlasts.remove(blast);
    }

    /**
     * Called when a level unloads to clear the blasts left in it. 1.12 was a
     * {@code @Mod.EventBusSubscriber} on Forge's {@code WorldEvent.Unload}, 26.3 subscribes to
     * {@code ServerLevelEvents.UNLOAD} from {@link icbm.classic.ICBMClassic#onInitialize()}.
     * <p>
     * The legacy {@code !world.isRemote} guard is implicit as unload events are server side only.
     *
     * @param level that is being unloaded
     */
    public static void onLevelUnload(ServerLevel level)
    {
        if (!activeBlasts.isEmpty())
        {
            final ResourceKey<Level> dimension = level.dimension();
            activeBlasts.stream()
                    .filter(blast -> blast != null && (blast.world() == null || dimension.equals(blast.world().dimension())))
                    .forEach(IBlast::clearBlast);
        }
    }

    /**
     * Called when the server stops to drop every tracked blast.
     * <p>
     * No 1.12 equivalent, the legacy handler only reacted to a level unload and kept the blasts of a
     * stopped server around.
     */
    public static void clearAll()
    {
        final List<IBlast> toClear = new ArrayList<>(activeBlasts);
        activeBlasts.clear();
        toClear.forEach(IBlast::clearBlast);
    }

    /**
     * Called to remove blasts near the location
     *
     * @param world = position
     * @param x     - position
     * @param y     - position
     * @param z     - position
     * @param range - distance from position, less than zero will turn into global
     * @return number of blasts removed
     */
    public static int removeNear(Level world, double x, double y, double z, double range)
    {
        final Pos pos = new Pos(x, y, z);

        //Collect blasts marked for removal
        final List<IBlast> toRemove = WorkTickingActionHandler.activeBlasts.stream()
                .filter(blast -> blast.world() == world)
                .filter(blast -> range < 0 || range > 0 && range > pos.distance(blast))
                .collect(Collectors.toList());

        //Do removals
        activeBlasts.removeAll(toRemove);
        toRemove.forEach(IBlast::clearBlast);

        return toRemove.size();
    }
}
