package icbm.classic.test.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

/**
 * Covers the S2.08 recipes: the generated recipes are loaded by the server and their ingredients resolve,
 * including the {@code c:} tags from S2.07.
 */
public class RecipeGameTest
{
    private static final List<String> RECIPES = List.of(
            // 1.12 recipes/*.json
            "concrete", "compact_concrete", "reinforced_concrete", "reinforced_glass",
            "spikes", "poison_spikes", "fire_spikes", "glass_button", "glass_pressure_plate",
            "antidote", "poison_powder",
            // 1.12 recipes/parts/**, bomblet/, missile/
            "steel_clump", "steel_plate", "basic_circuit", "advanced_circuit", "elite_circuit",
            "copper_wire", "gold_wire", "gunpowder", "saltpeter_ball", "empty_bomblet", "missile_module",
            // 1.12 recipes/explosives/**, plus the flat forms of one explosive per form
            "condensed_explosive", "anvil_explosive", "nuclear_explosive", "antimatter_explosive", "redmatter_explosive",
            "condensed_missile", "nuclear_missile", "anvil_grenade", "nuclear_bomb_cart", "anvil_bomblet");

    @GameTest(maxTicks = 40)
    public void recipesAreLoadedAndMatch(GameTestHelper helper)
    {
        final RecipeManager manager = helper.getLevel().getServer().getRecipeManager();

        for (String name : RECIPES)
        {
            helper.assertTrue(manager.byKey(recipeKey(name)).isPresent(), "missing recipe icbmclassic:" + name);
        }

        // 2x2 of steel ingots: only matches through the c:ingots/steel tag (S2.07)
        assertCrafts(helper, manager, "steel_plate", 2, 2,
                List.of(new ItemStack(ModItems.STEEL_INGOT), new ItemStack(ModItems.STEEL_INGOT),
                        new ItemStack(ModItems.STEEL_INGOT), new ItemStack(ModItems.STEEL_INGOT)),
                ModItems.STEEL_PLATE, 4);

        // shapeless: coal + sulfur dust + saltpeter, again tag driven for the two dusts
        assertCrafts(helper, manager, "gunpowder", 2, 2,
                List.of(new ItemStack(Items.COAL), new ItemStack(ModItems.SULFUR_DUST),
                        new ItemStack(ModItems.SALTPETER), ItemStack.EMPTY),
                Items.GUNPOWDER, 3);

        helper.succeed();
    }

    /** Builds the input, checks that the named recipe is the one that matches and what it crafts. */
    private static void assertCrafts(GameTestHelper helper, RecipeManager manager, String name, int width, int height,
            List<ItemStack> stacks, net.minecraft.world.level.ItemLike expected, int count)
    {
        final CraftingInput input = CraftingInput.of(width, height, stacks);
        final Optional<RecipeHolder<CraftingRecipe>> found = manager.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(found.isPresent(), "no crafting recipe matches the ingredients of " + name);
        helper.assertTrue(found.get().id().identifier().equals(ICBMConstants.id(name)),
                "expected " + name + " to match, but " + found.get().id().identifier() + " did");
        final ItemStack result = found.get().value().assemble(input);
        helper.assertTrue(result.getItem() == expected.asItem() && result.getCount() == count,
                name + " should craft " + count + " " + expected.asItem() + " but crafted " + result);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String name)
    {
        return ResourceKey.create(Registries.RECIPE, ICBMConstants.id(name));
    }
}
