package icbm.classic.content.missile.logic.flight.prefab;

import icbm.classic.api.missiles.IMissile;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * Flight logic to move in a direction vector
 */
public abstract class AccelerateByVec3Logic extends FlightLogic {

    /** Direction to move */
    @Getter @Setter @Accessors(chain = true)
    private Vec3 direction;

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
            final Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(
                motion.x + direction.x * acceleration,
                motion.y + direction.y * acceleration,
                motion.z + direction.z * acceleration);

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

    private static final SaveHandler<AccelerateByVec3Logic> SAVE_LOGIC = new SaveHandler<AccelerateByVec3Logic>()
        .mainRoot()
        /* */.nodeDouble("acceleration", AccelerateByVec3Logic::getAcceleration, AccelerateByVec3Logic::setAcceleration)
        /* */.nodeVec3("direction", AccelerateByVec3Logic::getDirection, AccelerateByVec3Logic::setDirection)
        .base();
}
