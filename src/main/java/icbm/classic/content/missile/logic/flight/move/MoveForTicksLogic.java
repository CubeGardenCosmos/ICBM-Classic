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
import org.jspecify.annotations.NonNull;

/**
 * Flight logic to move in a direction matching {@link Direction} until a set distance is meet
 */
public class MoveForTicksLogic extends AccelerateByFacingLogic {
    public static final Identifier REG_NAME = ICBMConstants.id("engine.move.ticks");

    /** Distance to cover */
    @Getter @Setter @Accessors(chain = true)
    private int ticks;

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir) {
        super.onEntityTick(entity, missile, ticksInAir);
        if (ticks >= 0) {
            ticks--;
        }
    }

    @Override
    public boolean isDone() {
        return ticks <= 0;
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

    private static final SaveHandler<MoveForTicksLogic> SAVE_LOGIC = new SaveHandler<MoveForTicksLogic>()
        .mainRoot()
        /* */.nodeInteger("ticks", MoveForTicksLogic::getTicks, MoveForTicksLogic::setTicks)
        .base();
}
