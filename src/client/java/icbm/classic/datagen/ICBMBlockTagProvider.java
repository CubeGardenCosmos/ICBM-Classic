package icbm.classic.datagen;

import icbm.classic.content.reg.RegHelper;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;

import java.util.concurrent.CompletableFuture;

/**
 * Block mining tags of the registered blocks (S2.07).
 * <p>
 * 1.12 harvest levels: {@code grep -rn "setHarvestLevel" legacy-1.12/src/main/java} finds nothing, no content
 * block ever set one, so no {@code minecraft:needs_iron_tool}/{@code needs_stone_tool} tag is generated
 * (step card S2.07 point 2).
 * <p>
 * Pickaxe blocks are the 1.12 {@code Material.ROCK} concrete, the {@code Material.IRON} spikes and the
 * vanilla button/pressure plate behaviour of the glass button and plate. The reinforced glass
 * ({@code Material.GLASS}) and the flat explosive blocks ({@code Material.TNT}) stay without a tool tag,
 * exactly like vanilla glass and TNT.
 */
public class ICBMBlockTagProvider extends FabricTagsProvider.BlockTagsProvider
{
    public ICBMBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries)
    {
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(
                RegHelper.blockKey("concrete"),
                RegHelper.blockKey("compact_concrete"),
                RegHelper.blockKey("reinforced_concrete"),
                RegHelper.blockKey("spikes"),
                RegHelper.blockKey("poison_spikes"),
                RegHelper.blockKey("fire_spikes"),
                RegHelper.blockKey("glass_button"),
                RegHelper.blockKey("glass_pressure_plate"));
    }
}
