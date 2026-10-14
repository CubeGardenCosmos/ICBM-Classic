package icbm.classic.content.missile.logic.targeting;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.parts.IMissileTarget;
import icbm.classic.api.missiles.parts.IMissileTargetDelayed;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.ISavable;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Simple 3D position targeting data
 * <p>
 * 1.12 implemented the Forge NBT serializable interface; 26.3 uses {@link ISavable} with the
 * {@code ValueOutput}/{@code ValueInput} API (porting-guide.md §4).
 */
public class BasicTargetData implements IMissileTarget, IMissileTargetDelayed, ISavable {

    public static final Identifier REG_NAME = ICBMConstants.id("basic");
    @Setter
    private Vec3 position;
    private int firingDelay = 0;

    public BasicTargetData() {
        //Only used for save/load
    }

    public BasicTargetData(double x, double y, double z) {
        this.position = new Vec3(x, y, z);
    }

    public BasicTargetData(Vec3 position) {
        this.position = position;
    }

    public BasicTargetData(BlockPos pos) {
        this.position = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public BasicTargetData withFiringDelay(int ticks) {
        this.firingDelay = ticks;
        return this;
    }

    @Override
    public int getFiringDelay() {
        return firingDelay;
    }

    @Override
    public IMissileTarget cloneWithoutDelay() {
        return new BasicTargetData(position);
    }

    @Override
    public boolean isValid() {
        // 1.12 wrote this as `Double.isNaN(getX()) && Double.isFinite(getX())`, kept as is (behaviour
        // equivalence, the porting guide asks not to fix the old TODOs/problems)
        return position != null
                && Double.isNaN(getX()) && Double.isFinite(getX())
                && Double.isNaN(getY()) && Double.isFinite(getY())
                && Double.isNaN(getZ()) && Double.isFinite(getZ());
    }

    @Override
    public Vec3 getPosition() {
        return position;
    }

    @Override
    public double getX() {
        return position != null ? position.x : Double.NaN;
    }

    @Override
    public double getY() {
        return position != null ? position.y : Double.NaN;
    }

    @Override
    public double getZ() {
        return position != null ? position.z : Double.NaN;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }

    @NonNull
    @Override
    public IBuilderRegistry<IMissileTarget> getRegistry() {
        return ICBMClassicAPI.MISSILE_TARGET_DATA_REGISTRY;
    }

    @Override
    public void save(ValueOutput out) {
        // 1.12 wrote the components as three doubles plus the delay; the keys are unchanged
        out.putDouble("x", position.x);
        out.putDouble("y", position.y);
        out.putDouble("z", position.z);
        out.putInt("firingDelay", firingDelay);
    }

    @Override
    public void load(ValueInput in) {
        // 1.12 read the three doubles then built one Vec3d (missing keys answered 0)
        double x = in.getDoubleOr("x", 0);
        double y = in.getDoubleOr("y", 0);
        double z = in.getDoubleOr("z", 0);
        this.position = new Vec3(x, y, z);
        this.firingDelay = in.getIntOr("firingDelay", 0);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof BasicTargetData) {
            return Objects.equals(((BasicTargetData) other).position, position);
        }
        return false;
    }
}
