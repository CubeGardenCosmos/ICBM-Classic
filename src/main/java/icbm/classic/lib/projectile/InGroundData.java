package icbm.classic.lib.projectile;

import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stores data about position, side, and block projectile is inside
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InGroundData implements ISavable {
    /**
     * Block position projectile is stuck inside
     */
    private BlockPos pos;

    /**
     * Face of tile we are stuck inside
     */
    private Direction side;

    /**
     * Block state we are stuck inside
     */
    private BlockState state;

    public InGroundData(Level world, BlockHitResult hit) {
        this.pos = hit.getBlockPos();
        this.side = hit.getDirection();
        this.state = world.getBlockState(pos);
    }

    // 1.12 exposed getMaterial() here. 26.3 removed the Material system (porting-guide.md §2.1), callers
    // check the state with BlockState#isAir instead.

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<InGroundData> SAVE_LOGIC = new SaveHandler<InGroundData>()
        .mainRoot()
        .nodeBlockPos("pos", InGroundData::getPos, InGroundData::setPos)
        .nodeDirection("side", InGroundData::getSide, InGroundData::setSide)
        .nodeBlockState("state", InGroundData::getState, InGroundData::setState)
        .base();
}
