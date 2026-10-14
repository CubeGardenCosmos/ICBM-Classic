package icbm.classic.content.missile.logic.flight.move;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.logic.flight.prefab.AccelerateByFacingLogic;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/**
 * Flight logic to move in a direction matching {@link Direction} until a set distance is meet
 */
public class MoveByFacingLogic extends AccelerateByFacingLogic {
    public static final Identifier REG_NAME = ICBMConstants.id("engine.move.facing");

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
            distance -= getDirection().getStepX() * motion.x;
            distance -= getDirection().getStepY() * motion.y;
            distance -= getDirection().getStepZ() * motion.z;
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

    private static final SaveHandler<MoveByFacingLogic> SAVE_LOGIC = new SaveHandler<MoveByFacingLogic>()
        .mainRoot()
        /* */.nodeDouble("distance", MoveByFacingLogic::getDistance, MoveByFacingLogic::setDistance)
        .base();
}
