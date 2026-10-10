package icbm.classic.api.events;

import icbm.classic.api.missiles.IMissile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/**
 * Data carried by the missile events.
 * <p>
 * 1.12 extended the Forge {@code Event} class; 26.3 uses Fabric events, so this class is only the data
 * carrier and the nested classes expose their own Fabric {@code Event}. Cancelable variants use a
 * callback returning {@code boolean}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 *
 * @deprecated will be replaced by {@link icbm.classic.api.actions.IAction} event
 */
@Deprecated
public abstract class MissileEvent
{
    public final IMissile missile;
    public final Entity entityMissile;

    public MissileEvent(IMissile missile, Entity entityMissile)
    {
        this.missile = missile;
        this.entityMissile = entityMissile;
    }

    /**
     * Called right after the missile sets up its launch
     * data but before it is set into motion. Use this
     * to change settings, target, or trigger additional
     * logic.
     * <p>
     * Called after {@link LauncherEvent.PreLaunch}
     */
    public static class PostLaunch extends MissileEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        listener.onPostLaunch(event);
                    }
                });

        public PostLaunch(IMissile missile, Entity entityMissile)
        {
            super(missile, entityMissile);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile data and entity
             */
            void onPostLaunch(PostLaunch event);
        }
    }

    /**
     * Called right before the missile runs its impact code. Use
     * this to cancel impact for special use cases. Such as
     * having the missile pass through blocks, ignore friendly fire,
     * or trigger a different explosion/result.
     * <p>
     * This is normally called from the ray trace impact
     * function. The missile may be a tick away from the
     * actual impact point. Keep this in mind when using
     * the missile's position vs impact position from the
     * ray hit.
     * <p>
     * Called before {@link PostImpact}
     */
    public static class PreImpact extends MissileEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onPreImpact(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public final HitResult hit;

        public PreImpact(IMissile missile, Entity entityMissile, HitResult hit)
        {
            super(missile, entityMissile);
            this.hit = hit;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile data and impact hit result
             * @return false to cancel the impact
             */
            boolean onPreImpact(PreImpact event);
        }
    }

    /**
     * Called when the missile is about to enter the simulation queue. Use
     * this to prevent simulation or capture the missile to send it to a different
     * queue or system.
     *
     * Main purpose of this event is to block simulation. It does offer the option
     * to change how simulation works or switch queues. The problem with this is
     * cross dimension should be handled by other mechanics. As the missile may
     * not be able to predict this behavior correctly. Resulting in strange
     * interactions and broken expectations of the player.
     *
     * Instead, modify the flight/guidance system of the missile. Allowing it to
     * deliberately switching dimensions and properly enter the other dimension as expected.
     * Such as entering the bottom of a space dimension or orbit of a planet.
     *
     * For magic or disconnected dimensions please use a portal. This can easily be implemented
     * on the portal's side or the block impact system of the missile.
     */
    public static class EnteringSimQueue extends MissileEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onEnteringSimQueue(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public EnteringSimQueue(IMissile missile, Entity entityMissile)
        {
            super(missile, entityMissile);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile data and entity
             * @return false to prevent simulation
             */
            boolean onEnteringSimQueue(EnteringSimQueue event);
        }
    }

    /**
     * Called right after the missile ran its impact code.
     * <p>
     * Called after {@link PreImpact}
     */
    public static class PostImpact extends MissileEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        listener.onPostImpact(event);
                    }
                });

        public final HitResult hit;

        public PostImpact(IMissile missile, Entity entityMissile, HitResult hit)
        {
            super(missile, entityMissile);
            this.hit = hit;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the missile data and impact hit result
             */
            void onPostImpact(PostImpact event);
        }
    }
}
