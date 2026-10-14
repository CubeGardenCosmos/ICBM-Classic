package icbm.classic.test.reg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import icbm.classic.ICBMClassic;
import icbm.classic.ICBMConstants;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Covers S2.10: the two English language sources (datagen names + migrated hand written text) are merged
 * into the single {@code en_us.json} of the jar and it covers every registered block and item.
 */
public class LangGameTest
{
    @GameTest(maxTicks = 40)
    public void mergedEnglishCoversTheContent(GameTestHelper helper)
    {
        final JsonObject lang = read(helper);
        int blocks = 0;
        int items = 0;

        for (Identifier block : BuiltInRegistries.BLOCK.keySet())
        {
            if (block.getNamespace().equals(ICBMConstants.DOMAIN))
            {
                helper.assertTrue(lang.has("block." + ICBMConstants.DOMAIN + "." + block.getPath()),
                        "missing block name for " + block);
                blocks++;
            }
        }
        for (Identifier item : BuiltInRegistries.ITEM.keySet())
        {
            if (!item.getNamespace().equals(ICBMConstants.DOMAIN))
            {
                continue;
            }
            //Block items use the block description id (RegHelper#blockItem)
            final String prefix = BuiltInRegistries.BLOCK.containsKey(item) ? "block." : "item.";
            helper.assertTrue(lang.has(prefix + ICBMConstants.DOMAIN + "." + item.getPath()),
                    "missing " + prefix + " name for " + item);
            items++;
        }

        //32 S2.03/S2.05 blocks plus the two radioactive blocks of S3.08, and their two block items on top of
        //the 115 items S2.03-S2.05 registered, plus anti_ballistic_missile of S4.04, plus cluster_missile,
        //held_item_missile, parachute, balloon of S4.06
        helper.assertTrue(blocks == 34, "expected 34 icbmclassic blocks but checked " + blocks);
        helper.assertTrue(items == 122, "expected 122 icbmclassic items but checked " + items);
        helper.assertTrue(lang.has("itemGroup.icbmclassic.main"), "missing creative tab title");
        helper.assertTrue(lang.has("subtitles.icbmclassic.explosion"), "missing a S2.09 subtitle");
        helper.assertTrue(lang.has("icbmclassic.form.missile"), "missing a form template");
        helper.assertTrue(lang.has("icbmclassic.message.gps.set.all"), "missing migrated hand written text");

        ICBMClassic.LOGGER.info("S2.10 lang: {} blocks and {} items named in the merged en_us.json ({} keys)",
                blocks, items, lang.size());
        helper.succeed();
    }

    private static JsonObject read(GameTestHelper helper)
    {
        final String resource = "assets/" + ICBMConstants.DOMAIN + "/lang/en_us.json";
        try (InputStream stream = LangGameTest.class.getClassLoader().getResourceAsStream(resource))
        {
            helper.assertTrue(stream != null, "missing " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        catch (Exception e)
        {
            helper.fail("failed to read " + resource + ": " + e);
            return null;
        }
    }
}
