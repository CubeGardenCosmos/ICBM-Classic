package icbm.classic.content.missile.logic.flight;

import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.content.missile.logic.flight.prefab.FlightLogic;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

/**
 * Handles engine warmup logic
 */
public class WarmupFlightLogic extends FlightLogic {

    public static final Identifier REG_NAME = ICBMConstants.id("engine.warmup");

    /** Timer for missile to wait on pad before climbing */
    @Getter @Setter @Accessors(chain = true)
    private int timer = 0;

    @Override
    protected void dumpInformation(Consumer<String> outputLines) {
        super.dumpInformation(outputLines);
        outputLines.accept("\tTimer: " + timer);
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir) {
        super.onEntityTick(entity, missile, ticksInAir);
        // 1.12 spawned the launch pad smoke here (ICBMClassic.proxy.spawnPadSmoke, client only). 26.3 entities
        // do not tick client code, so the pad smoke is produced by
        // icbm.classic.client.particle.ModParticlesClient from the client tick; it picks the pad variant for a
        // missile that is not moving, which is exactly the span of this timer (S4.07).
        --timer;
    }

    @Override
    public boolean shouldDecreaseMotion(Entity entity)
    {
        //Disable gravity and friction
        return timer < 0 && getNextStep() == null;
    }

    @Override
    public boolean isDone() {
        return timer <= 0;
    }

    @Override
    public boolean shouldRunEngineEffects(Entity entity) {
        return timer < 0;
    }

    @Override
    public boolean canSafelyExitLogic() {
        return timer < 0;
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

    private static final SaveHandler<WarmupFlightLogic> SAVE_LOGIC = new SaveHandler<WarmupFlightLogic>()
        .mainRoot()
            // 1.12 declared the save half of this node as `(bl) -> null`, so the timer was never written and
            // always reloaded as the node default. The 26.3 SaveNodeInteger null checks the value the same way,
            // so the port keeps the 1.12 behaviour (see evidence).
            /* */.nodeInteger("timer", (bl) -> null, (bl, data) -> bl.timer = data)
        .base();
}
