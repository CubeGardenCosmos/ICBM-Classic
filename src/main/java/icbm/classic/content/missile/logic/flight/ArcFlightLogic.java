package icbm.classic.content.missile.logic.flight;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.missile.entity.EntityMissile;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.lib.buildable.BuildableObject;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Flight path that moves in a ballistic arc from start position to target position
 */
public class ArcFlightLogic extends BuildableObject<ArcFlightLogic, IBuilderRegistry<IMissileFlightLogic>> implements IMissileFlightLogic
{
    public static final Identifier REG_NAME = ICBMConstants.id("path.arc");

    public boolean hasStartedFlight = false;
    /**
     * Tick runtime of flight arc
     */
    private int missileFlightTime;
    /**
     * Motion Y acceleration for arc to work
     */
    private float acceleration;

    /**
     * Difference in distance from target, used as acceleration
     */
    private double deltaPathX, deltaPathY, deltaPathZ;
    private double startX, startY, startZ;
    private double endX, endY, endZ;

    private boolean flightUpAlways = false;

    private int ticksFlight = 0;

    private boolean wasSimulationBlocked = false;

    public ArcFlightLogic() {
        super(REG_NAME, ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY, SAVE_LOGIC);
    }

    @Override
    public void calculateFlightPath(final Level world, double startX, double startY, double startZ, final IMissileTarget targetData)
    {
        //Record start and end position
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        if(targetData != null)
        {
            this.endX = targetData.getX();
            this.endY = targetData.getY();
            this.endZ = targetData.getZ();
        }

        // TODO check if entire path is chunk loaded, if a reasonable chunk count isn't then we should notify the user in the launcher UI and change to a up/down flight path
        //      This likely will require a static function to run the calculation and show in the GUI. However, should be referenced based on the missile type
        //      As future version might include different guidance systems based on the item's stored data. Meaning a single function wouldn't work but could
        //      be referenced in a prediction callback. Something like IMissile#getGuidance().doPrediction()
    }

    //TODO wire IMissile to connect to ILauncher so we can get launcher source and let the launcher know when we are clear
    //TODO code launcher to not reload until clear, use the all clear flag from the missile combined with collision checks and dead checks

    /**
     * The arc parameters calculated by {@link #calculateArcParameters(double, double, double, double, double, double, boolean, double)}.
     *
     * @param deltaX            distance to cover on the x axis
     * @param deltaY            distance to cover on the y axis
     * @param deltaZ            distance to cover on the z axis
     * @param missileFlightTime ticks the arc is expected to take, only valid when {@link #flightUpAlways()} is false
     * @param acceleration      y acceleration applied each tick, only valid when {@link #flightUpAlways()} is false
     * @param flightUpAlways    true when the target is out of arc range and the missile should fly straight up
     */
    public record ArcParameters(double deltaX, double deltaY, double deltaZ, int missileFlightTime, float acceleration, boolean flightUpAlways) {
    }

    /**
     * Pure math of the 1.12 {@code calculatePath(Entity)} method, split out so it can be tested without an
     * entity or a level (see {@code ArcFlightLogicMathTest}).
     *
     * @param startX           launch x
     * @param startY           launch y
     * @param startZ           launch z
     * @param endX             target x
     * @param endY             target y
     * @param endZ             target z
     * @param playerRiding     true when a player rides the missile, forces the arc path
     * @param arcDistanceLimit {@link ConfigMissile#ARC_DISTANCE_LIMIT}, max flat distance to fly an arc over
     * @return calculated parameters, {@code flightUpAlways} when the flat distance is out of range
     */
    public static ArcParameters calculateArcParameters(double startX, double startY, double startZ,
                                                       double endX, double endY, double endZ,
                                                       boolean playerRiding, double arcDistanceLimit)
    {
        // Calculate the distance difference of the missile
        final double deltaPathX = endX - startX;
        final double deltaPathY = endY - startY;
        final double deltaPathZ = endZ - startZ;

        // TODO: Calculate parabola and relative out the targetHeight.
        // Calculate the power required to reach the target co-ordinates
        // Ground Displacement
        final float flatDistance = (float)Math.sqrt(deltaPathX * deltaPathX + deltaPathZ * deltaPathZ);

        if(flatDistance < arcDistanceLimit || playerRiding)
        {
            //Path constants
            final float ticksPerMeterFlat = 2f;
            final float heightToDistanceScale = flatDistance > 1000 ? 3f : 1f;
            final float maxHeight = 1000f;
            final float initialArcHeight = flatDistance > 100 ? 160f : 0;

            // Parabolic Height
            // Ballistic flight vars
            final float arcHeightMax = Math.min(maxHeight, initialArcHeight + (flatDistance * heightToDistanceScale));

            // Flight time
            final int missileFlightTime = (int) Math.ceil(ticksPerMeterFlat * flatDistance);

            // Acceleration
            double heightToDistance = arcHeightMax / flatDistance;
            double heightToTime = arcHeightMax / missileFlightTime;
            double timeToDistance = missileFlightTime / flatDistance;
            final float acceleration = (float) (((arcHeightMax - heightToDistance) * heightToDistance) / (missileFlightTime / timeToDistance) / (heightToTime * flatDistance));

            return new ArcParameters(deltaPathX, deltaPathY, deltaPathZ, missileFlightTime, acceleration, false);
        }

        return new ArcParameters(deltaPathX, deltaPathY, deltaPathZ, 0, 0, true);
    }

