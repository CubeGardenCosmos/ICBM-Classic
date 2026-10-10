package icbm.classic.lib.actions;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.lib.saving.NbtSaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * General purpose action for loading into entities, capabilities, and tiles. In which
 * the action triggered and condition are customizable.
 */
public final class PotentialAction extends PotentialActionImp<PotentialAction> {

    public static final ResourceLocation REG_NAME = new ResourceLocation(ICBMConstants.DOMAIN, "basic");

    @Setter() @Getter @Accessors(chain = true)
    private IActionData actionData;

    @Override
    public NBTTagCompound serializeNBT() {
        return SAVE_LOGIC.save(this, super.serializeNBT());
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        super.deserializeNBT(nbt);
        SAVE_LOGIC.load(this, nbt);
    }

    private static final NbtSaveHandler<PotentialAction> SAVE_LOGIC = new NbtSaveHandler<PotentialAction>()
        .mainRoot()
        /* */.nodeBuildableObject("action_data", () -> ICBMClassicAPI.ACTION_REGISTRY, PotentialAction::getActionData, PotentialAction::setActionData)
        .base();

    @Nonnull
    @Override
    public ResourceLocation getRegistryKey() {
        return REG_NAME;
    }
}
