package icbm.classic.content.abm;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public final class EngagementRegistry
{
    /** Filled in S7.05 with per-world engagement tracking. */
    public static void markIntercepted(ServerLevel level, UUID targetId) { }

    private EngagementRegistry() { }
}
