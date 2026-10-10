package icbm.classic.lib.capability.emp;

import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Registers the EMP receivers ICBM-Classic adds to vanilla types.
 * <p>
 * 1.12 registered a Forge capability in {@code CapabilityManager} and attached wrappers through
 * {@code AttachCapabilitiesEvent} ({@code EntityItem} → {@link CapabilityEmpEntityItem},
 * {@code EntityCreeper} → {@link CapabilityEmpCreeper}). 26.3 registrations happen on the API lookups
 * ({@link IcbmLookups#EMP_RECEIVER_ENTITY}) and are triggered from {@link IcbmLookups#init()}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
public final class CapabilityEMP
{
    private CapabilityEMP()
    {
    }

    /**
     * Registers the vanilla EMP receivers. Called by {@link IcbmLookups#init()} during mod
     * initialization.
     */
    public static void register()
    {
        // 1.12: AttachCapabilitiesEvent<Entity> - EntityItem -> CapabilityEmpEntityItem
        IcbmLookups.EMP_RECEIVER_ENTITY.registerForTypes(
                (entity, context) -> entity instanceof ItemEntity item ? new CapabilityEmpEntityItem(item) : null,
                EntityTypes.ITEM);

        // 1.12: AttachCapabilitiesEvent<Entity> - EntityCreeper -> CapabilityEmpCreeper
        IcbmLookups.EMP_RECEIVER_ENTITY.registerForTypes(
                (entity, context) -> entity instanceof Creeper creeper ? new CapabilityEmpCreeper(creeper) : null,
                EntityTypes.CREEPER);
    }
}
