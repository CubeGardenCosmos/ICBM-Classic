package icbm.classic.api.caps;

import icbm.classic.api.ICBMClassicHelpers;
import net.minecraft.world.item.ItemStack;

/**
 * Access to the missile of a launcher.
 * <p>
 * Keep in mind this may not be directly on the calling block.
 * It may be wrapped to the host block. Especially in the case of
 * multi-block such as the launcher base.
 * <p>
 * In theory this should just be a wrapper to the inventory. However,
 * it may do additional checks to ensure the missile is supported
 * <p>
 * 1.12 exposed this as a Forge capability ({@code ICBMClassicAPI.MISSILE_HOLDER_CAPABILITY}).
 * 26.3 uses {@link icbm.classic.lib.lookup.IcbmLookups#MISSILE_HOLDER}
 * ({@code icbmclassic:missile_holder}) instead, and block entities simply implement this interface.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/9/19.
 */
public interface IMissileHolder //TODO refactor to be a list
{

    /**
     * Gets the itemstack currently in the missile slot. This
     * may not actually be a missile if something bypassed checks
     *
     * @return stack in slot
     */
    ItemStack getMissileStack();

    /**
     * Checks if the missile stack is a missile
     *
     * @return true if missile
     */
    default boolean hasMissile() {
        return ICBMClassicHelpers.isMissile(getMissileStack());
    }

    /**
     * Called to insert the missile into the holder
     *
     * Insert will run {@link #canSupportMissile(ItemStack)} so no need
     * to call it before.
     *
     * @param stack    - stack
     * @param simulate - true to test insert, false to apply
     * @return remaining stack or empty stack if taken
     */
    ItemStack insertMissileStack(ItemStack stack, boolean simulate);

    /**
     * Called to consume the missile stack.
     *
     * @return true if missile was consumed
     */
    boolean consumeMissile();

    /**
     * Is the missile supported by the holder.
     *
     * @param stack - stack
     * @return true if supported
     */
    boolean canSupportMissile(ItemStack stack);
}
