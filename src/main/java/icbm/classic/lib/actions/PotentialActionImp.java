package icbm.classic.lib.actions;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.conditions.IConditionCause;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.status.ActionStatusTypes;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import icbm.classic.lib.tile.ITick;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Pre-built action for general purpose implementation where all components are known.
 */
public abstract class PotentialActionImp<SELF extends PotentialActionImp<SELF>> implements IPotentialAction, ITick, ISavable {

    @Getter @Setter(AccessLevel.PRIVATE)
    private ICondition preCheck;

    @Getter @Setter
    private IActionFieldProvider fieldProvider;

    public SELF withCondition(ICondition check) {
        this.preCheck = check;
        if(check != null) {
            check.init(fieldProvider);
        }
        return (SELF) this;
    }

    public SELF withProvider(IActionFieldProvider provider) {
        this.fieldProvider = provider;
        if(preCheck != null) {
            preCheck.init(fieldProvider);
        }
        return (SELF) this;
    }

    @NonNull
    @Override
    public IActionStatus checkAction(Level world, double x, double y, double z, @Nullable IActionCause cause) {
        // Edge case: lazy init failed to get action data
        if(this.getActionData() == null) {
            return ActionResponses.MISSING_ACTION_DATA;
        }
        return preCheck == null ? ActionResponses.READY : preCheck.getCondition();
    }

    public IActionStatus doAction(BlockEntity tile, @Nullable IActionCause cause) {
        final Level level = tile.getLevel();
        final BlockPos pos = tile.getBlockPos();
        final IActionCause selfCause = new CausedByBlock(level, pos, level.getBlockState(pos)).setPreviousCause(cause);
        return doAction(level, pos, selfCause);
    }

    public IActionStatus doAction(Level world, BlockPos pos, @Nullable IActionCause cause) {
        return doAction(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, cause);
    }

    @NonNull
    @Override
    public IActionStatus doAction(Level world, double x, double y, double z, @Nullable IActionCause cause) {
        final IActionData actionData = this.getActionData();

        // Edge case: lazy init failed to get action data
        if(actionData == null) {
            return ActionResponses.MISSING_ACTION_DATA;
        }

        final IActionStatus preCheckStatus = checkAction(world, x, y, z, cause); //TODO if preCheck is a special type use in cause-by chain. Example: timer
        if(preCheckStatus.isType(ActionStatusTypes.BLOCKING)) {
            return preCheckStatus;
        }

        IActionCause causeToUse = cause;
        if(preCheck != null) {
            if(preCheck instanceof IConditionCause) {
                final IActionCause preCheckCause = ((IConditionCause) preCheck).getCause();
                if(preCheckCause != null) {
                    causeToUse = preCheckCause.setPreviousCause(cause);
                }
            }
            preCheck.reset();
        }

        final IActionSource actionSource = new ActionSource(world, new Vec3(x, y, z), causeToUse);
        return ICBMClassicAPI.ACTION_LISTENER.runAction(actionData.create(world, x, y, z, actionSource, this.fieldProvider));
    }

    @Override
    public void update(int tick, boolean isServer) {
        if(isServer && preCheck != null) {
            preCheck.onTick();
        }
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<PotentialActionImp> SAVE_LOGIC = new SaveHandler<PotentialActionImp>()
        .mainRoot()
        /* */.nodeBuildableObject("pre_check", () -> ICBMClassicAPI.CONDITION_REGISTRY, PotentialActionImp::getPreCheck, PotentialActionImp::setPreCheck)
        .base();
}
