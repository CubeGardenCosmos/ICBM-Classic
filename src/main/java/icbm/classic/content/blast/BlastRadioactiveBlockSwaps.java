package icbm.classic.content.blast;

import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.content.blast.threaded.BlastThreaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles switching out blocks for dead version or with radioactive versions
 * <p>
 * Port notes (porting-guide.md §2, docs/architecture/overview.md §6):
 * <ul>
 *     <li>1.12 read the live world from the blast thread, the port goes through {@link BlastThreaded}: the
 *     worker only collects positions, the block states are read in {@link #destroyBlock(BlockPos)} on the
 *     server thread and the base class applies them through the block edit queue.</li>
 *     <li>1.12 {@code RadioactiveHandler.radioactiveBlockSwaps.getValue(blockState)} looked the replacement up
 *     in a block state table owned by the radioactive handler, which is added by S3.08. Until then
 *     {@link #getReplacement(BlockState)} resolves the two radioactive block ids directly.</li>
 * </ul>
 */
public class BlastRadioactiveBlockSwaps extends BlastThreaded implements IBlastTickable
{
    /** 1.12 {@code RadioactiveHandler.radioactiveBlockSwaps} used {@code setChance(0.2f)} for the swap rows */
    protected static final float SWAP_CHANCE = 0.2F;

    /** 1.12 {@code BlockReg.blockRadioactive} with {@code EnumType.STONE}, see docs/architecture/registry-ids.md */
    protected static final Identifier RADIOACTIVE_STONE_ID = ICBMConstants.id("radioactive_stone");
    /** 1.12 {@code BlockReg.blockRadioactive} with {@code EnumType.DIRT}, see docs/architecture/registry-ids.md */
    protected static final Identifier RADIOACTIVE_DIRT_ID = ICBMConstants.id("radioactive_dirt");

    /** Stops the "block is not registered" report from repeating for every block of the blast */
    private boolean reportedMissingReplacement = false;

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
     * 1.12 filled the table with stone -&gt; radioactive stone, dirt/farmland/grass path/grass/mycelium -&gt;
     * radioactive dirt (chance 0.2F), every plant/leaf/vine block -&gt; air, and the extra rows of
     * {@code ConfigBlast.nuclear.radiationReplacements} (legacy-1.12/src/main/java/icbm/classic/content/radioactive/RadioactiveHandler.java
     * lines 14-32).
     *
     * @param blockState state being checked
     * @return replacement to apply, null when the block is not in the table
     */
    protected BlockReplacementData getReplacement(BlockState blockState)
    {
        //TODO(S3.08) rebuild this table in RadioactiveHandler. Only the stone/dirt rows are resolved here, the
        //    plant/leaf/vine to air rows (1.12 material checks) and the config driven rows come with the handler.
        final Block block = blockState.getBlock();
        if (block == Blocks.STONE)
        {
            return createReplacement(RADIOACTIVE_STONE_ID);
        }
        else if (block == Blocks.DIRT || block == Blocks.FARMLAND || block == Blocks.DIRT_PATH || block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM)
        {
            return createReplacement(RADIOACTIVE_DIRT_ID);
        }
        return null;
    }

    /**
     * Builds a table row for the given block id, skipping the replacement when the block is not registered.
     *
     * @param id id of the block to place
     * @return replacement data, null when the block is missing
     */
    protected BlockReplacementData createReplacement(Identifier id)
    {
        final Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        if (block.isEmpty())
        {
            //The radioactive blocks are registered by S3.08, warn once per blast instead of once per block
            if (!reportedMissingReplacement)
            {
                reportedMissingReplacement = true;
                ICBMClassic.logger().warn("BlastRadioactiveBlockSwaps: block '{}' is not registered, skipping the radioactive replacement (added by S3.08)", id);
            }
            return null;
        }
        return new BlockReplacementData().setChance(SWAP_CHANCE).setBlockState(block.get().defaultBlockState());
    }
}
