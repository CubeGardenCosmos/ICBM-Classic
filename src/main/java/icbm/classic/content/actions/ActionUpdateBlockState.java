package icbm.classic.content.actions;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import java.util.function.Function;

public class ActionUpdateBlockState extends ActionBase implements IAction {
    private final Function<BlockState, Boolean> validation; //TODO add ActionField
    private final Function<BlockState, BlockState> modifyBlock;

    public ActionUpdateBlockState(Level world, BlockPos pos,
                                  Function<BlockState, Boolean> validation, Function<BlockState, BlockState> modifyBlock,
                                  IActionSource source, IActionData actionData) {
        super(world, new Vec3(pos), source, actionData);
        this.validation = validation;
        this.modifyBlock = modifyBlock;
    }

    @NonNull
    @Override
    public IActionStatus doAction() {
        // Check we have requirements
        if(modifyBlock == null) {
            return ActionResponses.MISSING_BLOCK_STATE;
        }
        else if(validation == null) {
            return ActionResponses.MISSING_VALIDATION;
        }

        final BlockPos pos = this.getBlockPos();
        final BlockState currentState = getWorld().getBlockState(pos);
        // Ensure we are still the target block, this may be checking exact state or single property
        if(validation.apply(currentState)) {
            final BlockState newState = modifyBlock.apply(currentState);
            if(newState != currentState && !getWorld().setBlock(pos, newState, 3)) {
                return ActionResponses.BLOCK_PLACEMENT_FAILED;
            }
            return ActionResponses.COMPLETED;
        }
        return ActionResponses.VALIDATION_ERROR;
    }
}
