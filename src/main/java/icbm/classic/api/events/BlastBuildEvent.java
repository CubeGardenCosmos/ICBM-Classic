package icbm.classic.api.events;

import icbm.classic.api.explosion.IBlastInit;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired when a blast is built to allow changing settings before the blast is locked into its settings.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/3/19.
 *
 * @deprecated being replaced with {@link icbm.classic.api.actions.IAction} which will not
 * provide an event for creating. Only an event for pre-run and post-run.
 */
@Deprecated
public class BlastBuildEvent extends BlastEvent<IBlastInit>
{
    /**
     * 1.12 posted this on the Forge event bus and was not cancelable. Fire with
     * {@code BlastBuildEvent.EVENT.invoker().onBlastBuilt(new BlastBuildEvent(blast))}; listeners
     * mutate the blast through {@link #getBlastInit()}.
     */
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
            listeners -> event -> {
                for (Callback listener : listeners) {
                    listener.onBlastBuilt(event);
                }
            });

    public BlastBuildEvent(IBlastInit blast)
    {
        super(blast);
    }

    /**
     * Casts this event's blast to {@link IBlastInit} to allow changing its values
     * @return The blast init
     */
    public IBlastInit getBlastInit()
    {
        return (IBlastInit)blast;
    }

    @FunctionalInterface
    public interface Callback
    {
        /**
         * Called to allow changing the blast settings before the blast is built.
         *
         * @param event carrying the blast being built
         */
        void onBlastBuilt(BlastBuildEvent event);
    }
}
