package icbm.classic.content.entity.flyingblock;

import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Snapshot of a block taken before it is turned into a flying block, used to put the block back when the
 * entity lands.
 *
 * <p>26.3 port notes (porting-guide.md §2.1/§4):
 * <ul>
 *     <li>1.12 Forge nbt-serializable → {@link ISavable} with
 *     {@link ValueInput}/{@link ValueOutput}.</li>
 *     <li>1.12 block state → {@link BlockState}.</li>
 *     <li>{@code Block#getItem(World, BlockPos, state)} → {@link Block#asItem()}: the 1.12 default
 *     returned the block's item without any state data, so the captured state stays out of the stack.</li>
 *     <li>1.12 tile entity write → {@link BlockEntity#saveWithId(ValueOutput)}: the 1.12 write added
 *     the {@code id} (compared by {@link EntityFlyingBlock#tryPlacement(BlockPos, net.minecraft.world.phys.BlockHitResult)})
 *     but no coordinates.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class BlockCaptureData implements ISavable {
    //TODO maybe replace with BlockSnapshot?

    /** Pulled from {@link Level#getBlockState(BlockPos)} */
    private BlockState blockState;
    /** Pulled from {@link Block#asItem()} */
    private ItemStack sourceStack;
    /** Saved block entity data, 1.12 named this field {@code tileEntityData} */
    private CompoundTag blockEntityData;

    public BlockCaptureData(BlockState state, ItemStack stack) {
        this.blockState = state;
        this.sourceStack = stack;
    }

    public BlockCaptureData(Level world, BlockPos blockPos) {
        this.blockState = world.getBlockState(blockPos);
        this.sourceStack = new ItemStack(this.blockState.getBlock().asItem());

        final BlockEntity blockEntity = world.getBlockEntity(blockPos); //TODO add config to disable this for specific blocks
        if (blockEntity != null) {
            final TagValueOutput save = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
            blockEntity.saveWithId(save);
            this.blockEntityData = save.buildResult();
        }
    }

    public BlockState getBlockState() {
        if (blockState == null) {
            this.blockState = Blocks.STONE.defaultBlockState();
        }
        return blockState;
    }

    public ItemStack getSourceStack() {
        if (this.sourceStack == null) {
            this.sourceStack = new ItemStack(Blocks.STONE);
        }
        return sourceStack;
    }

    @Override
    public void save(ValueOutput out) {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<BlockCaptureData> SAVE_LOGIC = new SaveHandler<BlockCaptureData>()
        //Stuck in ground data
        .mainRoot()
        .nodeBlockState("block_state", BlockCaptureData::getBlockState, BlockCaptureData::setBlockState)
        .nodeItemStack("source_stack", BlockCaptureData::getSourceStack, BlockCaptureData::setSourceStack)
        .nodeCompoundTag("block_entity", BlockCaptureData::getBlockEntityData, BlockCaptureData::setBlockEntityData)
        .base();
}
