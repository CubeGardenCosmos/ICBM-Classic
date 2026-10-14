package icbm.classic.content.missile.logic.flight.prefab;

import icbm.classic.api.missiles.IMissile;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Flight logic to move in a direction matching {@link Direction} until a set distance is meet
 */
public abstract class AccelerateByFacingLogic extends FlightLogic {

    /** Direction to move */
    @Getter @Setter @Accessors(chain = true)
    private Direction direction;

    /** Acceleration to move at per tick */
    @Getter @Setter @Accessors(chain = true)
    private double acceleration;

    @Getter
    private double velocityAdded = 0;

    @Override
    protected void dumpInformation(Consumer<String> outputLines) {
        super.dumpInformation(outputLines);
        outputLines.accept("\tDirection: " + direction);
        outputLines.accept("\tAcceleration: " + acceleration);
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir) {
        super.onEntityTick(entity, missile, ticksInAir);
        if(isValid() && !isDone()) {
            // Move missile
            // 1.12 wrote entity.motionX/Y/Z, 26.3 exposes the motion vector as a Vec3
            final Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(
                motion.x + direction.getStepX() * acceleration,
                motion.y + direction.getStepY() * acceleration,
                motion.z + direction.getStepZ() * acceleration);

            // Track acceleration added
            velocityAdded += acceleration;
        }
    }

    @Override
    public boolean isValid() {
        return direction != null;
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        SAVE_LOGIC.load(this, in);
    }

    private static final SaveHandler<AccelerateByFacingLogic> SAVE_LOGIC = new SaveHandler<AccelerateByFacingLogic>()
        .mainRoot()
        /* */.nodeDouble("acceleration", AccelerateByFacingLogic::getAcceleration, AccelerateByFacingLogic::setAcceleration)
        /* */.nodeDirection("direction", AccelerateByFacingLogic::getDirection, AccelerateByFacingLogic::setDirection)
        .base();
}
