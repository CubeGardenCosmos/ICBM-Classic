package icbm.classic.api.reg.events;

import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering action status message types
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code ActionStatusRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class ActionStatusRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterActionStatuses(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering action status message builders
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterActionStatuses(IBuilderRegistry<IActionStatus> registry);
    }
}
