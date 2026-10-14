package icbm.classic.content.missile.logic.flight;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.missiles.parts.IMissileFlightLogic;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.ISavable;
import icbm.classic.lib.saving.SaveHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * Flight computer that does nothing, acts as a placeholder for when we fire missiles like an arrow or are using
 * raw motion setting logic in another system.
 */
public class DeadFlightLogic implements IMissileFlightLogic, ISavable
{
    public static final Identifier REG_NAME = ICBMConstants.id("dead");

    public int fuelTicks = 0;

    public DeadFlightLogic()
    {
        //for save/load logic
    }

    public DeadFlightLogic(int fuelTicks)
    {
        this.fuelTicks = fuelTicks;
    }

    @Override
    public boolean shouldRunEngineEffects(Entity entity) {
        return hasFuel(entity);
    }

    protected boolean hasFuel(Entity entity) {
        return fuelTicks > 0;
    }

    @Override
    public void onEntityTick(Entity entity, IMissile missile, int ticksInAir)
    {
        fuelTicks--;

        if(hasFuel(entity)) {
            // 1.12 read/wrote motionX/Y/Z and rotationYaw/Pitch directly, 26.3 uses the accessors and the
            // previous tick rotation fields so the client copy interpolates in the right direction
            final Vec3 motion = entity.getDeltaMovement();
            final float f3 = Mth.sqrt((float)(motion.x * motion.x + motion.z * motion.z));
            final float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
            final float pitch = (float) (Math.atan2(motion.y, (double) f3) * 180.0D / Math.PI);
            entity.yRotO = yaw;
            entity.setYRot(yaw);
            entity.xRotO = pitch;
            entity.setXRot(pitch);
        }
    }

    @Override
    public boolean shouldAlignWithMotion(Entity entity) {
        return false; //TODO allow alignment at a much slower rate once out of fuel
    }

    @Override
    public void save(ValueOutput out)
    {
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in)
    {
        SAVE_LOGIC.load(this, in);
    }

    @Override
    public <V> V predictPosition(Entity entity, VecBuilderFunc<V> builder, int ticks)
    {
        final Vec3 motion = entity.getDeltaMovement();
        return builder.apply(
            entity.getX() + motion.x * ticks, //TODO add gravity
            entity.getY() + motion.y * ticks,
            entity.getZ() + motion.z * ticks
        );
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }

    @NonNull
    @Override
    public IBuilderRegistry<IMissileFlightLogic> getRegistry() {
        return ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY;
    }

    @Override
    public boolean shouldDecreaseMotion(Entity entity)
    {
        return !hasFuel(entity);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof DeadFlightLogic) {
            // 1.12 compared the registry key by reference; Identifier has value equality, so use it
            return fuelTicks == ((DeadFlightLogic) other).fuelTicks && getRegistryKey().equals(((DeadFlightLogic) other).getRegistryKey());
        }
        return false;
    }

    private static final SaveHandler<DeadFlightLogic> SAVE_LOGIC = new SaveHandler<DeadFlightLogic>()
        .mainRoot()
        /* */.nodeInteger("fuel", (logic) -> logic.fuelTicks, (logic, data) -> logic.fuelTicks = data)
        .base();
}
