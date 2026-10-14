package icbm.classic.content.reg;

import icbm.classic.api.refs.ICBMEntities;
import icbm.classic.content.entity.EntityExplosion;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.entity.EntityFragments;
import icbm.classic.content.entity.EntityLightBeam;
import icbm.classic.content.entity.EntitySmoke;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
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

    /** Fragment thrown by shrapnel style blasts, 1.12 {@code block.fragment} (40 blocks / 1 → 3 chunks / 1) */
    public static EntityType<EntityFragments> FRAGMENT;

    /** Block lifted by anti-gravity blasts, 1.12 {@code block.gravity} (128 blocks / 1 → 8 chunks / 1) */
    public static EntityType<EntityFlyingBlock> FLYING_BLOCK;

    /** Primed explosive block entity, 1.12 {@code block.explosive} (50 blocks / 5 → 4 chunks / 5) */
    public static EntityType<EntityExplosive> PRIMED_EXPLOSIVE;

    /** Light beam of beam style blasts, 1.12 {@code beam.light} (80 blocks / 5 → 5 chunks / 5) */
    public static EntityType<EntityLightBeam> LIGHT_BEAM;

    /** Gas cloud of smoke style blasts, 1.12 {@code smoke} (100 blocks / 15 → 7 chunks / 15) */
    public static EntityType<EntitySmoke> SMOKE;

    private ModEntities() {}

    /** Registers every entity type. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        // 1.12 EntityReg: buildEntityEntry(EntityExplosion.class, ICBMEntities.EXPLOSION, 100, 5)
        EXPLOSION_HOLDER = registerEntity(ICBMEntities.EXPLOSION, 0.98F, 0.98F, 7, 5, EntityExplosion::new);
        // 1.12 EntityReg: buildEntityEntry(EntityFragments.class, ICBMEntities.BLOCK_FRAGMENT, 40, 1)
        FRAGMENT = registerEntity(ICBMEntities.BLOCK_FRAGMENT, 0.5F, 0.5F, 3, 1, EntityFragments::new);
        // 1.12 EntityReg: buildEntityEntry(EntityFlyingBlock.class, ICBMEntities.BLOCK_GRAVITY, 128, 1)
        FLYING_BLOCK = registerEntity(ICBMEntities.BLOCK_GRAVITY, 0.98F, 0.98F, 8, 1, EntityFlyingBlock::new);
        // 1.12 EntityReg: buildEntityEntry(EntityExplosive.class, ICBMEntities.BLOCK_EXPLOSIVE, 50, 5)
        PRIMED_EXPLOSIVE = registerEntity(ICBMEntities.BLOCK_EXPLOSIVE, 0.98F, 0.98F, 4, 5, EntityExplosive::new);
        // 1.12 EntityReg: buildEntityEntry(EntityLightBeam.class, ICBMEntities.BEAM, 80, 5)
        LIGHT_BEAM = registerEntity(ICBMEntities.BEAM, 1.0F, 1.0F, 5, 5, EntityLightBeam::new);
        // 1.12 EntityReg: buildEntityEntry(EntitySmoke.class, ICBMEntities.SMOKE, 100, 15)
        SMOKE = registerEntity(ICBMEntities.SMOKE, 1.0F, 1.0F, 7, 15, EntitySmoke::new);

        // TODO(S4) missiles, grenade, bomb_cart, bomblet, missile_seat, redmatter, parachute, balloon
    }

    /**
     * Registers an entity type using the {@link BuiltInRegistries#ENTITY_TYPE} registry, matching the 1.12
     * {@code EntityEntryBuilder} call it replaces. 1.12 tracking ranges are in blocks, 26.3 uses chunks
     * (porting-guide.md §2.2), the converted values come from registry-ids.md §4.
     *
     * @param id                entity id, from {@link ICBMEntities}
     * @param width             entity width, 1.12 {@code Entity#setSize}
     * @param height            entity height, 1.12 {@code Entity#setSize}
     * @param trackingRange     client tracking range in chunks
     * @param updateInterval    update interval in ticks
     * @param factory           entity constructor, must take {@code (EntityType, Level)}
     * @return the registered type
     */
    private static <T extends Entity> EntityType<T> registerEntity(Identifier id, float width, float height,
                                                                  int trackingRange, int updateInterval,
                                                                  EntityType.EntityFactory<T> factory)
    {
        final ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
            EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .updateInterval(updateInterval)
                .build(key));
    }
}
