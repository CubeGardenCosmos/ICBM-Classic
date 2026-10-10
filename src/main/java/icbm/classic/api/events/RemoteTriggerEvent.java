package icbm.classic.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Called on the server side when the player rightclicks
 * the remote detonator. Use this to cancel the event to
 * not activate the affected launcher.
 * <p>
 * 1.12 posted this on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * whose callback returns {@code boolean} (false cancels the trigger).
 */
public class RemoteTriggerEvent
{
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> event -> {
                for (Callback listener : listeners) {
                    if (!listener.onTrigger(event)) {
                        return false;
                    }
                }
                return true;
            });

    public final Level world;
    public final Player player;
    public final ItemStack stack;

    public RemoteTriggerEvent(Level world, Player player, ItemStack stack)
    {
        this.world = world;
        this.stack = stack;
        this.player = player;
    }

    @FunctionalInterface
    public interface Callback
    {
        /**
         * @param event carrying the level, player and held stack
         * @return false to cancel the trigger
         */
        boolean onTrigger(RemoteTriggerEvent event);
    }
}
