package icbm.classic.lib.capability.chicken;

import icbm.classic.lib.attachment.ModAttachments;
import net.minecraft.world.entity.Entity;

/**
 * Marks a chicken as "in space" (antigrav easter egg).
 * <p>
 * 1.12 kept the flag in a synced data manager entry backed by a Forge capability ({@code ICapabilityProvider}
 * + {@code INBTSerializable} + {@code AttachCapabilitiesEvent}). 26.3 stores it in the
 * {@link ModAttachments#SPACE_CHICKEN} data attachment, so both the data manager and the capability
 * wrapper are gone; the static helpers are kept for existing 1.12 call sites.
 *
 * Created by Dark(DarkGuardsman, Robin) on 2020-05-02.
 */
public final class CapSpaceChicken
{
    private CapSpaceChicken()
    {
    }

    public static boolean isSpace(Entity chicken)
    {
        return chicken != null && chicken.getAttachedOrElse(ModAttachments.SPACE_CHICKEN, false);
    }

    public static void setSpace(Entity chicken, boolean space)
    {
        if (chicken != null)
        {
            chicken.setAttached(ModAttachments.SPACE_CHICKEN, space);
        }
    }
}
