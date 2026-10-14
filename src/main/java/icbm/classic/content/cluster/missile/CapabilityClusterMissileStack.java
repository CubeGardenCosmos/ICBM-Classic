package icbm.classic.content.cluster.missile;

import icbm.classic.ICBMConstants;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.actions.conditionals.ConditionAnd;
import icbm.classic.content.actions.conditionals.ConditionTargetDistance;
import icbm.classic.content.actions.conditionals.ConditionalImpact;
import icbm.classic.content.cluster.action.ActionDataCluster;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.data.ExplosiveSettings.ClusterSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CapabilityClusterMissileStack implements ICapabilityMissileStack
{
    private final ItemStack stack;

    public CapabilityClusterMissileStack(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public String getMissileId()
    {
        return ICBMConstants.PREFIX + "missile[cluster]";
    }

    @Override
    public IMissile newMissile(Level world)
    {
        final EntityMissileActionable missile = new EntityMissileActionable(ModEntities.GENERIC_MISSILE, world);
        missile.setOriginalStack(stack);
        missile.initHealth(ConfigMissile.CLUSTER_MISSILE.MAX_HEALTH);

        final ActionDataCluster actionData = new ActionDataCluster();
        final ExplosiveSettings settings = stack.get(ModDataComponents.EXPLOSIVE_SETTINGS);
        final ClusterSettings cluster = (settings != null && settings.cluster().isPresent())
                ? settings.cluster().get()
                : ClusterSettings.DEFAULT;

        final Item bombletItem = ModItems.EXPLOSIVE_BOMBLETS.get(cluster.bombletExplosive());
        final ItemStack bombletStack = bombletItem != null ? new ItemStack(bombletItem) : new ItemStack(ModItems.EMPTY_BOMBLET);
        for (int i = 0; i < cluster.count(); i++)
        {
            actionData.getClusterSpawnEntries().add(bombletStack.copy());
        }
        missile.setActionData(actionData);

        final ConditionAnd conditionAnd = new ConditionAnd();
        conditionAnd.getConditions().add(new ConditionalImpact().setImpactDesired(false));
        conditionAnd.getConditions().add(new ConditionTargetDistance().setTriggerDistance(3));
        missile.getMainAction().withCondition(conditionAnd);

        return missile.getMissileCapability();
    }
}