    protected void calculatePath(Entity entity)
    {
        //TODO rebuild to calculate arc up to maxHeight and move at a fixed speed instead of speed of sound+++++
        //TODO once it reaches maxHeight have it fly flat to make for a smoother player riding experience
        //TODO at end of arc if we can't fix the target offset have the missile fly at an angle strait at the target to fix accuracy issues

        final ArcParameters path = calculateArcParameters(startX, startY, startZ, endX, endY, endZ,
            entity.getPassengers().stream().anyMatch(e -> e instanceof Player), ConfigMissile.ARC_DISTANCE_LIMIT);

        this.deltaPathX = path.deltaX();
        this.deltaPathY = path.deltaY();
        this.deltaPathZ = path.deltaZ();

        if (path.flightUpAlways()) {
            // 1.12 only ever turned this flag on and left missileFlightTime/acceleration untouched
            flightUpAlways = true;
        } else {
            this.missileFlightTime = path.missileFlightTime();
            this.acceleration = path.acceleration();
        }
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir)
    {
        // Reset path if player starts riding after we already started
        if(flightUpAlways && entity.getPassengers().stream().anyMatch(e -> e instanceof Player)) {
            this.flightUpAlways = false;

            // TODO instead of restarting flight logic... end this instance and replace with a new instance appended to flight plan
            this.startX = entity.getX();
            this.startY = entity.getY();
            this.startZ = entity.getZ();
            ticksFlight = 0;
            this.calculatePath(entity);
            return;
        }

        //Starts the missile into normal flight
        if (!hasStartedFlight) {

            hasStartedFlight = true;

            this.startX = entity.getX();
            this.startY = entity.getY();
            this.startZ = entity.getZ();

            this.calculatePath(entity);

            // TODO split this into two different flight logics. Then have a parent logic object switch which is running
            if(!flightUpAlways) {

                entity.setDeltaMovement(
                    this.deltaPathX / missileFlightTime,
                    this.acceleration * ((float) missileFlightTime / 2f),
                    this.deltaPathZ / missileFlightTime);
            }
            else {
                final float flatDistance = (float)Math.sqrt(deltaPathX * deltaPathX + deltaPathZ * deltaPathZ);
                final float hortSpeed = 0.05f;
                final float vertSpeed = 2f;
                entity.setDeltaMovement(
                    (this.deltaPathX / flatDistance) * hortSpeed,
                    vertSpeed,
                    (this.deltaPathZ / flatDistance) * hortSpeed);
            }
        }
        //Normal path logic
        else {
            runFlightLogic(entity, ticksInAir);
        }
    }

