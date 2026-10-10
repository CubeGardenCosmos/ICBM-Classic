package icbm.classic.content.cluster.missile;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.actions.conditionals.ConditionAnd;
import icbm.classic.content.actions.conditionals.ConditionTargetDistance;
import icbm.classic.content.cluster.action.ActionDataCluster;
import icbm.classic.content.actions.conditionals.ConditionalImpact;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.lib.saving.NbtSaveHandler;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

public class CapabilityClusterMissileStack implements ICapabilityMissileStack, INBTSerializable<NBTTagCompound> {
    private final ItemStack stack;

    @Getter
    private final ActionDataCluster actionDataCluster = new ActionDataCluster();

    public CapabilityClusterMissileStack(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public String getMissileId() {
        return ICBMConstants.PREFIX + "missile[cluster]";
    }

    @Override
    public IMissile newMissile(World world)
    {
        final EntityMissileActionable missile = new EntityMissileActionable(world)
            .setOriginalStack(stack)
            .setActionData(actionDataCluster)
            .initHealth(ConfigMissile.CLUSTER_MISSILE.MAX_HEALTH);

        final ConditionAnd conditionAnd = new ConditionAnd();

        // Disable impact trigger
        conditionAnd.getConditions().add(new ConditionalImpact().setImpactDesired(false));

        // Trigger when near target
        conditionAnd.getConditions().add(new ConditionTargetDistance().setTriggerDistance(3)); //TODO pull from targetData?

        missile.getMainAction().withCondition(conditionAnd);

        return missile.getMissileCapability();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        return SAVE_LOGIC.save(this);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        SAVE_LOGIC.load(this, nbt);
    }

    private static final NbtSaveHandler<CapabilityClusterMissileStack> SAVE_LOGIC = new NbtSaveHandler<CapabilityClusterMissileStack>()
        .mainRoot()
        /* */.nodeINBTSerializable("cluster_action", CapabilityClusterMissileStack::getActionDataCluster)
        .base();

}
