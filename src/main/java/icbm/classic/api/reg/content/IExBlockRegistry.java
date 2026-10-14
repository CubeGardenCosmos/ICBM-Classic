package icbm.classic.api.reg.content;

import icbm.classic.api.data.BlockActivateFunction;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

/**
 * @deprecated being removed in 1.13
 */
@Deprecated
public interface IExBlockRegistry extends IExplosiveContentRegistry, IExFuseBlockRegistry
{
    /**
     * Called to set a function to invoke when an explosive block is clicked
     * <p>
     * Do not use this in place of normal events, this is designed to add logic for
     * specific block types.
     *
     * @param exName
     * @param function
     */
    void setActivationListener(Identifier exName, BlockActivateFunction function);

    /**
     * Gets the activation function registered for an explosive.
     * <p>
     * 1.12 kept the map write only ({@code ExBlockContentReg#blockActivationCallbackMap} was populated but
     * never read, {@code BlockActivateFunction#onActivate} had no caller), so the getter is new with the
     * 26.3 invocation in {@code ExplosiveBlock#useWithoutItem}.
     *
     * @param exName registry key of the explosive
     * @return function to invoke on activation, null when none was registered
     */
    @Nullable
    BlockActivateFunction getActivationListener(Identifier exName);
}
