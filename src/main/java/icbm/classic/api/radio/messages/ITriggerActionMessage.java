package icbm.classic.api.radio.messages;

import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioTargetAssignable;

/**
 * Packet used to trigger an action
 * @deprecated replace with {@link icbm.classic.api.actions.data.IActionFieldProvider} to avoid
 * needing different interfaces for each version. When we can just pass fields.
 */
public interface ITriggerActionMessage extends IRadioMessage {

    /**
     * Callback from the launch event
     *
     * <p>26.3 port note: 1.12 passed an {@code IActionStatus} here. The action system (and with it
     * {@code IActionStatus}) is ported in S1.08, which is after this step. Until the two are aligned
     * (S1.08/S7) the callback receives the target assignable that the launch result points at, so
     * radio messages do not depend on the not yet ported action status type.</p>
     *
     * @param target to assign from invoking the launcher, or null if none
     */
    default void onTriggerCallback(IRadioTargetAssignable target) {

    }

    /**
     * Should the action be triggered. Some
     * messages may be multipurpose and not always
     * want to trigger the action.
     *
     * @return true if should trigger
     */
    default boolean shouldTrigger() {
        return true;
    }
}
