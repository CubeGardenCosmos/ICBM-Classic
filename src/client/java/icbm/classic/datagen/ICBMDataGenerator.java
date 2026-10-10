package icbm.classic.datagen;

import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Data generation entrypoint (S2.06).
 * <p>
 * The mod entrypoint runs before the data generation (fabric-api calls the {@code fabric-datagen}
 * entrypoints from inside the client start up, loader invokes {@code main} entrypoints first), so
 * {@code ModBlocks}/{@code ModItems} are already filled when the providers run. The guard below turns a
 * change of that order into a clear error instead of an empty asset tree.
 */
public class ICBMDataGenerator implements DataGeneratorEntrypoint
{
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator)
    {
        if (ModBlocks.CONCRETE == null || ModItems.MISSILE_MODULE == null)
        {
            throw new IllegalStateException("ICBM content is not registered, the main entrypoint did not run "
                    + "before data generation, so the model providers have nothing to generate");
        }
        final FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ICBMModelProvider::new);
        pack.addProvider(ICBMLanguageProvider::new);
        pack.addProvider(ICBMBlockLootProvider::new);
        pack.addProvider(ICBMRecipeProvider::new);
        // the item tag provider copies from the block tags, so it needs the block provider instance
        final ICBMBlockTagProvider blockTags = pack.addProvider(ICBMBlockTagProvider::new);
        pack.addProvider((output, registries) -> new ICBMItemTagProvider(output, registries, blockTags));
    }
}
