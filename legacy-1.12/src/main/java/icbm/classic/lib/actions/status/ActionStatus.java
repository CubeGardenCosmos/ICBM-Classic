package icbm.classic.lib.actions.status;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.status.IActionStatus;
import lombok.NoArgsConstructor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

/**
 * Generic status messages that provide no details and are cached as static immutable versions
 */
@NoArgsConstructor
public class ActionStatus implements IActionStatus {

    private boolean error = false;
    private boolean block = false;
    protected String translationKey;
    protected ITextComponent textComponent;
    private ResourceLocation regName;

    public ActionStatus asError() {
        this.error = true;
        return this;
    }

    public ActionStatus asBlocking() {
        this.block = true;
        return this;
    }

    public ActionStatus withTranslation(String key) {
        this.translationKey = key;
        return this;
    }

    public ActionStatus withRegName(String key) {
        return withRegName(ICBMConstants.DOMAIN, key);
    }

    public ActionStatus withRegName(String domain, String key) {
        this.regName = new ResourceLocation(domain, key);
        return this;
    }

    @Override
    public boolean isError() {
        return error;
    }

    @Override
    public boolean isBlocking() {
        return isError() || block;
    }

    @Override
    public ITextComponent message() {
        if(textComponent == null) {
            textComponent = new TextComponentTranslation(translationKey);
        }
        return textComponent;
    }

    @Nonnull
    @Override
    public ResourceLocation getRegistryKey() {
        return regName;
    }

    @Override
    public String toString() {
        return "ActionStatus[ '" + getRegistryKey() + "' , '" + translationKey + "' ]@" + hashCode();
    }
}
