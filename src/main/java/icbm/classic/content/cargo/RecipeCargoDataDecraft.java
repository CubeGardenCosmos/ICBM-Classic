package icbm.classic.content.cargo;

import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeCargoDataDecraft extends CustomRecipe
{
    private static boolean isCargoHolder(ItemStack stack)
    {
        return stack.is(ModItems.PARACHUTE) || stack.is(ModItems.BALLOON);
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        if (input.ingredientCount() != 1)
        {
            return false;
        }

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty())
            {
                if (isCargoHolder(stack))
                {
                    final ItemStack held = stack.get(ModDataComponents.HELD_ITEM);
                    return held != null && !held.isEmpty();
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && isCargoHolder(stack))
            {
                final ItemStack held = stack.get(ModDataComponents.HELD_ITEM);
                if (held != null && !held.isEmpty())
                {
                    return held.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input)
    {
        final NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && isCargoHolder(stack))
            {
                final ItemStack emptyHolder = new ItemStack(stack.getItem());
                remaining.set(i, emptyHolder);
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer()
    {
        return ModRecipes.RECIPE_CARGO_DATA_DECRAFT_SERIALIZER;
    }
}
