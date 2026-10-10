package icbm.classic.content.actions.conditionals;

import icbm.classic.ICBMConstants;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.conditions.IConditionCause;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.actions.status.MissingFieldStatus;
import icbm.classic.lib.saving.NbtSaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;

public class ConditionTargetDistance implements ICondition, IConditionCause, INBTSerializable<NBTTagCompound> {
    public static final ResourceLocation REG_NAME = new ResourceLocation(ICBMConstants.DOMAIN, "projectile.target.distance");
    private IActionFieldProvider provider;

    @Getter
    @Setter
    @Accessors(chain = true)
    private double triggerDistance = 10;

    @Getter
    @Setter
    @Accessors(chain = true)
    private boolean inverted = false;

    private double currentDistance = Double.MIN_VALUE;

    @Override
    public void init(IActionFieldProvider provider) {
        this.provider = provider;
    }

    @Override
    public void onTick()
    {
        if(provider.hasField(ActionFields.TARGET_POSITION) && provider.hasField(ActionFields.HOST_POSITION)) {
            final Vec3d value = provider.getValue(ActionFields.TARGET_POSITION);
            final Vec3d host = provider.getValue(ActionFields.HOST_POSITION);
            if(value != null && host != null) {
                currentDistance = host.distanceTo(value);
                //TODO add a manhatten distance version for faster calculations
            }
        }
    }

    @Override
    public IActionStatus getCondition() {
        if(provider == null) {
            return new MissingFieldStatus().setSource("ConditionalImpact").setField("provider");
        }
        else if(!provider.hasField(ActionFields.TARGET_POSITION)) {
            return new MissingFieldStatus().setSource("ConditionalImpact#provider").setField(ActionFields.TARGET_POSITION.getKey());
        }
        else if(!provider.hasField(ActionFields.HOST_POSITION)) {
            return new MissingFieldStatus().setSource("ConditionalImpact#provider").setField(ActionFields.HOST_POSITION.getKey());
        }

        if(!inverted && currentDistance >= triggerDistance || inverted && currentDistance <= triggerDistance) {
            return ActionResponses.WAITING;
        }
        return ActionResponses.READY;
    }

    @Nonnull
    @Override
    public ResourceLocation getRegistryKey() {
        return REG_NAME;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return SAVE_LOGIC.save(this);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        SAVE_LOGIC.load(this, nbt);
    }

    private static final NbtSaveHandler<ConditionTargetDistance> SAVE_LOGIC = new NbtSaveHandler<ConditionTargetDistance>()
        .mainRoot()
        /* */.nodeDouble("trigger_distance", ConditionTargetDistance::getTriggerDistance, ConditionTargetDistance::setTriggerDistance)
        /* */.nodeBoolean("invert", ConditionTargetDistance::isInverted, ConditionTargetDistance::setInverted)
        .base();

    @Override
    public IActionCause getCause() {
        return null; // TODO implement
    }
}
