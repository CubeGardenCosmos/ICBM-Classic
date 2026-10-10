package icbm.classic.api;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.listener.IActionListenerHandler;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.reg.obj.IBuilderRegistry;

/**
 * API reference class for ICBM-Classic mod
 * <p>
 * Action system entry points (S1.08). The 1.12 capability injector fields are gone - see
 * {@code docs/architecture/porting-guide.md} §2.6.
 */
public final class ICBMClassicAPI {

    //=========================
    //=== Registry ============
    //=========================

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

    //=========================
    //=== References ==========
    //=========================
    public static final String ID = "icbmclassic";
}
