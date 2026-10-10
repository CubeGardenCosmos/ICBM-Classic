package icbm.classic.datagen;

import icbm.classic.content.reg.RegHelper;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

/**
 * Item tags of the registered items (S2.07): the 1.12 OreDictionary names become {@code c:} convention tags
 * (registry-ids.md §3).
 * <p>
 * {@code c:ingots/copper} is the same tag as {@code ConventionalItemTags.COPPER_INGOTS}; fabric has no
 * constants for the steel ingot, the plates and the two dusts, so those are written as {@code c:} tags here.
 */
public class ICBMItemTagProvider extends FabricTagsProvider.ItemTagsProvider
{
    public ICBMItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries,
            FabricTagsProvider.BlockTagsProvider blockTags)
    {
        super(output, registries, blockTags);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries)
    {
        // 1.12: OreDictionary.registerOre("ingotSteel", ...) and friends
        builder(common("ingots/steel")).add(RegHelper.itemKey("steel_ingot"));
        builder(common("ingots/copper")).add(RegHelper.itemKey("copper_ingot"));
        builder(common("plates/steel")).add(RegHelper.itemKey("steel_plate"));
        builder(common("plates/iron")).add(RegHelper.itemKey("iron_plate"));
        builder(common("dusts/sulfur")).add(RegHelper.itemKey("sulfur_dust"));
        builder(common("dusts/saltpeter")).add(RegHelper.itemKey("saltpeter"));
    }

    /** A {@code c:} (common) item tag, e.g. {@code c:ingots/steel}. */
    private static TagKey<Item> common(String path)
    {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
