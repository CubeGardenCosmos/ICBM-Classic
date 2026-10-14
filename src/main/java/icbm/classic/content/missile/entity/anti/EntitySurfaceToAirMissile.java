package icbm.classic.content.missile.entity.anti;

import icbm.classic.api.radio.IRadioTargetAssignable;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.logic.flight.FollowTargetLogic;
import icbm.classic.content.reg.ModDamageTypes;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Surface to Air Missile (SAM) / Anti-Ballistic Missile entity.
 * <p>
 * Scans for hostile flying missiles using {@link SAMTargetData} and intercepts them with
 * {@link FollowTargetLogic}. Implements {@link IRadioTargetAssignable} to allow radar stations
 * and incoming missile messages to redirect the target.
 *
 * Created by Robin Seifert on 11/30/2021.
 */
public class EntitySurfaceToAirMissile extends EntityMissile<EntitySurfaceToAirMissile> implements IRadioTargetAssignable
{
    private ItemStack renderStackCache;
    public final SAMTargetData scanLogic = new SAMTargetData(this);

    private boolean hasStartedFollowing = false;

    public EntitySurfaceToAirMissile(EntityType<? extends EntitySurfaceToAirMissile> type, Level level)
    {
        super(type, level);
        this.getMissileCapability().setTargetData(scanLogic);
        this.initHealth(ConfigMissile.TIER_2_HEALTH);
    }

    public EntitySurfaceToAirMissile(Level level)
    {
        this(ModEntities.MISSILE_SAM, level);
    }

    @Override
    public void assignTarget(Entity target)
    {
        scanLogic.setCurrentTarget(target);
    }

    @Override
    public void tick()
    {
        if (!this.level().isClientSide())
        {
            // Scan for targets
            scanLogic.tick();

            final Entity currentTarget = scanLogic.getTarget();

            // Switch to follow logic once we have a target in range, launcher will set initial flight logic to get it out of the tube
            final boolean canExit = this.getMissileCapability().getFlightLogic() == null
                    || this.getMissileCapability().getFlightLogic().canSafelyExitLogic();
            if (!hasStartedFollowing && currentTarget != null && canExit)
            {
                hasStartedFollowing = true;
                // TODO play missile lock sound effect

                // Update our targeting system to track sam targets, some flight systems will use their own targeting logic
                this.getMissileCapability().setTargetData(scanLogic);

                // Update our flight logic to follow our sam target
                this.getMissileCapability().switchFlightLogic(new FollowTargetLogic(ConfigMissile.SAM_MISSILE.FUEL));
            }

            // Handle kill target logic
            if (currentTarget != null)
            {
                final double distance = this.distanceTo(currentTarget);

                if (distance <= ConfigMissile.SAM_MISSILE.FLIGHT_SPEED)
                {
                    if (this.level() instanceof ServerLevel serverLevel)
                    {
                        currentTarget.hurtServer(serverLevel,
                                ModDamageTypes.source(this.level(), ModDamageTypes.MISSILE_IMPACT, this, null),
                                ConfigMissile.SAM_MISSILE.ATTACK_DAMAGE);
                    }
                    // TODO play sound effect of missile exploding
                    this.discard();
                }
            }
        }

        // Normal update logic
        super.tick();
    }

    @Override
    public ItemStack toStack()
    {
        if (this.level().isClientSide())
        {
            if (renderStackCache == null)
            {
                renderStackCache = new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
            }
            return renderStackCache;
        }
        return new ItemStack(ModItems.ANTI_BALLISTIC_MISSILE);
    }
}
