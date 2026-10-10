package icbm.classic.lib.actions;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

/**
 * General purpose action for loading into entities, capabilities, and tiles. In which
 * the action triggered and condition are customizable.
 */
public final class PotentialAction extends PotentialActionImp<PotentialAction> {

    public static final Identifier REG_NAME = ICBMConstants.id("basic");

    @Setter() @Getter @Accessors(chain = true)
    private IActionData actionData;

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<PotentialAction> SAVE_LOGIC = new SaveHandler<PotentialAction>()
        .mainRoot()
        /* */.nodeBuildableObject("action_data", () -> ICBMClassicAPI.ACTION_REGISTRY, PotentialAction::getActionData, PotentialAction::setActionData)
        .base();

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }
}
