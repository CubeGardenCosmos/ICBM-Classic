package icbm.classic.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Called when the target of a launcher is set.
 * Use this to change the position that is being set,
 * or cancel the event to not set any position.
 * <p>
 * 1.12 posted this on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * whose callback returns {@code boolean} (false cancels the set); listeners mutate the target through
 * the {@link #target} field.
 */
public class LauncherSetTargetEvent
{
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> event -> {
                for (Callback listener : listeners) {
                    if (!listener.onSetTarget(event)) {
                        return false;
                    }
                }
                return true;
            });

    public final Level world;
    public final BlockPos pos;
    public Vec3 target;

    public LauncherSetTargetEvent(Level world, BlockPos pos, Vec3 target)
    {
        this.world = world;
        this.pos = pos;
        this.target = target;
    }

    @FunctionalInterface
    public interface Callback
    {
        /**
         * @param event carrying the level, launcher position and target
         * @return false to not set any position
         */
        boolean onSetTarget(LauncherSetTargetEvent event);
    }
}
