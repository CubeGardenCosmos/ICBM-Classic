package icbm.classic.api;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.listener.IActionListenerHandler;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveRegistry;
import icbm.classic.api.reg.content.IExBlockRegistry;
import icbm.classic.api.reg.content.IExGrenadeRegistry;
import icbm.classic.api.reg.content.IExMinecartRegistry;
import icbm.classic.api.reg.content.IExMissileRegistry;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import net.minecraft.resources.Identifier;

/**
 * API reference class for ICBM-Classic mod
 * <p>
 * S1.08 added the action system entry points and S1.10 added the remaining registries and the
 * explosive references. The 1.12 capability injector fields are gone: every capability now maps
 * to an entry of {@link icbm.classic.lib.lookup.IcbmLookups} (missile launcher, missile holder, EMP
 * receivers and missile) or to a type that is implemented/read directly
 * ({@link icbm.classic.api.caps.IExplosive}, {@code icbmclassic:gps_data}). Use
 * {@link ICBMClassicHelpers} or {@code IcbmLookups} to query them - see
 * {@code docs/architecture/porting-guide.md} §2.6.
 */
public final class ICBMClassicAPI {

    //=========================
    //=== Registry ============
    //=========================

    /** Registry for target data save/load in missiles */
    public static IBuilderRegistry<IMissileTarget> MISSILE_TARGET_DATA_REGISTRY; // TODO(S4.01) assigned by the missile system
    /** Registry for flight logic save/load in missiles */
    public static IBuilderRegistry<IMissileFlightLogic> MISSILE_FLIGHT_LOGIC_REGISTRY; // TODO(S4.01) assigned by the missile system

    /** Registry for conditional logic, often used for actions */
    public static IBuilderRegistry<ICondition> CONDITION_REGISTRY;

    /** Registry for actions that can be run */
    public static IBuilderRegistry<IActionData> ACTION_REGISTRY;
    /** Registry for actions with conditionals that may be run */
    public static IBuilderRegistry<IPotentialAction> ACTION_POTENTIAL_REGISTRY;
    /** Registry for status messages produced by machines, items, and entities in the mod */
    public static IBuilderRegistry<IActionStatus> ACTION_STATUS_REGISTRY;
    /** Registry for missile cause save/load in missiles */
    public static IBuilderRegistry<IActionCause> ACTION_CAUSE_REGISTRY;
    /** Event system for actions */
    public static IActionListenerHandler ACTION_LISTENER;

    /** Registry for projectile information */
    public static IProjectileDataRegistry PROJECTILE_DATA_REGISTRY; // assigned by ICBMClassic#onInitialize (S3.01)

    //=========================
    //=== References ==========
    //=========================
    public static final String ID = "icbmclassic";

    //=========================
    //=== Capabilities ========
    //=========================
    // 1.12 declared capability injector fields here (EMP, missile, missile holder, launcher, radio, GPS,
    // projectile stack, explosive, missile stack, blast velocity, blast). All of them are replaced:
    // - IMissile             -> icbm.classic.lib.lookup.IcbmLookups.MISSILE
    // - IMissileHolder       -> IcbmLookups.MISSILE_HOLDER
    // - IMissileLauncher     -> IcbmLookups.MISSILE_LAUNCHER
    // - IEMPReceiver         -> IcbmLookups.EMP_RECEIVER_BLOCK/ENTITY/ITEM
    // - ICapabilityMissileStack -> IcbmLookups.MISSILE_STACK
    // - IExplosive           -> implemented by the entity/block entity, queried with ICBMClassicHelpers
    // - IGPSData             -> icbmclassic:gps_data data component (S2.04/S5.08)
    // - IRadio               -> implemented by the radio host directly
    // - IProjectileStack     -> items expose their projectile data directly (S2.02/S4)
    // - IBlast / IBlastVelocity -> blasts are IAction and no longer capabilities

    //=========================
    //=== Will be removed =====
    //=========================
    /**
     * Main handler for explosives, do not override as this will break the mod
     * @deprecated replaced with {@link #ACTION_REGISTRY}
     */
    @Deprecated
    public static IExplosiveRegistry EXPLOSIVE_REGISTRY; // TODO(S2.02) assigned by ExplosiveRegistry

    @Deprecated
    public static IExMissileRegistry EX_MISSILE_REGISTRY; // TODO(S2.02)
    @Deprecated
    public static IExGrenadeRegistry EX_GRENADE_REGISTRY; // TODO(S2.02)
    @Deprecated
    public static IExBlockRegistry EX_BLOCK_REGISTRY; // TODO(S2.02)
    @Deprecated
    public static IExMinecartRegistry EX_MINECART_REGISTRY; // TODO(S2.02)

    /** Registry for explosive customizations
     * @deprecated replaced with {@link icbm.classic.api.actions.data.IActionFieldProvider}
     * */
    @Deprecated
    public static IBuilderRegistry<IExplosiveCustomization> EXPLOSIVE_CUSTOMIZATION_REGISTRY; // TODO(S3.07) assigned with the ender blast customization

    @Deprecated
    public static final Identifier EX_MISSILE = ICBMConstants.id("missile");
    @Deprecated
    public static final Identifier EX_GRENADE = ICBMConstants.id("grenade");
    @Deprecated
    public static final Identifier EX_BLOCK = ICBMConstants.id("block");
    @Deprecated
    public static final Identifier EX_MINECART = ICBMConstants.id("minecart");
}
