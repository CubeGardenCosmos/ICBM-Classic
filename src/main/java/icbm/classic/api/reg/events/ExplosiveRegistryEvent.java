package icbm.classic.api.reg.events;

import icbm.classic.api.reg.IExplosiveRegistry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired to allow registering new explosive types
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 1/4/19.
 * <p>
 * 1.12 fired this through the Forge event bus and exposed {@code register(...)}, {@code enableContent(...)}, and
 * {@code done()} sugar helpers on the event object. Those only existed because Forge listeners could not reach the
 * registry directly. 26.3 uses a Fabric {@code Event}; the listener receives the registry directly, so it calls
 * {@code registry.register(id, tier, blastFactory)} and
 * {@code registry.getContentRegistry(contentID).enableContent(id)} itself. Register a listener with
 * {@code ExplosiveRegistryEvent.EVENT.register(registry -> ...)} before the registry locks.
 *
 * @deprecated being removed in 1.13
 */
@Deprecated
public final class ExplosiveRegistryEvent {

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> registry -> {
                for (Callback listener : listeners) {
                    listener.onRegisterExplosives(registry);
                }
            });

    @FunctionalInterface
    public interface Callback {
        /**
         * Called to allow registering new explosives
         *
         * @param registry to register into, locked after the event returns
         */
        void onRegisterExplosives(IExplosiveRegistry registry);
    }
}
