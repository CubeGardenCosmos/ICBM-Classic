package icbm.classic.content.cluster.missile;

import icbm.classic.content.cluster.bomblet.ItemBombDroplet;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModRecipes;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.data.ExplosiveSettings.ClusterSettings;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeCluster extends CustomRecipe
{
    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        ItemStack cluster = ItemStack.EMPTY;
        int bombletCount = 0;
        Identifier bombletType = null;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (stack.is(ModItems.CLUSTER_MISSILE))
            {
                if (!cluster.isEmpty())
                {
                    return false;
                }
                cluster = stack;
            }
            else if (stack.getItem() instanceof ItemBombDroplet bomblet)
            {
                if (bombletType == null)
                {
                    bombletType = bomblet.getExplosiveId();
                }
                else if (!bombletType.equals(bomblet.getExplosiveId()))
                {
                    return false;
                }
                bombletCount += stack.getCount();
            }
            else
            {
                return false;
            }
        }

        return !cluster.isEmpty() && bombletCount > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack cluster = ItemStack.EMPTY;
        int bombletCount = 0;
        Identifier bombletType = null;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (stack.is(ModItems.CLUSTER_MISSILE))
            {
                cluster = stack;
            }
            else if (stack.getItem() instanceof ItemBombDroplet bomblet)
            {
                bombletType = bomblet.getExplosiveId();
                bombletCount += stack.getCount();
            }
        }

        if (cluster.isEmpty() || bombletType == null)
        {
            return ItemStack.EMPTY;
        }

        final ItemStack result = cluster.copyWithCount(1);
        final ExplosiveSettings existing = result.get(ModDataComponents.EXPLOSIVE_SETTINGS);
        int finalCount = bombletCount;
        if (existing != null && existing.cluster().isPresent())
        {
            final ClusterSettings current = existing.cluster().get();
            if (current.bombletExplosive().equals(bombletType))
            {
                finalCount = Math.min(100, current.count() + bombletCount);
            }
        }

        result.set(ModDataComponents.EXPLOSIVE_SETTINGS, new ExplosiveSettings(new ClusterSettings(bombletType, finalCount)));
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer()
    {
        return ModRecipes.RECIPE_CLUSTER_SERIALIZER;
    }
}
