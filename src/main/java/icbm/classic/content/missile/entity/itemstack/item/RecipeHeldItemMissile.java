package icbm.classic.content.missile.entity.itemstack.item;

import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModItems;
import icbm.classic.content.reg.ModRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class RecipeHeldItemMissile extends CustomRecipe
{
    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        if (input.ingredientCount() != 2)
        {
            return false;
        }

        ItemStack missile = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (stack.is(ModItems.HELD_ITEM_MISSILE))
            {
                if (!missile.isEmpty())
                {
                    return false;
                }
                // Missile must not already have a held item
                final ItemStack existing = stack.get(ModDataComponents.HELD_ITEM);
                if (existing != null && !existing.isEmpty())
                {
                    return false;
                }
                missile = stack;
            }
            else if (HeldItemMissileHandler.isAllowed(stack))
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

        return !missile.isEmpty() && !cargo.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        ItemStack missile = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++)
        {
            final ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (stack.is(ModItems.HELD_ITEM_MISSILE))
            {
                missile = stack;
            }
            else if (HeldItemMissileHandler.isAllowed(stack))
            {
                cargo = stack;
            }
        }

        if (missile.isEmpty() || cargo.isEmpty())
        {
            return ItemStack.EMPTY;
        }

        final ItemStack result = missile.copyWithCount(1);
        result.set(ModDataComponents.HELD_ITEM, cargo.copyWithCount(1));
        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer()
    {
        return ModRecipes.RECIPE_HELD_ITEM_MISSILE_SERIALIZER;
    }
}
