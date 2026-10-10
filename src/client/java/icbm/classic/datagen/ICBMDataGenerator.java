package icbm.classic.datagen;

import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

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
        pack.addProvider(ICBMBlockLootProvider::new);
        pack.addProvider(ICBMRecipeProvider::new);
        final ICBMBlockTagProvider blockTags = pack.addProvider(ICBMBlockTagProvider::new);
        pack.addProvider((output, registries) -> new ICBMItemTagProvider(output, registries, blockTags));
    }
}
