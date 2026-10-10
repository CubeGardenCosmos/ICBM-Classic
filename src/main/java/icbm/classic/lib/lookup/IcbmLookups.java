package icbm.classic.lib.lookup;

import icbm.classic.ICBMConstants;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.caps.IMissileHolder;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.lib.attachment.ModAttachments;
import icbm.classic.lib.capability.emp.CapabilityEMP;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.minecraft.core.Direction;

import org.jspecify.annotations.Nullable;

/**
 * API lookups that replace the 1.12 Forge capabilities used by ICBM-Classic.
 * <p>
 * See {@code docs/architecture/porting-guide.md} §2.6. Every 1.12 capability has a counterpart here
 * or on the content type itself:
 * <ul>
 *     <li>{@code MISSILE_LAUNCHER_CAPABILITY} → {@link #MISSILE_LAUNCHER}</li>
 *     <li>{@code MISSILE_HOLDER_CAPABILITY} → {@link #MISSILE_HOLDER}</li>
 *     <li>{@code EMP_CAPABILITY} → {@link #EMP_RECEIVER_BLOCK}/{@link #EMP_RECEIVER_ENTITY}/{@link #EMP_RECEIVER_ITEM}</li>
 *     <li>{@code MISSILE_CAPABILITY} → {@link #MISSILE}</li>
 *     <li>{@code MISSILE_STACK_CAPABILITY} → {@link #MISSILE_STACK}</li>
 *     <li>{@code EXPLOSIVE_CAPABILITY} → {@link icbm.classic.api.caps.IExplosive} implemented by entities and
 *     block entities; items use the {@code icbmclassic:explosive_settings} data component (S2.02)</li>
 *     <li>{@code GPS_CAPABILITY} → {@code icbmclassic:gps_data} data component (S2.04/S5.08)</li>
 *     <li>{@code RADIO_CAPABILITY} → dropped, radios implement {@link icbm.classic.api.radio.IRadio} directly</li>
 * </ul>
 * The class is loaded by {@link #init()} from {@code ICBMClassic.onInitialize()}; that call also performs
 * the vanilla default registrations (EMP receivers) and loads the data attachments.
 *
 * Created by Dark(DarkGuardsman, Robin) on 2026-10-10.
 */
public final class IcbmLookups
{
    /** Launcher capability for a block, context is the side being asked about */
    public static final BlockApiLookup<IMissileLauncher, @Nullable Direction> MISSILE_LAUNCHER =
            BlockApiLookup.get(ICBMConstants.id("missile_launcher"), IMissileLauncher.class, Direction.class);

    /** Missile inventory capability for a block, context is the side being asked about */
    public static final BlockApiLookup<IMissileHolder, @Nullable Direction> MISSILE_HOLDER =
            BlockApiLookup.get(ICBMConstants.id("missile_holder"), IMissileHolder.class, Direction.class);

    /** EMP receiver on a block */
    public static final BlockApiLookup<IEMPReceiver, Void> EMP_RECEIVER_BLOCK =
            BlockApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);

    /** EMP receiver on an entity */
    public static final EntityApiLookup<IEMPReceiver, Void> EMP_RECEIVER_ENTITY =
            EntityApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);

    /** EMP receiver on an item stack */
    public static final ItemApiLookup<IEMPReceiver, Void> EMP_RECEIVER_ITEM =
            ItemApiLookup.get(ICBMConstants.id("emp_receiver"), IEMPReceiver.class, Void.class);

    /** Missile on an entity */
    public static final EntityApiLookup<IMissile, Void> MISSILE =
            EntityApiLookup.get(ICBMConstants.id("missile"), IMissile.class, Void.class);

    /**
     * Missile data on an item stack. 1.12 used {@code MISSILE_STACK_CAPABILITY} for this; the lookup is
     * the 26.3 counterpart and is required by {@code ICBMClassicHelpers#isMissile(ItemStack)} and
     * {@link IMissileHolder}.
     */
    public static final ItemApiLookup<ICapabilityMissileStack, Void> MISSILE_STACK =
            ItemApiLookup.get(ICBMConstants.id("missile_stack"), ICapabilityMissileStack.class, Void.class);

    private IcbmLookups()
    {
    }

    /**
     * Called from {@code ICBMClassic.onInitialize()} to force class loading of the lookups (the static
     * fields register the ids), register the vanilla EMP receivers and load the data attachments.
     */
    public static void init()
    {
        ModAttachments.init();
        CapabilityEMP.register();
    }
}
