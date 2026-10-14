package icbm.classic.content.missile.logic.targeting;

import icbm.classic.ICBMConstants;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor
public class BallisticTargetingData extends BasicTargetData {
    public static final Identifier REG_NAME = ICBMConstants.id("ballistic");

    public static final String NBT_IMPACT_HEIGHT = "impact_height";
    /**
     * Height above target to detonate
     */
    private double impactHeightOffset;

    public BallisticTargetingData(Vec3 position, int impactHeightOffset) {
        super(position);
        this.impactHeightOffset = impactHeightOffset;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey()
    {
        return REG_NAME;
    }

    public double getImpactHeightOffset() {
        return impactHeightOffset;
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        out.putDouble(NBT_IMPACT_HEIGHT, impactHeightOffset);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        impactHeightOffset = in.getDoubleOr(NBT_IMPACT_HEIGHT, 0);
    }
}
