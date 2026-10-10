package icbm.classic.api.reg.events;

import icbm.classic.api.reg.IExplosiveRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Used to register new content types before explosives are registered.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 1/4/19.
 * <p>
 * 1.12 fired this through the Forge event bus and exposed a {@code register(id, contentRegistry)} helper on the event
 * object. 26.3 uses a Fabric {@code Event}; the listener receives the registry directly so it calls
 * {@code registry.registerContentRegistry(id, contentRegistry)} itself. Register a listener with
 * {@code ExplosiveContentRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 *
 * @deprecated being removed in 1.13
 */
@Deprecated
public final class ExplosiveContentRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterContent(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to register a new content type
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterContent(IExplosiveRegistry registry);
    }
}
