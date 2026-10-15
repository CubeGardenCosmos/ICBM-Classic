package icbm.classic.content.abm;

import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class MissileKillHelper
{
    public static void detonate(IMissile missile, IActionSource source)
    {
        Entity entity = missile.getMissileEntity();
        if (entity != null) detonate(missile, source, entity.getX(), entity.getY(), entity.getZ());
    }

    /** Retains the EMP impact coordinates used by the original firing-circuit branch. */
    public static void detonate(IMissile missile, IActionSource source, double x, double y, double z)
    {
        Entity entity = missile.getMissileEntity();
        if (entity == null) return;
        Level level = entity.level();
        if (entity instanceof EntityExplosiveMissile explosiveMissile)
        {
            explosiveMissile.explosive.doExplosion(x, y, z, source);
        }
        else if (entity instanceof EntityMissileActionable actionableMissile)
        {
            actionableMissile.getMainAction().doAction(level, x, y, z, source != null ? source.getCause() : null);
        }
        else if (entity instanceof icbm.classic.api.radio.IRadioTargetAssignable)
        {
            level.explode(entity, x, y, z, ConfigBlast.emp.missiles.engineFullBlowStrength, Level.ExplosionInteraction.NONE);
        }
    }

    private MissileKillHelper() { }
}
