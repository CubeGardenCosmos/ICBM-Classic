package icbm.classic.content.blocks.emptower;

import icbm.classic.content.reg.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TileEmpTowerFake extends BlockEntity
{
    @Nullable private BlockPos hostPosition;

    public TileEmpTowerFake(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.EMP_TOWER_FAKE, pos, state);
    }

    public void setHost(@Nullable TileEMPTower tower)
    {
        final BlockPos nextHost = tower == null ? null : tower.getBlockPos();
        if (!java.util.Objects.equals(hostPosition, nextHost))
        {
            hostPosition = nextHost;
            setChanged();
        }
    }

    @Nullable
    public TileEMPTower getHost()
    {
        if (hostPosition == null || level == null || !level.isLoaded(hostPosition)) return null;
        final BlockEntity entity = level.getBlockEntity(hostPosition);
        return entity instanceof TileEMPTower tile ? tile : null;
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);
        if (hostPosition != null) output.store("host_pos", BlockPos.CODEC, hostPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        hostPosition = input.read("host_pos", BlockPos.CODEC).orElse(null);
    }
}
