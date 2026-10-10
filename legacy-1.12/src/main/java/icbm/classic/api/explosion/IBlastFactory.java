package icbm.classic.api.explosion;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Simple interface for use in creating blasts
 *
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/3/19.
 * @deprecated will be moved internal
 */
@Deprecated
@FunctionalInterface
public interface IBlastFactory
{
    /**
     * Creates a new blast
     * @return new blast
     */
    @Nonnull
    IAction create(World world, double x, double y, double z, IActionSource source);
}
