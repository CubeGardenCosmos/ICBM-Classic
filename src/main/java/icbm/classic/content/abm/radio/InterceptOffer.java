package icbm.classic.content.abm.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record InterceptOffer(BlockPos launcherPos, Vec3 launchPoint, double abmSpeed, int readyInTicks)
{
}
