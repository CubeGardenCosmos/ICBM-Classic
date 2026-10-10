package icbm.classic.api.reg.events;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering action data message types
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code ActionRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class ActionRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterActions(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering action data message builders
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterActions(IBuilderRegistry<IActionData> registry);
    }
}
