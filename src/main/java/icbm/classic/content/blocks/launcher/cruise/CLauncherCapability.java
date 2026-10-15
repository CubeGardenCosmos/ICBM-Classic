package icbm.classic.content.blocks.launcher.cruise;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.blocks.launcher.LauncherBaseCapability;
import icbm.classic.content.blocks.launcher.status.FiringWithDelay;
import icbm.classic.content.blocks.launcher.status.LaunchedWithMissile;
import icbm.classic.content.blocks.launcher.status.LauncherStatus;
import icbm.classic.content.missile.logic.flight.DirectFlightLogic;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.energy.EnergyRules;
import icbm.classic.lib.lookup.IcbmLookups;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Launch logic for the cruise launcher. */
public class CLauncherCapability extends LauncherBaseCapability
{
    private final TileCruiseLauncher host;

    public CLauncherCapability(TileCruiseLauncher host)
    {
        this.host = host;
    }

    public TileCruiseLauncher getHost()
    {
        return host;
    }

    @Override
    public IActionStatus getStatus()
    {
        if (!EnergyRules.ignoresPower() && !host.energyStorage.has(host.getFiringCost()))
        {
            return LauncherStatus.ERROR_POWER;
        }
        if (host.getFiringPackage() != null && !host.isAimed())
        {
            return LauncherStatus.FIRING_AIMING;
        }
        if (host.missileHolder.getMissileStack().isEmpty())
        {
            return LauncherStatus.ERROR_EMPTY_STACK;
        }
        if (host.getFiringPackage() != null && host.getFiringPackage().getCountDown() > 0)
        {
            return new FiringWithDelay(host.getFiringPackage().getCountDown());
        }
        if (!host.canSpawnMissileWithNoCollision())
        {
            return LauncherStatus.ERROR_SPAWN_COLLIDER;
        }
        return LauncherStatus.READY;
    }

    @Override
    public IActionStatus preCheckLaunch(IMissileTarget targetData, @Nullable IActionCause cause)
    {
        if (targetData == null || targetData.getPosition() == null)
        {
            return LauncherStatus.ERROR_TARGET_NULL;
        }
        if (host.isTooClose(targetData.getPosition()))
        {
            return LauncherStatus.ERROR_MIN_RANGE;
        }
        return getStatus();
    }

    @Override
    public IActionStatus launch(ILauncherSolution solution, @Nullable IActionCause cause, boolean simulate)
    {
        final IMissileTarget target = solution.getTarget(this);
        final IActionStatus check = preCheckLaunch(target, cause);
        if (check.isBlocking())
        {
            return check;
        }
        if (simulate)
        {
            return LauncherStatus.LAUNCHED;
        }
        host.setTarget(target.getPosition());
        if (!host.isAimed())
        {
            host.setFiringPackage(new icbm.classic.content.blocks.launcher.FiringPackage(target, cause, 0));
            return LauncherStatus.FIRING_AIMING;
        }

        final ServerLevel level = host.getLevel() instanceof ServerLevel serverLevel ? serverLevel : null;
        if (level == null)
        {
            return LauncherStatus.ERROR_SPAWN;
        }
        final ItemStack stack = host.missileHolder.getMissileStack();
        final ICapabilityMissileStack missileStack = IcbmLookups.MISSILE_STACK.find(stack, null);
        if (missileStack == null)
        {
            return LauncherStatus.ERROR_INVALID_STACK;
        }
        final CausedByBlock blockCause = new CausedByBlock(level, host.getBlockPos(), host.getBlockState());
        blockCause.setPreviousCause(cause);
        final IActionSource source = new ActionSource(level,
                new Vec3(host.getBlockPos().getX() + 0.5, host.getBlockPos().getY() + TileCruiseLauncher.MISSILE_HOLDER_Y,
                        host.getBlockPos().getZ() + 0.5), blockCause);
        final IMissile missile = missileStack.newMissile(level);
        final Entity entity = missile.getMissileEntity();
        entity.setPos(source.getPosition());
        if (!host.missileHolder.consumeMissile())
        {
            return LauncherStatus.ERROR_INVALID_STACK;
        }
        if (!EnergyRules.consumeOrFree(host.energyStorage, host.getFiringCost()))
        {
            return LauncherStatus.ERROR_POWER;
        }
        missile.setMissileSource(source);
        missile.setTargetData(target);
        missile.setFlightLogic(new DirectFlightLogic(ConfigMissile.CRUISE_FUEL));
        missile.launch();
        if (!level.addFreshEntity(entity))
        {
            return LauncherStatus.ERROR_SPAWN;
        }
        return new LaunchedWithMissile().setMissile(missile);
    }

    @Override
    public float getPayloadVelocity()
    {
        return host.getMissileHolder().getMissileStack().getItem() == ModItems.ANTI_BALLISTIC_MISSILE
                ? ConfigMissile.SAM_MISSILE.FLIGHT_SPEED : ConfigMissile.DIRECT_FLIGHT_SPEED;
    }
}
