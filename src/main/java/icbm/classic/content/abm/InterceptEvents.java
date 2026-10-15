package icbm.classic.content.abm;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class InterceptEvents
{
    @FunctionalInterface public interface Intercepted { boolean onIntercept(ServerLevel level, EntityAntiBallisticMissile abm, Entity target, Vec3 point); }
    @FunctionalInterface public interface AbmLaunched { void onLaunched(ServerLevel level, EntityAntiBallisticMissile abm, UUID targetId, BlockPos launcherPos); }
    public static final Event<Intercepted> INTERCEPTED = EventFactory.createArrayBacked(Intercepted.class,
            ls -> (lvl, abm, t, p) -> { for (Intercepted l : ls) if (!l.onIntercept(lvl, abm, t, p)) return false; return true; });
    public static final Event<AbmLaunched> ABM_LAUNCHED = EventFactory.createArrayBacked(AbmLaunched.class,
            ls -> (lvl, abm, id, pos) -> { for (AbmLaunched l : ls) l.onLaunched(lvl, abm, id, pos); });

    private InterceptEvents() { }
}
