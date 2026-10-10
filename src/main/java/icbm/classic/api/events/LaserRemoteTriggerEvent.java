package icbm.classic.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;

/**
 * Called on the server side when the player rightclicks
 * the laser detonator. Use this to change the target
 * block position, or cancel the event to not activate
 * the affected launcher.
 * <p>
 * 1.12 posted this on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * whose callback returns {@code boolean} (false cancels the trigger); listeners mutate the target
 * through {@link #setPos(Vec3)}.
 *
 * @deprecated will be replaced with an event providing {@link icbm.classic.api.actions.IAction}
 */
@Deprecated
public class LaserRemoteTriggerEvent
{
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> event -> {
                for (Callback listener : listeners) {
                    if (!listener.onTrigger(event)) {
                        return false;
                    }
                }
                return true;
            });

    public final Level world;
    public final Player player;
    private Vec3 pos;

    /** Optional translation key to show user for why it was canceled */
    public String cancelReason;

    public LaserRemoteTriggerEvent(Level world, Vec3 pos, Player player)
    {
        this.world = world;
        this.pos = pos;
        this.player = player;
    }

    /**
     * Updates the target position of the event
     *
     * @param pos to set, can't be null or will throw exception
     */
    public void setPos(@NonNull Vec3 pos) {
        if(pos == null) {
            throw new IllegalArgumentException("LaserRemoteTriggerEvent: target pos can not be set to null");
        }
        this.pos = pos;
    }

    public Vec3 getPos() {
        return pos;
    }

    @FunctionalInterface
    public interface Callback
    {
        /**
         * @param event carrying the level, player and target position
         * @return false to cancel the trigger
         */
        boolean onTrigger(LaserRemoteTriggerEvent event);
    }
}
