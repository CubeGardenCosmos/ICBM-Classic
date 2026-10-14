package icbm.classic.content.blast.redmatter.caps;

import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.lib.actions.status.ActionResponses;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper for exposing the {@link EntityRedmatter} as a Blast
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>1.12 published this through the blast capability of the redmatter entity, that capability system is
 *     gone so the wrapper is read straight off {@code EntityRedmatter#blastData} by the code that needs it.</li>
 *     <li>{@code host.world} is {@code Entity#level()}, {@code host.posX/Y/Z} are {@code Entity#getX/Y/Z},
 *     {@code setDead()} is {@code Entity#discard()} and {@code isDead} is {@code Entity#isRemoved()}.</li>
 *     <li>The 1.12 source built an entity cause from the host. That cause type is ported in S4, so this keeps
 *     a null cause with the same TODO as {@code EntityExplosion#constructBlast}.</li>
 * </ul>
 * Created by Dark(DarkGuardsman, Robin) on 4/19/2020.
 */
@Deprecated //TODO rework so we don't need a blast wrapper as this is an entity that should be spawned using IAction not IBlast
public class BlastRedmatterWrapper implements IBlast
{
    private final EntityRedmatter host;

    public BlastRedmatterWrapper(EntityRedmatter host)
    {
        this.host = host;
    }

    @NonNull
    @Override
    public IActionStatus doAction()
    {
        return ActionResponses.COMPLETED;
    }

    @NonNull
    @Override
    public IActionSource getSource()
    {
        //TODO(S4) use the ported entity cause once content/missile/logic/source/cause is ported, the 1.12
        // call was new ActionSource(host.world, host position, cause(host))
        return new ActionSource(host.level(), host.position(), null);
    }

    @Override
    public void clearBlast()
    {
        host.discard(); //1.12: host.setDead()
    }

    //<editor-fold desc="properties">
    @Override
    public boolean isCompleted()
    {
        return host.isRemoved(); //1.12: host.isDead
    }

    @NonNull
    @Override
    public IExplosiveData getActionData()
    {
        return ICBMExplosives.REDMATTER;
    }

    @Override
    @Nullable
    public Entity getEntity()
    {
        return host;
    }

    @Override
    @Nullable
    public Entity getBlastSource()
    {
        return host;
    }
    //</editor-fold>

    //<editor-fold desc="position-data">
    @Override
    public Level world()
    {
        return host.level(); //1.12: host.world
    }

    @Override
    public double z()
    {
        return host.getZ(); //1.12: host.posZ
    }

    @Override
    public double x()
    {
        return host.getX(); //1.12: host.posX
    }

    @Override
    public double y()
    {
        return host.getY(); //1.12: host.posY
    }
    //</editor-fold>
}
