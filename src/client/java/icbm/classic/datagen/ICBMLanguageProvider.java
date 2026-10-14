package icbm.classic.datagen;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ExplosiveForms;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * English names of the flat 26.3 content (i18n.md §2/§4).
 * <p>
 * Only keys that can be derived from the registries are generated: every {@code icbmclassic} block, item
 * and entity type, the {@code itemGroup.icbmclassic.main} title, the explosive base names and the form
 * templates the flat names are composed from. GUI text, messages and tooltips live in the hand written
 * {@code src/main/resources/assets/icbmclassic/lang/en_us.json} (migrated by {@code scripts/migrate_lang.py}).
 * <p>
 * Flat explosives: {@code "<explosive name> <form name>"} (i18n.md §2), e.g. {@code condensed} +
 * {@code MISSILE} -> "Condensed Missile". The base names are the 1.12
 * {@code explosive.icbmclassic:<id>.name} values without the trailing " Explosives".
 * <p>
 * {@link #validate} fails the datagen run when a registered block/item/entity has no name here, so S4/S5/S6
 * cannot forget to name new content.
 */
public class ICBMLanguageProvider extends FabricLanguageProvider
{
    /** Explosive id -> base English name. Every entry of {@link ExplosiveForms#ORDER} must be present. */
    private static final Map<String, String> EXPLOSIVE_NAMES = new LinkedHashMap<>();

    /** Form -> English name used by the flat names and the {@code icbmclassic.form.*} templates. */
    private static final Map<ExplosiveForm, String> FORM_NAMES = new LinkedHashMap<>();

    /** Entity type id -> English name; 1.12 never named its entities in the lang file (S3.03/S4). */
    private static final Map<String, String> ENTITY_NAMES = new LinkedHashMap<>();

    /** Flat id -> English name of the content that is not an explosive form (1.12 ".name" values). */
    private static final Map<String, String> NAMES = new LinkedHashMap<>();

    static
    {
        EXPLOSIVE_NAMES.put("condensed", "Condensed");
        EXPLOSIVE_NAMES.put("shrapnel", "Shrapnel");
        EXPLOSIVE_NAMES.put("incendiary", "Incendiary");
        EXPLOSIVE_NAMES.put("debilitation", "Debilitation");
        EXPLOSIVE_NAMES.put("chemical", "Chemical");
        EXPLOSIVE_NAMES.put("anvil", "Anvil");
        EXPLOSIVE_NAMES.put("repulsive", "Repulsive");
        EXPLOSIVE_NAMES.put("attractive", "Attractive");
        EXPLOSIVE_NAMES.put("colors", "Colorful");
        EXPLOSIVE_NAMES.put("smoke", "Smoke Marking");
        EXPLOSIVE_NAMES.put("fragmentation", "Fragmentation");
        EXPLOSIVE_NAMES.put("contagious", "Contagious");
        EXPLOSIVE_NAMES.put("sonic", "Sonic");
        EXPLOSIVE_NAMES.put("breaching", "Breaching");
        EXPLOSIVE_NAMES.put("thermobaric", "Thermobaric");
        EXPLOSIVE_NAMES.put("nuclear", "Nuclear");
        EXPLOSIVE_NAMES.put("emp", "EMP");
        EXPLOSIVE_NAMES.put("exothermic", "Exothermic");
        EXPLOSIVE_NAMES.put("endothermic", "Endothermic");
        EXPLOSIVE_NAMES.put("antigravitational", "Anti-Gravitational");
        EXPLOSIVE_NAMES.put("ender", "Ender");
        EXPLOSIVE_NAMES.put("antimatter", "Antimatter");
        EXPLOSIVE_NAMES.put("redmatter", "Red Matter");
        // 1.12 had no content (and therefore no name) for these two, the word is derived from the id
        EXPLOSIVE_NAMES.put("rot", "Rot");
        EXPLOSIVE_NAMES.put("mutation", "Mutation");

        FORM_NAMES.put(ExplosiveForm.BLOCK, "Explosive");
        FORM_NAMES.put(ExplosiveForm.MISSILE, "Missile");
        FORM_NAMES.put(ExplosiveForm.GRENADE, "Grenade");
        FORM_NAMES.put(ExplosiveForm.BOMB_CART, "Bomb Cart");
        FORM_NAMES.put(ExplosiveForm.BOMBLET, "Bomblet");

        //Blocks (S2.03)
        NAMES.put("concrete", "Concrete");
        NAMES.put("compact_concrete", "Compact Concrete");
        NAMES.put("reinforced_concrete", "Reinforced Concrete");
        NAMES.put("reinforced_glass", "Reinforced Glass");
        NAMES.put("spikes", "Spikes");
        NAMES.put("poison_spikes", "Poison Spikes");
        NAMES.put("fire_spikes", "Flammable Spikes");
        NAMES.put("glass_button", "Glass Button");
        NAMES.put("glass_pressure_plate", "Glass Pressure Plate");

        //Radioactive blocks (S3.08), the 1.12 "icbmclassic.block.radioactive.<meta>.name" values
        NAMES.put("radioactive_dirt", "Radioactive Dirt");
        NAMES.put("radioactive_stone", "Radioactive Stone");

        //Materials and simple items (S2.04)
        NAMES.put("steel_ingot", "Steel Ingot");
        NAMES.put("copper_ingot", "Copper Ingot");
        NAMES.put("steel_clump", "Coal Encased Iron");
        NAMES.put("iron_plate", "Iron Plate");
        NAMES.put("steel_plate", "Steel Plate");
        NAMES.put("copper_wire", "Copper Wire");
        NAMES.put("gold_wire", "Gold Wire");
        NAMES.put("basic_circuit", "Basic Circuit");
        NAMES.put("advanced_circuit", "Advanced Circuit");
        NAMES.put("elite_circuit", "Elite Circuit");
        NAMES.put("sulfur_dust", "Organic Sulfur");
        NAMES.put("saltpeter", "Saltpeter");
        NAMES.put("saltpeter_ball", "Saltpeter Starter");
        NAMES.put("poison_powder", "Poison Powder");
        NAMES.put("antidote", "Antidote");
        NAMES.put("empty_bomblet", "Droplet");
        NAMES.put("missile_module", "Missile Module");

        //Entity types. 1.12 registered no entity name in its lang file, so these are port side additions
        // built from the 1.12 registry ids and class purpose (registry-ids.md §4)
        ENTITY_NAMES.put("explosion_holder", "Explosion Holder");
        ENTITY_NAMES.put("fragment", "Fragments");
        ENTITY_NAMES.put("flying_block", "Flying Block");
        ENTITY_NAMES.put("primed_explosive", "Primed Explosive");
        ENTITY_NAMES.put("light_beam", "Light Beam");
        ENTITY_NAMES.put("smoke", "Smoke");
        ENTITY_NAMES.put("redmatter", "Red Matter");
    }

    public ICBMLanguageProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries)
    {
        super(output, registries);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder)
    {
        for (Identifier explosive : ExplosiveForms.ORDER)
        {
            builder.add("icbmclassic.explosive." + explosive.getPath(), explosiveName(explosive.getPath()));
        }
        for (Map.Entry<ExplosiveForm, String> form : FORM_NAMES.entrySet())
        {
            builder.add("icbmclassic.form." + form.getKey().suffix, "%s " + form.getValue());
        }
        builder.add("itemGroup.icbmclassic.main", "ICBM-Classic");

        final Map<String, String> names = flatNames();
        for (Identifier block : keysOf(BuiltInRegistries.BLOCK))
        {
            builder.add("block.icbmclassic." + block.getPath(), nameOf(names, block));
        }
        for (Identifier item : keysOf(BuiltInRegistries.ITEM))
        {
            //Block items use the block description id (RegHelper#blockItem), so they are already covered
            if (!BuiltInRegistries.BLOCK.containsKey(item))
            {
                builder.add("item.icbmclassic." + item.getPath(), nameOf(names, item));
            }
        }
        for (Identifier entity : keysOf(BuiltInRegistries.ENTITY_TYPE))
        {
            final String name = ENTITY_NAMES.get(entity.getPath());
            if (name == null)
            {
                throw new IllegalStateException("no English name for entity " + entity
                        + ", add it to ICBMLanguageProvider");
            }
            builder.add("entity.icbmclassic." + entity.getPath(), name);
        }
    }

    /** Every flat id that has a name: the hand written table plus the composed explosive forms. */
    private static Map<String, String> flatNames()
    {
        final Map<String, String> names = new LinkedHashMap<>(NAMES);
        for (ExplosiveForm form : ExplosiveForm.values())
        {
            for (Identifier explosive : ExplosiveForms.enabled(form))
            {
                names.put(form.idFor(explosive),
                        explosiveName(explosive.getPath()) + " " + FORM_NAMES.get(form));
            }
        }
        return names;
    }

    private static String nameOf(Map<String, String> names, Identifier id)
    {
        final String name = names.get(id.getPath());
        if (name == null)
        {
            throw new IllegalStateException("no English name for " + id + ", add it to ICBMLanguageProvider");
        }
        return name;
    }

    private static String explosiveName(String id)
    {
        final String name = EXPLOSIVE_NAMES.get(id);
        if (name == null)
        {
            throw new IllegalStateException("no English name for explosive icbmclassic:" + id);
        }
        return name;
    }

    private static Set<Identifier> keysOf(net.minecraft.core.Registry<?> registry)
    {
        final Set<Identifier> keys = new LinkedHashSet<>();
        for (Identifier key : registry.keySet())
        {
            if (key.getNamespace().equals(ICBMConstants.DOMAIN))
            {
                keys.add(key);
            }
        }
        return keys;
    }
}
