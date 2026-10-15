package icbm.classic.content.blocks.launcher.base;

import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.events.LauncherEvent;
import icbm.classic.api.launcher.ILauncherSolution;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileFlightLogicStep;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.missiles.parts.IMissileTargetDelayed;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.blocks.launcher.FiringPackage;
import icbm.classic.content.blocks.launcher.LauncherBaseCapability;
import icbm.classic.content.blocks.launcher.status.FiringWithDelay;
import icbm.classic.content.blocks.launcher.status.LaunchedWithMissile;
import icbm.classic.content.blocks.launcher.status.LauncherStatus;
import icbm.classic.content.missile.logic.flight.ArcFlightLogic;
import icbm.classic.content.missile.logic.flight.WarmupFlightLogic;
import icbm.classic.content.missile.logic.flight.move.MoveByFacingLogic;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.content.missile.logic.targeting.BallisticTargetingData;
import net.minecraft.core.BlockPos;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.energy.EnergyRules;
import icbm.classic.lib.transform.rotation.EulerAngle;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class LauncherCapability extends LauncherBaseCapability
{
    public static final int PAD_WARM_UP_TIME = 20 * 2;
    public static final double MISSILE_CLIMB_HEIGHT = 2;
    private static final EulerAngle ANGLE = new EulerAngle(0, 0, 0);
    private static final Vec3[] SPAWN_OFFSETS = {
            new Vec3(0, -3.1f, 0), new Vec3(0, 2.6f, 0),
            new Vec3(0, -0.2f, -2.8f), new Vec3(0, -0.2f, 2.8f),
            new Vec3(-2.8f, -0.2f, 0), new Vec3(2.8f, -0.2f, 0)
    };

    private final TileLauncherBase host;

    public LauncherCapability(TileLauncherBase host)
    {
        this.host = host;
    }

    public TileLauncherBase getHost()
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
        if (host.missileHolder.getMissileStack().isEmpty())
        {
            return LauncherStatus.ERROR_EMPTY_STACK;
        }
        if (host.getFiringPackage() != null && host.getFiringPackage().getCountDown() > 0)
        {
            return new FiringWithDelay(host.getFiringPackage().getCountDown());
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
        if (isTargetTooClose(targetData.getPosition()))
        {
            return LauncherStatus.ERROR_MIN_RANGE;
        }
        if (isTargetTooFar(targetData.getPosition()))
        {
            return LauncherStatus.ERROR_MAX_RANGE;
        }
        return getStatus();
    }

    @Override
    public IActionStatus launch(ILauncherSolution solution, @Nullable IActionCause cause, boolean simulate)
    {
        final IMissileTarget targetData = solution.getTarget(this);
        final IActionStatus preCheck = preCheckLaunch(targetData, cause);
        if (preCheck.isBlocking())
        {
            return preCheck;
        }

        final Level level = host.getLevel();
        if (level == null)
        {
            return LauncherStatus.ERROR_SPAWN;
        }
        final CausedByBlock selfCause = new CausedByBlock(level, host.getBlockPos(), host.getBlockState());
        selfCause.setPreviousCause(cause);
        final Vec3 center = Vec3.atCenterOf(host.getBlockPos());
        final Vec3 spawnPosition = SPAWN_OFFSETS[host.getLaunchDirection().ordinal()].add(center);
        final ActionSource source = new ActionSource(level, spawnPosition, selfCause);

        final LauncherEvent.PreLaunch event = new LauncherEvent.PreLaunch(source, this, host.missileHolder, targetData, simulate);
        if (!LauncherEvent.PreLaunch.EVENT.invoker().onPreLaunch(event))
        {
            return event.cancelReason != null ? event.cancelReason : LauncherStatus.CANCELED;
        }

        final ItemStack stack = host.missileHolder.getMissileStack();
        final ICapabilityMissileStack missileStack = icbm.classic.lib.lookup.IcbmLookups.MISSILE_STACK.find(stack, null);
        if (missileStack == null)
        {
            return LauncherStatus.ERROR_INVALID_STACK;
        }

        final Vec3 target = applyInaccuracy(targetData.getPosition(), Math.max(1, solution.getFiringCount()));
        if (host.getFiringPackage() == null)
        {
            int delay = host.getFiringDelay();
            if (targetData instanceof IMissileTargetDelayed delayed)
            {
                delay += delayed.getFiringDelay();
            }
            if (delay > 0)
            {
                if (!simulate)
                {
                    host.setFiringPackage(new FiringPackage(targetData, cause, delay));
                }
                return new FiringWithDelay(delay);
            }
        }
        if (level.isClientSide() || simulate)
        {
            return LauncherStatus.LAUNCHED;
        }
        return fireMissile(missileStack.newMissile(level), source, target);
    }

    private IActionStatus fireMissile(IMissile missile, IActionSource source, Vec3 target)
    {
        if (!host.missileHolder.consumeMissile())
        {
            return LauncherStatus.ERROR_INVALID_STACK;
        }

        final Entity entity = missile.getMissileEntity();
        entity.setXRot(host.getMissilePitch(false));
        entity.setYRot(host.getMissileYaw(false));
        entity.setPos(source.getPosition());
        missile.setTargetData(new BallisticTargetingData(target, 2));
        missile.setFlightLogic(buildFLightPath());
        missile.setMissileSource(source);
        missile.launch();

        final Level level = host.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || !serverLevel.addFreshEntity(entity))
        {
            return LauncherStatus.ERROR_SPAWN;
        }
        if (!EnergyRules.consumeOrFree(host.energyStorage, host.getFiringCost()))
        {
            return LauncherStatus.ERROR_POWER;
        }
        if (host.seat != null && !host.seat.getPassengers().isEmpty())
        {
            final List<Entity> riders = List.copyOf(host.seat.getPassengers());
            for (Entity rider : riders)
            {
                rider.stopRiding();
                rider.startRiding(entity);
            }
        }
        return new LaunchedWithMissile().setMissile(missile);
    }

    public IMissileFlightLogic buildFLightPath()
    {
        if (host.getLaunchDirection() != Direction.UP)
        {
            final IMissileFlightLogicStep lockHeight = new MoveByFacingLogic()
                    .setDistance(host.getLockHeight()).setRelative(false)
                    .setDirection(host.getLaunchDirection()).setAcceleration(0.2);
            return new WarmupFlightLogic().setTimer(PAD_WARM_UP_TIME)
                    .addStep(lockHeight).addStep(new ArcFlightLogic());
        }
        final IMissileFlightLogicStep slowClimb = new MoveByFacingLogic()
                .setDistance(MISSILE_CLIMB_HEIGHT).setRelative(false)
                .setDirection(host.getLaunchDirection()).setAcceleration(0.005);
        final IMissileFlightLogicStep lockHeight = new MoveByFacingLogic()
                .setDistance(host.getLockHeight() - MISSILE_CLIMB_HEIGHT).setRelative(false)
                .setDirection(host.getLaunchDirection()).setAcceleration(0.1);
        return new WarmupFlightLogic().setTimer(PAD_WARM_UP_TIME)
                .addStep(slowClimb).addStep(lockHeight).addStep(new ArcFlightLogic());
    }

    @Override
    public float getInaccuracy(Vec3 target, int launcherCount)
    {
        final BlockPos pos = host.getBlockPos();
        final Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return calculateInaccuracy(center.distanceToSqr(target), launcherCount);
    }

    public static float calculateInaccuracy(double distanceSq, int missiles)
    {
        float inaccuracy = (float) ConfigLauncher.MIN_INACCURACY;
        final double scale = distanceSq / ((double) ConfigLauncher.RANGE * ConfigLauncher.RANGE);
        inaccuracy += (float) (scale * ConfigLauncher.SCALED_INACCURACY_DISTANCE);
        if (missiles > 1)
        {
            inaccuracy += (float) ((missiles - 1) * ConfigLauncher.SCALED_INACCURACY_LAUNCHERS);
        }
        return inaccuracy;
    }

    protected Vec3 applyInaccuracy(Vec3 target, int launcherCount)
    {
        final Level level = host.getLevel();
        if (level == null)
        {
            return target;
        }
        final float inaccuracy = getInaccuracy(target, launcherCount) * level.getRandom().nextFloat();
        ANGLE.setYaw(level.getRandom().nextFloat() * 360);
        return new Vec3(target.x + ANGLE.x() * inaccuracy, target.y, target.z + ANGLE.z() * inaccuracy);
    }

    public boolean isTargetTooClose(Vec3 target)
    {
        final BlockPos pos = host.getBlockPos();
        return Math.abs(target.x - (pos.getX() + 0.5)) < 10 && Math.abs(target.z - (pos.getZ() + 0.5)) < 10;
    }

    public boolean isTargetTooFar(Vec3 target)
    {
        final BlockPos pos = host.getBlockPos();
        return Math.abs(target.x - (pos.getX() + 0.5)) > ConfigLauncher.RANGE
                || Math.abs(target.z - (pos.getZ() + 0.5)) > ConfigLauncher.RANGE;
    }

    @Override
    public float getPayloadVelocity()
    {
        return host.missileHolder.getMissileStack().getItem() == ModItems.ANTI_BALLISTIC_MISSILE
                ? icbm.classic.content.abm.ConfigAbm.FLIGHT_SPEED : ConfigMissile.DIRECT_FLIGHT_SPEED;
    }
}
