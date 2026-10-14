package icbm.classic.content.reg;

import com.mojang.serialization.MapCodec;
import icbm.classic.ICBMConstants;
import icbm.classic.content.cargo.RecipeCargoData;
import icbm.classic.content.cargo.RecipeCargoDataDecraft;
import icbm.classic.content.cluster.missile.RecipeCluster;
import icbm.classic.content.missile.entity.itemstack.item.RecipeHeldItemMissile;
import icbm.classic.content.missile.entity.itemstack.item.RecipeHeldItemMissileDecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

public final class ModRecipes
{
    public static RecipeSerializer<RecipeCluster> RECIPE_CLUSTER_SERIALIZER;
    public static RecipeSerializer<RecipeHeldItemMissile> RECIPE_HELD_ITEM_MISSILE_SERIALIZER;
    public static RecipeSerializer<RecipeHeldItemMissileDecraft> RECIPE_HELD_ITEM_MISSILE_DECRAFT_SERIALIZER;
    public static RecipeSerializer<RecipeCargoData> RECIPE_CARGO_DATA_SERIALIZER;
    public static RecipeSerializer<RecipeCargoDataDecraft> RECIPE_CARGO_DATA_DECRAFT_SERIALIZER;

    private ModRecipes() {}

    public static void register()
    {
        RECIPE_CLUSTER_SERIALIZER = register("recipe_cluster", RecipeCluster::new);
        RECIPE_HELD_ITEM_MISSILE_SERIALIZER = register("recipe_held_item_missile", RecipeHeldItemMissile::new);
        RECIPE_HELD_ITEM_MISSILE_DECRAFT_SERIALIZER = register("recipe_held_item_missile_decraft", RecipeHeldItemMissileDecraft::new);
        RECIPE_CARGO_DATA_SERIALIZER = register("recipe_cargo_data", RecipeCargoData::new);
        RECIPE_CARGO_DATA_DECRAFT_SERIALIZER = register("recipe_cargo_data_decraft", RecipeCargoDataDecraft::new);
    }

    private static <T extends CustomRecipe> RecipeSerializer<T> register(String name, Supplier<T> factory)
    {
        final T instance = factory.get();
        final MapCodec<T> mapCodec = MapCodec.unit(instance);
        final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec = StreamCodec.unit(instance);
        final RecipeSerializer<T> serializer = new RecipeSerializer<>(mapCodec, streamCodec);
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ICBMConstants.id(name), serializer);
    }
}
