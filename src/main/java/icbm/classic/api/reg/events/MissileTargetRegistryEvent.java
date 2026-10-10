package icbm.classic.api.reg.events;

import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering missile target builders
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code MissileTargetRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class MissileTargetRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterTargets(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering missile target builders
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterTargets(IBuilderRegistry<IMissileTarget> registry);
    }
}
