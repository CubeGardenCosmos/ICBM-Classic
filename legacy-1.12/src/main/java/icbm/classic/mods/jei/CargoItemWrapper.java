package icbm.classic.mods.jei;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileStack;
import icbm.classic.content.cargo.CargoProjectileData;
import icbm.classic.content.cargo.balloon.BalloonProjectileData;
import icbm.classic.content.cargo.parachute.ParachuteProjectileData;
import icbm.classic.content.reg.ItemReg;
import icbm.classic.lib.projectile.ProjectileStack;
import lombok.AllArgsConstructor;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import mezz.jei.api.recipe.wrapper.ICraftingRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CargoItemWrapper implements ICraftingRecipeWrapper {

    private final ItemStack output;
    private final ItemStack input;
    private final ResourceLocation regName;

    @Override
    public void getIngredients(IIngredients iIngredients) {

        iIngredients.setInputs(ItemStack.class, Arrays.asList(output, input));

        final ItemStack out = output.copy();
        final IProjectileStack projectileStack = out.getCapability(ICBMClassicAPI.PROJECTILE_STACK_CAPABILITY, null);
        if(projectileStack instanceof ProjectileStack) {
            CargoProjectileData data;
            if(output.getItem() == ItemReg.itemBalloon) {
                data = new BalloonProjectileData();
            }
            else {
                data = new ParachuteProjectileData();
            }
            ((ProjectileStack) projectileStack).setProjectileData(data);
            ((CargoProjectileData<?, ?>) data).setHeldItem(input.copy());
        }
        iIngredients.setOutput(ItemStack.class, Collections.singletonList(out));
    }

    @Override
    @Nullable
    public ResourceLocation getRegistryName() {
        return regName;
    }
}
