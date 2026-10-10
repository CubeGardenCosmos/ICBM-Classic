package icbm.classic.lib.actions;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.lib.data.LazyBuilder;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.NonNull;

/**
 * Pre-built action for general purpose implementation where action is known
 */
public final class PotentialActionKnown extends PotentialActionImp<PotentialActionKnown> implements IActionFieldProvider {

    private final LazyBuilder<IActionData> actionData;

    public PotentialActionKnown(Identifier key) {
        this.actionData = new LazyBuilder<>(() -> ICBMClassicAPI.ACTION_REGISTRY.getOrBuild(key));
    }

    @NonNull
    @Override
    public IActionData getActionData() {
        return actionData.get();
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return null;
    }
}
