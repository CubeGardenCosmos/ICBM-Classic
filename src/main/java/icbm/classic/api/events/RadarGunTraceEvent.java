package icbm.classic.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Called on the server side when the player rightclicks
 * the radar gun. Use this to change the block position
 * that gets saved to the radar, or cancel the event to
 * not have any data saved.
 * <p>
 * 1.12 posted this on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * whose callback returns {@code boolean} (false cancels the trace); listeners mutate the saved position
 * through the {@link #pos} field.
 */
public class RadarGunTraceEvent
{
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> event -> {
                for (Callback listener : listeners) {
                    if (!listener.onTrace(event)) {
                        return false;
                    }
                }
                return true;
            });

    public final Level world;
    public final Player player;
    public Vec3 pos;

    public RadarGunTraceEvent(Level world, Vec3 pos, Player player)
    {
        this.world = world;
        this.pos = pos;
        this.player = player;
    }

    @FunctionalInterface
    public interface Callback
    {
        /**
         * @param event carrying the level, player and traced position
         * @return false to not save any data
         */
        boolean onTrace(RadarGunTraceEvent event);
    }
}
