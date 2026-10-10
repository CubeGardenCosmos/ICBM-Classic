package icbm.classic.api.reg.events;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired to allow registering missile source builders
 */
public class ActionCauseRegistryEvent extends Event
{
    public final IBuilderRegistry<IActionCause> registry;

    public ActionCauseRegistryEvent(IBuilderRegistry<IActionCause> registry)
    {
        this.registry = registry;
    }
}
