package icbm.classic.content.actions;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionProvider;
import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * General use action provider for dynamic action setups
 * <p>
 * 1.12 registered this type as a Forge capability ({@code ActionProvider#register}). 26.3 has no capability
 * system, so the type is attached through API Lookup / data attachments in later steps (S1.07/S1.10).
 */
public final class ActionProvider implements IActionProvider, ISavable {
    private final Map<MetaTag, IPotentialAction> actions = new HashMap();

    public ActionProvider withAction(MetaTag tag, IPotentialAction action) {
        actions.put(tag, action);
        return this;
    }

    @Nullable
    @Override
    public IPotentialAction getPotentialAction(MetaTag key) {
        return actions.get(key); //TODO do key#isSubType
    }

    @Override
    public void save(ValueOutput out) {
        final ValueOutput.ValueOutputList list = out.childrenList("actions");
        actions.forEach((tag, action) -> {
            final ValueOutput entry = list.addChild();
            entry.putString("key", tag.getKey());
            ICBMClassicAPI.ACTION_POTENTIAL_REGISTRY.save(action, entry.child("value"));
        });
    }

    @Override
    public void load(ValueInput in) {
        in.childrenList("actions").ifPresent(list -> {
            actions.clear();
            for (ValueInput entry : list) {
                final String keyString = entry.getStringOr("key", "");
                final MetaTag key = MetaTag.find(keyString);
                final IPotentialAction value = ICBMClassicAPI.ACTION_POTENTIAL_REGISTRY.load(entry.childOrEmpty("value"));
                actions.put(key, value);
            }
        });
    }
}
