package icbm.classic.content.blocks.launcher;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.launcher.IMissileLauncher;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.missiles.parts.IMissileTargetDelayed;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.tile.ITick;
import lombok.Data;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Consumer;

/** Stores firing information for countdowns/delays. */
@Data
public class FiringPackage implements ISavable, ITick {

    /** Input: Target data. */
    private IMissileTarget targetData;
    /** Input: Cause of firing the missile. */
    private IActionCause cause;
    /** Counter: time to tick down before firing. */
    private int countDown = -1;
    private boolean hasFired = false;
    private Consumer<IActionStatus> onTriggerCallback;

    public FiringPackage(IMissileTarget targetData, IActionCause cause, int countDown) {
        this.targetData = targetData;
        this.cause = cause;
        this.countDown = countDown;
    }

    public FiringPackage(IMissileTarget targetData, IActionCause cause) {
        this.targetData = targetData;
        this.cause = cause;
    }

    public void launch(IMissileLauncher missileLauncher) {
        if (!hasFired) {
            hasFired = true;
            if (targetData instanceof IMissileTargetDelayed delayed) {
                targetData = delayed.cloneWithoutDelay();
            }
            // Route through the standard launch path so validation and launcher behavior stay consistent.
            final IActionStatus status = missileLauncher.launch(launcher -> targetData, cause, false);
            if (onTriggerCallback != null) {
                onTriggerCallback.accept(status);
            }
        }
    }

    public boolean isReady() {
        return !hasFired;
    }

    @Override
    public void save(ValueOutput out) {
        out.putInt("countdown", countDown);
        if (targetData != null) {
            ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY.save(targetData, out.child("target"));
        }
        if (cause != null) {
            ICBMClassicAPI.ACTION_CAUSE_REGISTRY.save(cause, out.child("cause"));
        }
    }

    @Override
    public void load(ValueInput in) {
        countDown = in.getIntOr("countdown", -1);
        targetData = ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY.load(in.childOrEmpty("target"));
        cause = ICBMClassicAPI.ACTION_CAUSE_REGISTRY.load(in.childOrEmpty("cause"));
    }

    @Override
    public void update(int tick, boolean isServer) {
        if (isServer && !hasFired) {
            countDown--;
        }
    }
}
