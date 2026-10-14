package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Damage types added by the mod.
 * <p>
 * 1.12 created these on the fly with {@code new DamageSource("id")}. 26.3 builds every damage source from a
 * data pack entry ({@code data/icbmclassic/damage_type/<id>.json}), so the types are declared here and the
 * json files sit next to this class in the mod resources.
 * <p>
 * The json {@code message_id} values are the 1.12 damage source ids with the namespace normalized to dots
 * (1.12 used {@code icbmclassic:projectile} / {@code icbm.chemical}) because the migrated death messages in
 * {@code en_us.json} use {@code death.attack.icbmclassic.<name>}. The one exception is {@link #MISSILE_IMPACT},
 * which keeps the 1.12 id {@code missile} so death messages behave exactly as in 1.12 (no translation key).
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 10/14/2026.
 */
public final class ModDamageTypes
{
    /** Projectile impact, 1.12 {@code icbmclassic:projectile} ({@code EntityProjectile.DAMAGE_SOURCE}) */
    public static final ResourceKey<DamageType> PROJECTILE = key("projectile");
    /** Spikes block, 1.12 {@code icbmclassic:spikes} ({@code BlockSpikes.DAMAGE_SOURCE}) */
    public static final ResourceKey<DamageType> SPIKES = key("spikes");
    /** Bomblet impact, 1.12 {@code icbmclassic:bomblet} ({@code EntityBombDroplet.DAMAGE_SOURCE}) */
    public static final ResourceKey<DamageType> BOMBLET = key("bomblet");
    /** Radioactive blocks, 1.12 {@code icbmclassic:radioactive_block} ({@code BlockRadioactive.damageSource}) */
    public static final ResourceKey<DamageType> RADIOACTIVE_BLOCK = key("radioactive_block");
    /** Chemical gas, 1.12 {@code icbm.chemical} ({@code BlastChemical.CHEMICAL_DAMAGE}) */
    public static final ResourceKey<DamageType> CHEMICAL = key("chemical");
    /** Contagious gas, 1.12 {@code icbm.contagious} ({@code BlastContagious.CONTAGIOUS_DAMAGE}) */
    public static final ResourceKey<DamageType> CONTAGIOUS = key("contagious");
    /** Redmatter, 1.12 {@code icbm.redmatter} ({@code DamageSourceRedmatter}) */
    public static final ResourceKey<DamageType> REDMATTER = key("redmatter");
    /** Missile impact, 1.12 {@code EntityDamageSource("missile", missile)} */
    public static final ResourceKey<DamageType> MISSILE_IMPACT = key("missile_impact");

    private ModDamageTypes()
    {
        // static only
    }

    /**
     * Creates the registry key for a mod damage type.
     *
     * @param id path of the type inside the mod namespace, must match the json file name
     * @return registry key to look the type up with
     */
    public static ResourceKey<DamageType> key(String id)
    {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ICBMConstants.id(id));
    }

    /**
     * Looks up a mod damage type holder from the level registries.
     *
     * @param level level providing the registry access, client or server
     * @param key   type to look up
     * @return holder of the damage type
     */
    public static Holder<DamageType> holder(Level level, ResourceKey<DamageType> key)
    {
        return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }

    /**
     * Builds a damage source without a source entity, matching 1.12 {@code new DamageSource(id)}.
     *
     * @param level level used to resolve the holder
     * @param key   type of damage
     * @return damage source to apply
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key)
    {
        return new DamageSource(holder(level, key));
    }

    /**
     * Builds a damage source for a direct and causing entity, matching 1.12 {@code EntityDamageSource(id, direct)}.
     *
     * @param level   level used to resolve the holder
     * @param key     type of damage
     * @param direct  entity that caused the damage, can be null
     * @param causing entity behind the direct entity, can be null
     * @return damage source to apply
     */
    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity direct, @Nullable Entity causing)
    {
        return new DamageSource(holder(level, key), direct, causing);
    }
}
