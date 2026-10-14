package icbm.classic.content.cargo;

import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeCargoData extends CustomRecipe
{
    private static boolean isCargoHolder(ItemStack stack)
    {
        return stack.is(ModItems.PARACHUTE) || stack.is(ModItems.BALLOON);
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        if (input.ingredientCount() != 2)
        {
            return false;
        }

        ItemStack holder = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (isCargoHolder(stack))
            {
                if (!holder.isEmpty())
                {
                    return false;
                }
                final ItemStack existing = stack.get(ModDataComponents.HELD_ITEM);
                if (existing != null && !existing.isEmpty())
                {
                    return false;
                }
                holder = stack;
            }
            else if (CargoHolderHandler.isAllowed(stack))
            {
                if (!cargo.isEmpty())
                {
                    return false;
                }
                cargo = stack;
            }
            else
            {
                return false;
            }
        }

        return !holder.isEmpty() && !cargo.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack holder = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (isCargoHolder(stack))
            {
                holder = stack;
            }
            else if (CargoHolderHandler.isAllowed(stack))
            {
                cargo = stack;
            }
        }

        if (holder.isEmpty() || cargo.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        final ItemStack result = holder.copyWithCount(1);
        result.set(ModDataComponents.HELD_ITEM, cargo.copyWithCount(1));
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer()
    {
        return ModRecipes.RECIPE_CARGO_DATA_SERIALIZER;
    }
}
