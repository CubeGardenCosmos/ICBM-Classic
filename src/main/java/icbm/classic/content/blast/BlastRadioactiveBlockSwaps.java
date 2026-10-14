package icbm.classic.content.blast;

import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.content.blast.threaded.BlastThreaded;
import icbm.classic.content.radioactive.RadioactiveHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Handles switching out blocks for dead version or with radioactive versions
 * <p>
 * Port notes (porting-guide.md §2, docs/architecture/overview.md §6):
 * <ul>
 *     <li>1.12 read the live world from the blast thread, the port goes through {@link BlastThreaded}: the
 *     worker only collects positions, the block states are read in {@link #destroyBlock(BlockPos)} on the
 *     server thread and the base class applies them through the block edit queue.</li>
 *     <li>the replacement table is the 1.12 {@code RadioactiveHandler.radioactiveBlockSwaps}, rebuilt on the
 *     26.3 block registry by {@link RadioactiveHandler} (S3.08). {@code RadioactiveHandler#setup()} has to
 *     run before this blast, otherwise the table is empty and nothing is swapped.</li>
 * </ul>
 */
public class BlastRadioactiveBlockSwaps extends BlastThreaded implements IBlastTickable
{
    @Override
    public boolean doRun(int loops, Consumer<BlockPos> edits)
    {
        BlastHelpers.forEachPosInRadius(this.getBlastRadius(), (x, y, z) ->
                edits.accept(new BlockPos(xi() + x, yi() + y, zi() + z)));

        //TODO implement raytrace to reduce impact of effects behind protective shielding
        //TODO only contaminate top few layers of earth
        return false;
    }

    @Override
    public void destroyBlock(BlockPos targetPosition)
    {
        final BlockState blockState = world().getBlockState(targetPosition); //1.12: world.getBlockState(targetPosition)
        final BlockReplacementData replacement = getReplacement(blockState); //1.12: RadioactiveHandler.radioactiveBlockSwaps.getValue(blockState)
        if(replacement != null) {
            replacement.apply(world(), targetPosition);
        }
    }

    /**
     * Replacement for the given block state, 1.12 {@code RadioactiveHandler.radioactiveBlockSwaps.getValue(state)}.
     * <p>
     * The table (stone -&gt; radioactive stone, dirt/farmland/grass path/grass/mycelium -&gt; radioactive dirt
     * with chance 0.2F, every plant/leaf/vine block -&gt; air and the extra rows of
     * {@code ConfigBlast.nuclear.radiationReplacements}) lives in {@link RadioactiveHandler#radioactiveBlockSwaps}
     * (legacy-1.12/src/main/java/icbm/classic/content/radioactive/RadioactiveHandler.java lines 14-32).
     *
     * @param blockState state being checked
     * @return replacement to apply, null when the block is not in the table
     */
    protected BlockReplacementData getReplacement(BlockState blockState)
    {
        return RadioactiveHandler.radioactiveBlockSwaps.getValue(blockState);
    }
}
