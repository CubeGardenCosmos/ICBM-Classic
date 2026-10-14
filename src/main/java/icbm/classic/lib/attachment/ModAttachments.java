package icbm.classic.lib.attachment;

import com.mojang.serialization.Codec;
import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Data attachments used by ICBM-Classic.
 * <p>
 * 1.12 stored these as Forge capabilities on entities. 26.3 uses the Fabric data attachment API
 * ({@code docs/architecture/porting-guide.md} §2.6), registered during initialization.
 *
 * Created by Dark(DarkGuardsman, Robin) on 2026-10-10.
 */
public final class ModAttachments
{
    /**
     * Marks a chicken as "in space" (antigrav easter egg). 1.12 was {@code CapSpaceChicken} with a data
     * manager value plus a capability; both are gone in 26.3.
     * <p>
     * S4.07: the flag is synced to clients with {@link AttachmentSyncPredicate#all()} because the space
     * chicken helmet is a client side render layer (1.12's data manager entry was synced the same way).
     */
    public static final AttachmentType<Boolean> SPACE_CHICKEN =
            AttachmentRegistry.create(ICBMConstants.id("space_chicken"), builder -> builder
                    .persistent(Codec.BOOL)
                    .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    private ModAttachments()
    {
    }

    /**
     * Force class loading so the attachment is registered. The attachment registry must be written
     * during mod initialization, before any world is loaded.
     */
    public static void init()
    {
    }
}
