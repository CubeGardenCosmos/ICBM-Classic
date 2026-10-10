package icbm.classic.api.reg.events;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering missile source builders
 * <p>
 * 1.12 fired this through the Forge event bus. 26.3 uses a Fabric {@code Event}; register a listener with
 * {@code ActionCauseRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 */
public final class ActionCauseRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterActionCauses(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering missile source builders
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterActionCauses(IBuilderRegistry<IActionCause> registry);
    }
}
