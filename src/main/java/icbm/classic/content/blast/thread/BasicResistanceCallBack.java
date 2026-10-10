package icbm.classic.content.blast.thread;

import com.builtbroken.jlib.data.vector.IPos3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;

/**
 * Default resistance callback: liquids are easy to destroy, everything else uses its own explosion
 * resistance.
 *
 * <p>1.12 held the owning {@code Blast} to pass it into the 1.12 explosion resistance lookup and checked
 * Forge's {@code IFluidBlock}. 26.3 {@code Block#getExplosionResistance()}
 * takes no arguments, so the callback no longer needs state and stays safe to run on a worker thread.
 * {@code IFluidBlock} has no Fabric equivalent; modded fluid blocks extend {@link LiquidBlock}, so the
 * 0.25 resistance still applies to them.</p>
 *
 * @deprecated 1.12 API, follow-up blasts run off a {@code LevelSnapshot}
 */
@Deprecated
public class BasicResistanceCallBack implements IThreadCallBack
{
    @Override
    public float getResistance(Level level, IPos3D blastCenter, BlockPos pos, Entity source, Block block)
    {
        if (block instanceof LiquidBlock)
        {
            return 0.25f;
        }
        else
        {
            return block.getExplosionResistance();
        }
    }
}
