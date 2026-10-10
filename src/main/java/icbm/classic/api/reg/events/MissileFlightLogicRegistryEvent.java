package icbm.classic.api.reg.events;

import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering missile flight logic builders
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code MissileFlightLogicRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class MissileFlightLogicRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterFlightLogic(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering missile flight logic builders
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterFlightLogic(IBuilderRegistry<IMissileFlightLogic> registry);
    }
}