    protected void runFlightLogic(Entity entity, int ticksInAir)
    {
        ticksFlight++;

        if (!entity.level().isClientSide())
        {

            // Apply gravity
            if(!flightUpAlways) {
                // 1.12 wrote entity.motionY -= acceleration through the projectile's setMotionVector
                final Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, motion.y - this.acceleration, motion.z);
            }

            // Cut off x-z motion to prevent missing target
            if(Math.abs(entity.getX() - endX) <= 0.1f && Math.abs(entity.getZ() - endZ) <= 0.1f) {
                final Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(0, motion.y, 0);
            }

            // Update animate rotations
            alignWithMotion(entity);

            // Sim system
            if (shouldSimulate(entity))
            {
                // TODO(S4.03): hand the missile over to the off chunk simulation (MissileTrackerHandler)
                wasSimulationBlocked = true;
            }
        }
    }

    @Override
    public boolean canSafelyExitLogic() {
        return hasStartedFlight;
    }

    protected void alignWithMotion(Entity entity)
    {
        final Vec3 motion = entity.getDeltaMovement();
        final float pitch = (float) (Math.atan(motion.y / (Math.sqrt(motion.x * motion.x + motion.z * motion.z))) * 180 / Math.PI);
        // Look at the next point
        final float yaw = (float) (Math.atan2(motion.x, motion.z) * 180 / Math.PI);
        // 1.12 set rotationPitch/rotationYaw directly, 26.3 also updates the previous tick values so the client
        // copy does not interpolate through a full rotation between ticks
        entity.xRotO = pitch;
        entity.setXRot(pitch);
        entity.yRotO = yaw;
        entity.setYRot(yaw);
    }

    @Override
    public boolean shouldRunEngineEffects(Entity entity) {
        return hasStartedFlight;
    }

    protected boolean shouldSimulate(Entity entity)
    {
        if (wasSimulationBlocked || EntityMissile.hasPlayerRiding(entity))
        {
            return false;
        }
        else if (entity.getY() >= ConfigMissile.SIMULATION_EXIT_HEIGHT)
        {
            return true;
        }

        // TODO predict if the missile only needed a few more chunks to hit... if so chunk load or let get stuck in board to prevent changing arc path

        // 1.12 used BlockPos::new (a double constructor); 26.3 answers with BlockPos#containing
        final BlockPos futurePos = predictPosition(entity, (x, y, z) -> BlockPos.containing(x, y, z), 2);

        if(entity.level() instanceof ServerLevel serverLevel) {

            int xStart = Mth.floor(entity.getX()) >> 4;
            int zStart = Mth.floor(entity.getZ()) >> 4;
            int xEnd = futurePos.getX() >> 4;
            int zEnd = futurePos.getZ() >> 4;

            // 1.12 asked WorldServer#getChunkProvider#chunkExists; 26.3 LevelReader#hasChunk answers the same
            int minX = Math.min(xStart, xEnd);
            int maxX = Math.max(xStart, xEnd);
            int minZ = Math.min(zStart, zEnd);
            int maxZ = Math.max(zStart, zEnd);

            for (int i = minX; i <= maxX; ++i) {
                for (int j = minZ; j <= maxZ; ++j) {
                    if (!serverLevel.hasChunk(i, j)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public <V> V predictPosition(Entity entity, VecBuilderFunc<V> builder, int ticks)
    {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        final Vec3 motion = entity.getDeltaMovement();
        double motionY = motion.y;

        while (ticks-- > 0)
        {
            motionY -= this.acceleration;

            x += motion.x;
            y += motionY;
            z += motion.z;
        }

        return builder.apply(x, y, z);
    }

    @Override
    public boolean shouldDecreaseMotion(Entity entity)
    {
        //Disable gravity and friction
        return false;
    }

    private static final SaveHandler<ArcFlightLogic> SAVE_LOGIC = new SaveHandler<ArcFlightLogic>()
        //Stuck in ground data
        .addRoot("flags")
        /* */.nodeBoolean("flight_started", (bl) -> bl.hasStartedFlight, (bl, data) -> bl.hasStartedFlight = data)
        /* */.nodeBoolean("block_simulation", (bl) -> bl.wasSimulationBlocked, (bl, data) -> bl.wasSimulationBlocked = data)
        .base()
        .addRoot("inputs")
        /* */.nodeDouble("start_x", (bl) -> bl.startX, (bl, i) -> bl.startX = i)
        /* */.nodeDouble("start_y", (bl) -> bl.startY, (bl, i) -> bl.startY = i)
        /* */.nodeDouble("start_z", (bl) -> bl.startZ, (bl, i) -> bl.startZ = i)
        /* */.nodeDouble("end_x", (bl) -> bl.endX, (bl, i) -> bl.endX = i)
        /* */.nodeDouble("end_y", (bl) -> bl.endY, (bl, i) -> bl.endY = i)
        /* */.nodeDouble("end_z", (bl) -> bl.endZ, (bl, i) -> bl.endZ = i)
        .base()
        .addRoot("calculated")
        /* */.nodeInteger("flight_time", (bl) -> bl.missileFlightTime, (bl, data) -> bl.missileFlightTime = data)
        /* */.nodeFloat("acceleration", (bl) -> bl.acceleration, (bl, data) -> bl.acceleration = data)
        /* */.nodeDouble("delta_x", (bl) -> bl.deltaPathX, (bl, data) -> bl.deltaPathX = data)
        /* */.nodeDouble("delta_y", (bl) -> bl.deltaPathY, (bl, data) -> bl.deltaPathY = data)
        /* */.nodeDouble("delta_z", (bl) -> bl.deltaPathZ, (bl, data) -> bl.deltaPathZ = data)
        .base()
        .addRoot("timers")
        /* */.nodeInteger("flight", (bl) -> bl.ticksFlight, (bl, i) -> bl.ticksFlight = i)
        .base();
}
