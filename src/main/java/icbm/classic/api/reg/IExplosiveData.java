package icbm.classic.api.reg;

import icbm.classic.api.EnumTier;
import icbm.classic.api.actions.IActionData;

import org.jspecify.annotations.NonNull;

/**
 * Stores data about an explosive
 * <p>
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 1/4/19.
 *
 * @deprecated will be replaced by {@link IActionData}
 */
public interface IExplosiveData extends Comparable<IExplosiveData>, IActionData
{
    /**
     * Tier of the explosive.
     *
     * @return tier of the explosive
     */
    @Deprecated
    @NonNull
    default EnumTier getTier() {
        // Only for legacy explosives, moving forward tier is visual only and has no functionality
        return EnumTier.NONE;
    }
}
