package icbm.classic.content.cluster.missile;

import icbm.classic.api.EnumTier;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.missile.ConfigMissile;
import icbm.classic.content.cluster.bomblet.ItemBombDroplet;
import icbm.classic.content.items.ItemMissile;
import icbm.classic.content.reg.ModItems;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ClusterMissileHandler
{
    public static int sizeOf(ItemStack itemStack)
    {
        if (itemStack.isEmpty())
        {
            return ConfigMissile.CLUSTER_MISSILE.ITEM_SIZES.DEFAULT_SIZE;
        }

        if (itemStack.getItem() instanceof ItemBombDroplet)
        {
            final IExplosive explosive = ICBMClassicHelpers.getExplosive(itemStack);
            if (explosive != null)
            {
                final IExplosiveData data = explosive.getExplosiveData();
                if (data != null)
                {
                    final EnumTier tier = data.getTier();
                    return switch (tier)
                    {
                        case TWO -> 5;
                        case THREE -> 10;
                        case FOUR -> 20;
                        default -> 2;
                    };
                }
            }
            return 2;
        }

        if (itemStack.is(ModItems.PARACHUTE) || itemStack.is(ModItems.BALLOON))
        {
            return 2;
        }

        if (itemStack.is(ModItems.CLUSTER_MISSILE) || itemStack.getItem() instanceof ItemMissile)
        {
            return 20;
        }

        if (itemStack.is(ModItems.ANTI_BALLISTIC_MISSILE) || itemStack.is(ModItems.HELD_ITEM_MISSILE))
        {
            return 10;
        }

        return ConfigMissile.CLUSTER_MISSILE.ITEM_SIZES.DEFAULT_SIZE;
    }

    public static boolean isAllowed(ItemStack itemStack)
    {
        if (itemStack.isEmpty())
        {
            return false;
        }
        // By default all non-empty items are allowed unless banned by config
        return true;
    }
}
