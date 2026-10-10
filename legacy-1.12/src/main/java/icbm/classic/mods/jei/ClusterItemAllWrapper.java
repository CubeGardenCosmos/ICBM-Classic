package icbm.classic.mods.jei;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.ICapabilityMissileStack;
import icbm.classic.content.cluster.missile.CapabilityClusterMissileStack;
import lombok.AllArgsConstructor;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ClusterItemAllWrapper implements ICraftingRecipeWrapper {

    private final ItemStack output;
    private final List<ItemStack> inputs;
    private boolean random;
    private final ResourceLocation regName;

    private List<ItemStack> copyShuffle(List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>(stacks);
        Collections.shuffle(copy);
        return copy;
    }

    @Override
    public void getIngredients(IIngredients iIngredients) {

        if(random) {
            iIngredients.setInputs(ItemStack.class, Arrays.asList(
                output, inputs, copyShuffle(inputs),
                copyShuffle(inputs), copyShuffle(inputs), copyShuffle(inputs),
                copyShuffle(inputs), copyShuffle(inputs), copyShuffle(inputs)
            ));
        }
        else {
            iIngredients.setInputs(ItemStack.class, Arrays.asList(
                output, inputs, inputs,
                inputs, inputs, inputs,
                inputs, inputs, inputs
            ));
        }
        iIngredients.setOutput(ItemStack.class, Collections.singletonList(output));
    }

    @Override
    @Nullable
    public ResourceLocation getRegistryName() {
        return regName;
    }
}
