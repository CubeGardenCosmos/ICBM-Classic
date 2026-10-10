package icbm.classic.datagen;

import icbm.classic.content.reg.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/**
 * Block loot tables of the registered blocks (S2.07).
 * <p>
 * 1.12 left the drops alone: {@code legacy-1.12/src/main/java/icbm/classic/content/blocks} has no
 * {@code getDrops}/{@code getItemDropped}/{@code quantityDropped} override. The only drop related method is
 * {@code BlockReinforcedGlass#canSilkHarvest()}, and 1.12 answers {@code canSilkHarvest()} with {@code true}
 * for every block by default, so the reinforced glass drops itself without silk touch as well. Every block
 * therefore drops itself.
 * <p>
 * The machines (S5) and the radioactive blocks (S3.08) get their tables when they are registered.
 */
public class ICBMBlockLootProvider extends FabricBlockLootSubProvider
{
    public ICBMBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public void generate()
    {
        dropSelf(ModBlocks.CONCRETE);
        dropSelf(ModBlocks.COMPACT_CONCRETE);
        dropSelf(ModBlocks.REINFORCED_CONCRETE);
        dropSelf(ModBlocks.REINFORCED_GLASS);
        dropSelf(ModBlocks.SPIKES);
        dropSelf(ModBlocks.POISON_SPIKES);
        dropSelf(ModBlocks.FIRE_SPIKES);
        dropSelf(ModBlocks.GLASS_BUTTON);
        dropSelf(ModBlocks.GLASS_PRESSURE_PLATE);
        ModBlocks.EXPLOSIVE_BLOCKS.values().forEach(this::dropSelf);
    }
}
