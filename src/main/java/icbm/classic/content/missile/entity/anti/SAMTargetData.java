package icbm.classic.content.missile.entity.anti;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.lib.buildable.BuildableObject;
import icbm.classic.lib.radar.RadarEntity;
import icbm.classic.lib.radar.RadarMap;
import icbm.classic.lib.radar.RadarRegistry;
import lombok.Setter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Handles scanning for targets for the Surface to Air Missile.
 *
 * Created by Robin Seifert on 11/30/2021.
 */
public class SAMTargetData extends BuildableObject<SAMTargetData, IBuilderRegistry<IMissileTarget>> implements IMissileTarget
{
    public static final Identifier REG_NAME = ICBMConstants.id("anti.missile");

    private static final int MAX_TARGETS = 5;
    private static final int SCAN_DELAY = 10;

    private final Queue<Entity> targets = new LinkedList<>();

    @Setter
    private Entity currentTarget;

    private final EntitySurfaceToAirMissile host;

    private int scanDelayTick = 0;

    public SAMTargetData(EntitySurfaceToAirMissile host)
    {
        super(REG_NAME, ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY, null);
        this.host = host;
    }

    public void tick()
    {
        if (scanDelayTick < SCAN_DELAY)
        {
            scanDelayTick++;
        }
        else if (targets.isEmpty())
        {
            scanDelayTick = 0;
            refreshTargets();
        }
    }

    public void refreshTargets()
    {
        if (ConfigMissile.SAM_MISSILE.RADAR_MAP_ONLY)
        {
            seekByRadar();
        }
        else
        {
            seekByAABB();
        }
    }

    private void seekByRadar()
    {
        final RadarMap map = RadarRegistry.getRadarMapForWorld(host.level());
        if (map == null)
        {
            return;
        }

        final List<RadarEntity> entries = map.getRadarObjects(host.getX(), host.getZ(), ConfigMissile.SAM_MISSILE.TARGET_RANGE);
        final List<Entity> valid = entries.stream()
                .filter(RadarEntity::isValid)
                .map(e -> e.entity)
                .filter(this::isValid)
                .collect(Collectors.toList());

        valid.sort((a, b) -> {
            final double distanceA = host.distanceToSqr(a);
            final double distanceB = host.distanceToSqr(b);
            return Double.compare(distanceA, distanceB);
        });

        targets.addAll(valid.subList(0, Math.min(MAX_TARGETS, valid.size())));
    }

    private void seekByAABB()
    {
        final List<EntityMissile> missiles = getValidTargets();

        missiles.sort((a, b) -> {
            final double distanceA = host.distanceToSqr(a);
            final double distanceB = host.distanceToSqr(b);
            return Double.compare(distanceA, distanceB);
        });

        targets.addAll(missiles.subList(0, Math.min(MAX_TARGETS, missiles.size())));
    }

    private List<EntityMissile> getValidTargets()
    {
        return host.level().getEntitiesOfClass(EntityMissile.class, targetArea(), this::isValid);
    }

    private AABB targetArea()
    {
        return new AABB(
                host.getX() - ConfigMissile.SAM_MISSILE.TARGET_RANGE,
                host.getY() - ConfigMissile.SAM_MISSILE.TARGET_RANGE,
                host.getZ() - ConfigMissile.SAM_MISSILE.TARGET_RANGE,
                host.getX() + ConfigMissile.SAM_MISSILE.TARGET_RANGE,
                host.getY() + ConfigMissile.SAM_MISSILE.TARGET_RANGE,
                host.getZ() + ConfigMissile.SAM_MISSILE.TARGET_RANGE
        );
    }

    private boolean isValid(Entity entity)
    {
        return entity instanceof EntityMissile
                && !(entity instanceof EntitySurfaceToAirMissile)
                && entity.isAlive();
    }

    public Entity getTarget()
    {
        // Invalidate target if it is no longer valid (likely dead)
        if (!isValid(currentTarget))
        {
            currentTarget = null;
        }

        // Loop until we find a good target or run out of targets
        while (currentTarget == null && targets.peek() != null)
        {
            currentTarget = targets.poll();
            if (!isValid(currentTarget))
            {
                currentTarget = null;
            }
        }

        return currentTarget;
    }

    @Override
    public Vec3 getPosition()
    {
        return getTarget() != null ? getTarget().position() : null;
    }

    @Override
    public boolean isValid()
    {
        return isValid(getTarget());
    }

    @Override
    public double getX()
    {
        return Optional.ofNullable(getTarget()).map(Entity::getX).orElse(0.0);
    }

    @Override
    public double getY()
    {
        return Optional.ofNullable(getTarget()).map(Entity::getY).orElse(0.0);
    }

    @Override
    public double getZ()
    {
        return Optional.ofNullable(getTarget()).map(Entity::getZ).orElse(0.0);
    }
}
