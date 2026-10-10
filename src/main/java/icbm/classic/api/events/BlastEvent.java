package icbm.classic.api.events;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.explosion.IBlast;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Data carried by every blast themed event.
 * <p>
 * 1.12 extended the Forge {@code Event} class and was posted through {@code MinecraftForge.EVENT_BUS}.
 * 26.3 uses Fabric events and no longer needs the Forge inheritance: this class is now only the data
 * carrier, and the concrete blast events ({@link BlastBuildEvent}, {@link EmpEvent}) expose the Fabric
 * {@code Event} plus a {@code Callback} that receives the data object.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/3/19.
 *
 * @deprecated will be replaced with an event providing {@link icbm.classic.api.actions.IAction}
 */
@Deprecated
public abstract class BlastEvent<B extends IBlast>
{
    /** Source of the event */
    public final IAction blast;

    public BlastEvent(IAction blast)
    {
        this.blast = blast;
    }

    /**
     * Source of the blast.
     */
    public Level world()
    {
        return blast.getWorld();
    }

    /**
     * Source of the blast.
     */
    public double x()
    {
        return blast.getPosition().x;
    }

    /**
     * Source of the blast.
     */
    public double y()
    {
        return blast.getPosition().y;
    }

    /**
     * Source of the blast.
     */
    public double z()
    {
        return blast.getPosition().z;
    }

    /**
     * Source of the blast.
     * <p>
     * Normally a Missile, Grenade, or Minecraft
     *
     * @return entity, can be null in some cases
     */
    public Entity getSourceEntity()
    {
        if(blast instanceof IBlast) {
            return ((IBlast) blast).getBlastSource();
        }
        return null;
    }
}
