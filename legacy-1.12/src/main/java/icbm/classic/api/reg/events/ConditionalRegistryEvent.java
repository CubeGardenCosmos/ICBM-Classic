package icbm.classic.api.reg.events;

import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Fired to allow registering conditional types
 */
public class ConditionalRegistryEvent extends Event
{
    public final IBuilderRegistry<ICondition> registry;

    public ConditionalRegistryEvent(IBuilderRegistry<ICondition> registry)
    {
        this.registry = registry;
    }
}
