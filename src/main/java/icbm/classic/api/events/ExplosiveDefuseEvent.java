package icbm.classic.api.events;

import icbm.classic.api.caps.IExplosive;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Called when a player leftclicks an entity
 * that can be defused. Use specific events for
 * more control.
 * <p>
 * 1.12 posted these on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * per variant; the callback returns {@code boolean} (false cancels the defuse).
 *
 * @deprecated will be replaced with an event providing {@link icbm.classic.api.actions.IAction}
 */
@Deprecated
public class ExplosiveDefuseEvent
{
    public final Player player;
    public final Entity entityToDefuse;

    public ExplosiveDefuseEvent(Player player, Entity entityToDefuse)
    {
        this.player = player;
        this.entityToDefuse = entityToDefuse;
    }

    /**
     * Called when a player leftclicks an ICBM explosive.
     * Cancel to not defuse the explosive.
     */
    public static class ICBMExplosive extends ExplosiveDefuseEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onDefuse(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public final IExplosive explosive;

        public ICBMExplosive(Player player, Entity entityToDefuse, IExplosive explosive)
        {
            super(player, entityToDefuse);

            this.explosive = explosive;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the player, entity and explosive
             * @return false to not defuse the explosive
             */
            boolean onDefuse(ICBMExplosive event);
        }
    }

    /**
     * Called when a player leftclicks a vanilla TNT block,
     * or potential derivates of other mods. Cancel to not
     * defuse the tnt.
     */
    public static class TNTExplosive extends ExplosiveDefuseEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onDefuse(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public TNTExplosive(Player player, Entity entityToDefuse)
        {
            super(player, entityToDefuse);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the player and entity
             * @return false to not defuse the tnt
             */
            boolean onDefuse(TNTExplosive event);
        }
    }

    /**
     * Called when a player leftclicks an ICBM bomb cart.
     * Cancel to not defuse the bomb cart.
     */
    public static class ICBMBombCart extends ExplosiveDefuseEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onDefuse(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public ICBMBombCart(Player player, Entity entityToDefuse)
        {
            super(player, entityToDefuse);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the player and entity
             * @return false to not defuse the bomb cart
             */
            boolean onDefuse(ICBMBombCart event);
        }
    }
}
