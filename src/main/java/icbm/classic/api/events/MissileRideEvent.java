package icbm.classic.api.events;

import icbm.classic.api.missiles.IMissile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Called when a player is about to start or stop riding a missile.
 * <p>
 * 1.12 posted these on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * per variant; the callback returns {@code boolean} (false cancels the mount/dismount).
 */
public class MissileRideEvent
{
    public final IMissile missile;
    public final Player player;

    public MissileRideEvent(IMissile missile, Player player)
    {
        this.missile = missile;
        this.player = player;
    }

    /**
     * Called right before a player starts to ride a missile.
     * Cancel this event to disallow the player to ride the missile.
     */
    public static class Start extends MissileRideEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onStartRide(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public Start(IMissile missile, Player player)
        {
            super(missile, player);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile and player
             * @return false to disallow riding the missile
             */
            boolean onStartRide(Start event);
        }
    }

    /**
     * Called right before a player stops to ride a missile.
     * Cancel this event to disallow the player to dismount the missile.
     */
    public static class Stop extends MissileRideEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onStopRide(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public Stop(IMissile missile, Player player)
        {
            super(missile, player);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile and player
             * @return false to disallow dismounting the missile
             */
            boolean onStopRide(Stop event);
        }
    }
}
