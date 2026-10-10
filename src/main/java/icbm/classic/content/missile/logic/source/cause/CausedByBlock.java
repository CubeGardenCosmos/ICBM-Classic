package icbm.classic.content.missile.logic.source.cause;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.ICausedByBlock;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.NonNull;

/**
 * General purpose block cause
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CausedByBlock extends ActionCause implements ICausedByBlock {

    public static final Identifier REG_NAME = ICBMConstants.id("block");

    private Level world;
    private BlockPos blockPos;
    private BlockState blockState;

    /** Dimension the cause was created in. 1.12 stored the integer level id and resolved the level lazily. */
    private Identifier dimensionKey;

    public CausedByBlock(Level world, BlockPos pos, BlockState state) {
        this.world = world;
        this.dimensionKey = world != null ? world.dimension().identifier() : null;
        this.blockPos = pos;
        this.blockState = state;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @NonNull
    @Override
    public IBuilderRegistry<IActionCause> getRegistry() {
        return ICBMClassicAPI.ACTION_CAUSE_REGISTRY;
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

    private static final SaveHandler<CausedByBlock> SAVE_LOGIC = new SaveHandler<CausedByBlock>()
        .mainRoot()
        /* */.nodeIdentifier("level", CausedByBlock::getDimensionKey, CausedByBlock::setDimensionKey)
        /* */.nodeBlockPos("pos", CausedByBlock::getBlockPos, CausedByBlock::setBlockPos)
        /* */.nodeBlockState("state", CausedByBlock::getBlockState, CausedByBlock::setBlockState)
        .base();
}
