package icbm.classic.content.missile.logic.flight.move;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.logic.flight.prefab.AccelerateByVec3Logic;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/**
 * Flight logic to move in a direction vector
 */
public class MoveByVec3Logic extends AccelerateByVec3Logic {
    public static final Identifier REG_NAME = ICBMConstants.id("engine.move.vector");

    /** Distance to cover */
    @Getter @Setter @Accessors(chain = true)
    private double distance;

    /** When set to true it will track distance relative to the acceleration and not actual amount moved */
    @Getter @Setter @Accessors(chain = true)
    private boolean relative = false;

    @Override
    protected void dumpInformation(Consumer<String> outputLines) {
        super.dumpInformation(outputLines);
        outputLines.accept("\tDistance: " + distance);
        outputLines.accept("\tRelative: " + relative);
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir) {
        super.onEntityTick(entity, missile, ticksInAir);

        // Movement ignoring outside forces
        if(relative) {
            distance -= getVelocityAdded();
        }
        // Measured movement in direction, technically can run forever if something stops movement
        else
        {
            final Vec3 motion = entity.getDeltaMovement();
            distance -= getDirection().x * motion.x;
            distance -= getDirection().y * motion.y;
            distance -= getDirection().z * motion.z;
        }
    }

    @Override
    public boolean isDone() {
        return distance <= 0;
    }

    @Override
    public boolean canSafelyExitLogic() {
        return super.canSafelyExitLogic();
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

    private static final SaveHandler<MoveByVec3Logic> SAVE_LOGIC = new SaveHandler<MoveByVec3Logic>()
        .mainRoot()
        /* */.nodeDouble("distance", MoveByVec3Logic::getDistance, MoveByVec3Logic::setDistance)
        .base();
}
