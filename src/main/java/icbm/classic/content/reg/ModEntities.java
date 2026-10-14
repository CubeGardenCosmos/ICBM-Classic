package icbm.classic.content.reg;

import icbm.classic.api.refs.ICBMEntities;
import icbm.classic.content.entity.EntityExplosion;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity type registration. Renamed from the 1.12 {@code EntityReg} (porting-guide.md §3).
 * <p>
 * 1.12 used {@code EntityEntryBuilder} with a tracking range in blocks; 26.3 uses
 * {@code EntityType.Builder#clientTrackingRange} in <b>chunks</b> — the converted values are listed in
 * registry-ids.md §4. Filled by S3.03 (blast entities) and S4 (missiles, grenade, bomb cart).
 */
public final class ModEntities
{
    /**
     * Blast controller entity, 1.12 {@code holder.explosion} → {@code icbmclassic:explosion_holder}
     * (registry-ids.md §4: 1.12 tracking range 100 blocks / update interval 5 → 7 chunks / 5 ticks).
     */
    public static EntityType<EntityExplosion> EXPLOSION_HOLDER;

    private ModEntities() {}

    /** Registers every entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        final ResourceKey<EntityType<?>> explosionHolderKey = ResourceKey.create(Registries.ENTITY_TYPE, ICBMEntities.EXPLOSION);
        EXPLOSION_HOLDER = Registry.register(BuiltInRegistries.ENTITY_TYPE, explosionHolderKey,
            EntityType.Builder.<EntityExplosion>of(EntityExplosion::new, MobCategory.MISC)
                .sized(0.98F, 0.98F) //1.12 EntityExplosion#EntityExplosion called setSize(0.98F, 0.98F)
                .clientTrackingRange(7)
                .updateInterval(5)
                .build(explosionHolderKey));

        // TODO(S3.03) fragment, flying_block, primed_explosive, light_beam, smoke
        // TODO(S4) missiles, grenade, bomb_cart, bomblet, missile_seat, redmatter, parachute, balloon
    }
}
