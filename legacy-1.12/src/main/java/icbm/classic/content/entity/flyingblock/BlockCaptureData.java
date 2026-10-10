package icbm.classic.content.entity.flyingblock;

import icbm.classic.lib.saving.NbtSaveHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

@Data
@NoArgsConstructor
public class BlockCaptureData implements INBTSerializable<NBTTagCompound> {
    //TODO maybe replace with BlockSnapshot?

    /** Pulled from {@link World#getBlockState(BlockPos)} */
    private IBlockState blockState;
    /** Pulled from {@link net.minecraft.block.Block#getItem(World, BlockPos, IBlockState)} */
    private ItemStack sourceStack;
    /** Saved tile data */
    private NBTTagCompound tileEntityData;

    public BlockCaptureData(IBlockState state, ItemStack stack) {
        this.blockState = state;
        this.sourceStack = stack;
    }

    public BlockCaptureData(World world, BlockPos blockPos) {
        this.blockState = world.getBlockState(blockPos);
        this.sourceStack = blockState.getBlock().getItem(world, blockPos, blockState);

        final TileEntity tileEntity = world.getTileEntity(blockPos); //TODO add config to disable this for specific blocks
        if(tileEntity != null) {
            this.tileEntityData = new NBTTagCompound();
            tileEntity.writeToNBT(this.tileEntityData);
        }
    }

    public IBlockState getBlockState() {
        if(blockState == null) {
            this.blockState = Blocks.STONE.getDefaultState();
        }
        return blockState;
    }

    public ItemStack getSourceStack() {
        if(this.sourceStack == null) {
            this.sourceStack = new ItemStack(Blocks.STONE);
        }
        return sourceStack;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return SAVE_LOGIC.save(this);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        SAVE_LOGIC.load(this, nbt);
    }

    private static final NbtSaveHandler<BlockCaptureData> SAVE_LOGIC = new NbtSaveHandler<BlockCaptureData>()
        //Stuck in ground data
        .mainRoot()
        .nodeBlockState("block_state", BlockCaptureData::getBlockState, BlockCaptureData::setBlockState)
        .nodeItemStack("source_stack", BlockCaptureData::getSourceStack, BlockCaptureData::setSourceStack)
        .nodeCompoundTag("block_entity", BlockCaptureData::getTileEntityData, BlockCaptureData::setTileEntityData)
        .base();
}
