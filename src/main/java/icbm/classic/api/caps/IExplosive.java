package icbm.classic.api.caps;

import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/**
 * Used to provide an explosive for usage.
 * <p>
 * 1.12 exposed this as a Forge capability ({@code ICBMClassicAPI.EXPLOSIVE_CAPABILITY}).
 * 26.3 has no capability system: entities and block entities implement this interface directly
 * (used by {@code ICBMClassicHelpers#getExplosive(net.minecraft.world.entity.Entity)}), items
 * build an instance from their flat explosive id plus the {@code icbmclassic:explosive_settings}
 * data component (see porting-guide §2.6).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 *
 * @deprecated will be replaced by a capacity providing {@link IPotentialAction}
 */
public interface IExplosive
{

    /**
     * Gets the explosive provided
     *
     * @return explosive data
     */
    @NonNull
    IExplosiveData getExplosiveData();

    /**
     * Called to apply customizations.
     *
     * This is meant to be used in combination with {@link #addCustomization(IExplosiveCustomization)}
     * and any other customizations applyed by this explosive instance. It is recommended to use
     * customization objects to handle save/load.
     *
     * @param blast to customize
     */
    void applyCustomizations(IBlast blast);

    /**
     * Adds a layer of blast customization
     *
     * @param customization to apply during blast spawning
     */
    void addCustomization(IExplosiveCustomization customization);

    /**
     * Callback to allow explosives to add more information to items
     * and user interfaces. This is meant to show information
     * that can't easily be seen from the item itself. Such as
     * scale settings or customizations applied dynamically.
     *
     * @param collector to pass information into
     */
    default void collectInformation(Consumer<String> collector) {

    }

    /**
     * Gets the stack version of the explosive
     *
     * @return stack, or null if this has no stack
     */
    @Nullable
    ItemStack toStack();

    /**
     * Called when the explosive is defused
     *
     * @deprecated will be moved to it's own capability down the road
     */
    @Deprecated
    default void onDefuse() //TODO add args on who defused and how
    {

    }
}
