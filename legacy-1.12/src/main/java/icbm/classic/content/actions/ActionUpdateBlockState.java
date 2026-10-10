package icbm.classic.content.actions;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class ActionUpdateBlockState extends ActionBase implements IAction {
    private final Function<IBlockState, Boolean> validation; //TODO add ActionField
    private final Function<IBlockState, IBlockState> modifyBlock;

    public ActionUpdateBlockState(World world, BlockPos pos,
                                  Function<IBlockState, Boolean> validation, Function<IBlockState, IBlockState> modifyBlock,
                                  IActionSource source, IActionData actionData) {
        super(world, new Vec3d(pos), source, actionData);
        this.validation = validation;
        this.modifyBlock = modifyBlock;
    }

    @Nonnull
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
        final IBlockState currentState = getWorld().getBlockState(pos);
        // Ensure we are still the target block, this may be checking exact state or single property
        if(validation.apply(currentState)) {
            final IBlockState newState = modifyBlock.apply(currentState);
            if(newState != currentState && !getWorld().setBlockState(pos, newState)) {
                return ActionResponses.BLOCK_PLACEMENT_FAILED;
            }
            return ActionResponses.COMPLETED;
        }
        return ActionResponses.VALIDATION_ERROR;
    }
}
