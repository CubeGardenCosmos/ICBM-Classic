package icbm.classic.test.reg;

import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Covers the S2.06 assets: every registered block has a block state and every registered item an
 * {@code items/<id>.json} model definition, so no registered content renders as a missing model.
 * <p>
 * The files are looked up through the class loader: the GameTest runs on the server and the client assets
 * are not part of any loaded resource pack there, but the generated files sit on the classpath.
 */
public class AssetsGameTest
{
    @GameTest
    public void everyRegisteredBlockAndItemHasAssets(GameTestHelper helper)
    {
        final List<String> missing = new ArrayList<>();

        int blocks = 0;
        for (Block block : BuiltInRegistries.BLOCK)
        {
            final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!ICBMConstants.DOMAIN.equals(id.getNamespace()))
            {
                continue;
            }
            blocks++;
            if (!exists("blockstates/" + id.getPath() + ".json"))
            {
                missing.add("blockstates/" + id.getPath() + ".json");
            }
        }

        int items = 0;
        for (Item item : BuiltInRegistries.ITEM)
        {
            final Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!ICBMConstants.DOMAIN.equals(id.getNamespace()))
            {
                continue;
            }
            items++;
            if (!exists("items/" + id.getPath() + ".json"))
            {
                missing.add("items/" + id.getPath() + ".json");
            }
        }

        ICBMClassic.LOGGER.info("S2.06 assets: {} icbmclassic blocks and {} icbmclassic items checked", blocks, items);
        helper.assertTrue(blocks > 0 && items > 0, "expected registered icbmclassic content, got "
                + blocks + " blocks and " + items + " items");
        helper.assertTrue(missing.isEmpty(), "missing asset files: " + missing);
        helper.succeed();
    }

    private static boolean exists(String path)
    {
        final String resource = "assets/" + ICBMConstants.DOMAIN + "/" + path;
        return AssetsGameTest.class.getClassLoader().getResource(resource) != null;
    }
}
