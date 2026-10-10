package icbm.classic.content.blocks.launcher.status;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.missiles.IMissile;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

/**
 * Same as {@link LauncherStatus#LAUNCHED} but provides missile fired for additional interaction
 */
@Data
public class LaunchedWithMissile implements IActionStatus {
    public static final ResourceLocation REG_NAME = new ResourceLocation(ICBMConstants.DOMAIN, "launcher.launched.missile_data");

    /**
     * Missile launched, runtime only and doesn't save
     */
    @Accessors(chain = true)
    private IMissile missile; //TODO try to save/load so message is still useful

    //TODO include missile target
    //TODO include missile payload, name, type, etc... anything useful to player

    @Override
    public ITextComponent message() {
        return LauncherStatus.LAUNCHED.message(); //TODO provide missile info
    }

    @Nonnull
    @Override
    public ResourceLocation getRegistryKey() {
        return REG_NAME;
    }
}
