package icbm.classic.content.missile.logic.source.cause;

import icbm.classic.ICBMConstants;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RedstoneCause extends CausedByBlock {

    public static final Identifier REG_NAME = ICBMConstants.id("block.redstone");

    private Direction side;

    public RedstoneCause(Level world, BlockPos pos, BlockState state, Direction side) {
        super(world, pos, state);
        this.side = side;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<RedstoneCause> SAVE_LOGIC = new SaveHandler<RedstoneCause>()
        .mainRoot()
        /* */.nodeDirection("side", RedstoneCause::getSide, RedstoneCause::setSide)
        .base();
}
