package icbm.classic.test.reg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import icbm.classic.ICBMClassic;
import icbm.classic.content.reg.ModSounds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Covers S2.09: every {@link ModSounds} field is registered, has a {@code sounds.json} entry with a subtitle
 * and every referenced {@code .ogg} exists; every subtitle key exists in {@code en_us.json}.
 */
public class SoundGameTest
{
    /** Field name -> sound id (the 1.12 sounds.json key, snake_cased). */
    private static final Map<String, String> SOUNDS = new LinkedHashMap<>();

    static
    {
        SOUNDS.put("ANTIMATTER", "antimatter");
        SOUNDS.put("BEAM_CHARGING", "beam_charging");
        SOUNDS.put("COLLAPSE", "collapse");
        SOUNDS.put("DEBILITATION", "debilitation");
        SOUNDS.put("EMP", "emp");
        SOUNDS.put("EXPLOSION", "explosion");
        SOUNDS.put("EXPLOSION_FIRE", "explosion_fire");
        SOUNDS.put("GAS_LEAK", "gas_leak");
        SOUNDS.put("MACHINE_HUM", "machine_hum");
        SOUNDS.put("MISSILE_ENGINE", "missile_engine");
        SOUNDS.put("MISSILE_LAUNCH", "missile_launch");
        SOUNDS.put("POWER_DOWN", "power_down");
        SOUNDS.put("REDMATTER", "redmatter");
        SOUNDS.put("SONICWAVE", "sonic_wave");
        SOUNDS.put("MEEP", "meep");
        SOUNDS.put("RADIOACTIVE_TICK", "radioactive_tick");
    }

    @GameTest(maxTicks = 40)
    public void soundsAreRegisteredAndReferencable(GameTestHelper helper)
    {
        final JsonObject soundsJson = readJson(helper, "assets/icbmclassic/sounds.json").getAsJsonObject();
        final JsonObject langJson = readJson(helper, "assets/icbmclassic/lang/en_us.json").getAsJsonObject();

        int checked = 0;
        for (Map.Entry<String, String> entry : SOUNDS.entrySet())
        {
            final SoundEvent event = field(helper, entry.getKey());
            helper.assertTrue(event != null, "ModSounds." + entry.getKey() + " was not registered");

            final Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(event);
            helper.assertTrue(key != null, "ModSounds." + entry.getKey() + " is missing from the sound event registry");
            helper.assertTrue(key.equals(Identifier.fromNamespaceAndPath("icbmclassic", entry.getValue())),
                    "ModSounds." + entry.getKey() + " is registered as " + key);

            final JsonElement registration = soundsJson.get(entry.getValue());
            helper.assertTrue(registration != null, "sounds.json has no entry for " + entry.getValue());
            final JsonObject registrationObject = registration.getAsJsonObject();

            final JsonElement subtitle = registrationObject.get("subtitle");
            helper.assertTrue(subtitle != null, "sounds.json entry " + entry.getValue() + " has no subtitle");
            final String subtitleKey = subtitle.getAsString();
            helper.assertTrue(subtitleKey.equals("subtitles.icbmclassic." + entry.getValue()),
                    "sounds.json entry " + entry.getValue() + " has subtitle " + subtitleKey);
            helper.assertTrue(langJson.has(subtitleKey), "en_us.json has no text for " + subtitleKey);

            final JsonArray files = registrationObject.getAsJsonArray("sounds");
            helper.assertTrue(files != null && !files.isEmpty(), "sounds.json entry " + entry.getValue() + " has no files");
            for (JsonElement file : files)
            {
                final Identifier sound = Identifier.parse(file.getAsString());
                // Sound#getPath resolves a sound name to assets/<namespace>/sounds/<path>.ogg
                final String resource = "assets/" + sound.getNamespace() + "/sounds/" + sound.getPath() + ".ogg";
                final byte[] header = new byte[4];
                try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource))
                {
                    helper.assertTrue(stream != null, "missing sound file for " + entry.getValue() + ": " + resource);
                    helper.assertTrue(stream.read(header) == 4 && new String(header, StandardCharsets.US_ASCII).equals("OggS"),
                            resource + " is not an ogg file");
                }
                catch (Exception e)
                {
                    helper.fail("failed to read " + resource + ": " + e);
                }
            }
            checked++;
        }

        // every SoundEvent field of ModSounds must be in the map above, so a new field cannot be forgotten here
        int fields = 0;
        for (Field f : ModSounds.class.getDeclaredFields())
        {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == SoundEvent.class)
            {
                fields++;
                helper.assertTrue(SOUNDS.containsKey(f.getName()), "ModSounds." + f.getName() + " is not covered by this test");
            }
        }
        helper.assertTrue(fields == SOUNDS.size(), "expected " + SOUNDS.size() + " ModSounds fields but found " + fields);

        ICBMClassic.LOGGER.info("S2.09 sounds: {} sound events checked", checked);
        helper.succeed();
    }

    private static SoundEvent field(GameTestHelper helper, String name)
    {
        try
        {
            return (SoundEvent) ModSounds.class.getField(name).get(null);
        }
        catch (ReflectiveOperationException e)
        {
            helper.fail("ModSounds." + name + " is missing: " + e);
            return null;
        }
    }

    private static JsonElement readJson(GameTestHelper helper, String resource)
    {
        try (InputStream stream = SoundGameTest.class.getClassLoader().getResourceAsStream(resource))
        {
            helper.assertTrue(stream != null, "missing resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            helper.fail("failed to read " + resource + ": " + e);
            return null;
        }
    }
}
