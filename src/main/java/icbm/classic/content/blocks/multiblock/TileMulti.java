package icbm.classic.content.blocks.multiblock;

import icbm.classic.lib.NBTConstants;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Basic implementation of a multi block, 1.12 {@code icbm.classic.content.blocks.multiblock.TileMulti} ported
 * onto {@link BlockEntity}.
 * <p>
 * 1.12 stored the position of the master block in {@code NBTConstants.HOST_POS} as an int array, which is kept
 * (ADR 0002: no save migration, but the key names stay for comparison). 26.3 uses {@link BlockPos} plus
 * {@link ValueInput}/{@link ValueOutput} instead of raw NBT access.
 * <p>
 * 1.12's {@code onLoad} only carried a "TODO place blocks" comment and {@link BlockEntity} has no
 * {@code onLoad} counterpart, so the port has no placeholder for it. The 1.12 field was private without any
 * accessor; {@link Getter}/{@link Setter} are added so the fake blocks that delegate to the master block
 * (S5.02/S5.06) can use it.
 * <p>
 * Created by Dark on 8/9/2015.
 */
@Deprecated
public class TileMulti extends BlockEntity
{
    @Getter
    @Setter
    private BlockPos hostPosition = null;

    public TileMulti(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in)
    {
        super.loadAdditional(in);
        //1.12: compound.hasKey(HOST_POS) -> compound.getIntArray(HOST_POS)
        in.getIntArray(NBTConstants.HOST_POS).ifPresent(data -> {
            if (data.length >= 3)
            {
                hostPosition = new BlockPos(data[0], data[1], data[2]);
            }
        });
    }

    @Override
    protected void saveAdditional(ValueOutput out)
    {
        if (hostPosition != null)
        {
            out.putIntArray(NBTConstants.HOST_POS, new int[]{hostPosition.getX(), hostPosition.getY(), hostPosition.getZ()});
        }
        super.saveAdditional(out);
    }
}
