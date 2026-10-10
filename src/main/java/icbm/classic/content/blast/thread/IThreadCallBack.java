package icbm.classic.content.blast.thread;

import com.builtbroken.jlib.data.vector.IPos3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Callback used by threaded explosions to look up the resistance of a block.
 *
 * <p>1.12 ran this on a blast thread and passed the live world. In 26.3 the call happens on a
 * {@code WorkerThread}, so the level argument is always {@code null} and implementations MUST NOT use it
 * (docs/architecture/overview.md §6). It is kept in the signature so the 1.12 call sites keep compiling and
 * to make the missing world data explicit at every call site.</p>
 *
 * @deprecated 1.12 API, follow-up blasts run off a {@code LevelSnapshot}
 */
@Deprecated
public interface IThreadCallBack
{
    /**
     * @param level      legacy world argument, null when called from a worker thread
     * @param blastCenter center of the explosion
     * @param pos        position being checked
     * @param source     entity that caused the explosion, may be null
     * @param block      block being checked
     * @return resistance of the block, higher values survive the blast
     */
    float getResistance(Level level, IPos3D blastCenter, BlockPos pos, Entity source, Block block);
}
