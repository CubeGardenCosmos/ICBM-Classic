package icbm.classic.api.reg.events;

import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering projectile data
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code ProjectileDataRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class ProjectileDataRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterProjectileData(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering projectile data
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterProjectileData(IProjectileDataRegistry registry);
    }
}
