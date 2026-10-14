package icbm.classic.content.reg;

import icbm.classic.api.refs.ICBMEntities;
import icbm.classic.content.entity.EntityBombCart;
import icbm.classic.content.entity.EntityExplosion;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.entity.EntityFragments;
import icbm.classic.content.entity.EntityGrenade;
import icbm.classic.content.entity.EntityLightBeam;
import icbm.classic.content.entity.EntitySmoke;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import icbm.classic.content.missile.entity.explosive.EntityExplosiveMissile;
import icbm.classic.content.missile.entity.explosive.EntityMissileActionable;
import icbm.classic.content.cargo.balloon.EntityBalloon;
import icbm.classic.content.cargo.parachute.EntityParachute;
import icbm.classic.content.cluster.bomblet.EntityBombDroplet;
import icbm.classic.content.entity.EntityPlayerSeat;
import icbm.classic.content.missile.entity.anti.EntitySurfaceToAirMissile;
import icbm.classic.content.missile.entity.itemstack.EntityHeldItemMissile;
import icbm.classic.lib.lookup.IcbmLookups;
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

    /** Redmatter, 1.12 {@code redmatter} (500 blocks / 1 → 32 chunks / 1) */
    public static EntityType<EntityRedmatter> REDMATTER;

    /** Thrown grenade, 1.12 {@code item.grenade} (50 blocks / 5 → 4 chunks / 5, S4.05) */
    public static EntityType<EntityGrenade> GRENADE;

    /** Explosive minecart, 1.12 {@code cart.bomb} (50 blocks / 2 → 4 chunks / 2, S4.05) */
    public static EntityType<EntityBombCart> BOMB_CART;

    /** Explosive missile, 1.12 {@code explosive_missile} (500 blocks / 1 → 32 chunks / 1, S4.02) */
    public static EntityType<EntityExplosiveMissile> EXPLOSIVE_MISSILE;

    /** Generic actionable missile, 1.12 {@code generic_missile} (500 blocks / 1 → 32 chunks / 1, S4.02) */
    public static EntityType<EntityMissileActionable> GENERIC_MISSILE;

    /** Surface to air missile, 1.12 {@code missile.sam} (500 blocks / 1 → 32 chunks / 1, S4.04) */
    public static EntityType<EntitySurfaceToAirMissile> MISSILE_SAM;

    public static EntityType<EntityHeldItemMissile> HELD_ITEM_MISSILE;
    public static EntityType<EntityBombDroplet> BOMB_DROPLET;
    public static EntityType<EntityPlayerSeat> MISSILE_SEAT;
    public static EntityType<EntityParachute> PARACHUTE;
    public static EntityType<EntityBalloon> BALLOON;
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
        // 1.12: EntityExplosive exposed capabilityEMP through getCapability(CapabilityEMP.EMP, null); the
        // entity implements IEMPReceiver itself, the lookup registration needs the EntityType to exist
        // (IcbmLookups.init runs earlier, see CapabilityEMP#register)
        IcbmLookups.EMP_RECEIVER_ENTITY.registerSelf(PRIMED_EXPLOSIVE);
        // 1.12 EntityReg: buildEntityEntry(EntityLightBeam.class, ICBMEntities.BEAM, 80, 5)
        LIGHT_BEAM = registerEntity(ICBMEntities.BEAM, 1.0F, 1.0F, 5, 5, EntityLightBeam::new);
        // 1.12 EntityReg: buildEntityEntry(EntitySmoke.class, ICBMEntities.SMOKE, 100, 15)
        SMOKE = registerEntity(ICBMEntities.SMOKE, 1.0F, 1.0F, 7, 15, EntitySmoke::new);
        // 1.12 EntityReg: buildEntityEntry(EntityRedmatter.class, ICBMEntities.REDMATTER, 500, 1)
        REDMATTER = registerEntity(ICBMEntities.REDMATTER, 0.98F, 0.98F, 32, 1, EntityRedmatter::new);

        // 1.12 EntityReg: buildEntityEntry(EntityGrenade.class, ICBMEntities.GRENADE, 50, 5)
        // 1.12 size comes from the EntityGrenade constructor (setSize(0.25F, 0.25F))
        GRENADE = registerEntity(ICBMEntities.GRENADE, 0.25F, 0.25F, 4, 5, EntityGrenade::new);
        // 1.12 EntityReg: buildEntityEntry(EntityBombCart.class, ICBMEntities.BOMB_CART, 50, 2)
        // 1.12 size is the vanilla minecart one inherited from EntityMinecartTNT (0.98F x 0.7F)
        BOMB_CART = registerEntity(ICBMEntities.BOMB_CART, 0.98F, 0.7F, 4, 2, EntityBombCart::new);

        // 1.12 EntityReg: buildEntityEntry(EntityExplosiveMissile.class, ICBMEntities.MISSILE_EXPLOSIVE, 500, 1)
        EXPLOSIVE_MISSILE = registerEntity(ICBMEntities.MISSILE_EXPLOSIVE, 0.5F, 0.5F, 32, 1, EntityExplosiveMissile::new);
        // 1.12 EntityReg: buildEntityEntry(EntityMissileActionable.class, ICBMEntities.MISSILE_GENERIC, 500, 1)
        GENERIC_MISSILE = registerEntity(ICBMEntities.MISSILE_GENERIC, 0.5F, 0.5F, 32, 1, EntityMissileActionable::new);

        // 1.12 EntityReg: buildEntityEntry(EntitySurfaceToAirMissile.class, ICBMEntities.MISSILE_SAM, 500, 1)
        MISSILE_SAM = registerEntity(ICBMEntities.MISSILE_SAM, 0.5F, 0.5F, 32, 1, EntitySurfaceToAirMissile::new);

        HELD_ITEM_MISSILE = registerEntity(ICBMEntities.MISSILE_HELD_ITEM, 0.5F, 0.5F, 32, 1, EntityHeldItemMissile::new);
        BOMB_DROPLET = registerEntity(ICBMEntities.BOMB_DROPLET, 0.25F, 0.25F, 32, 1, EntityBombDroplet::new);
        MISSILE_SEAT = registerEntity(ICBMEntities.MISSILE_SEAT, 0.5F, 0.5F, 4, 2, EntityPlayerSeat::new);
        PARACHUTE = registerEntity(ICBMEntities.PARACHUTE, 0.5F, 0.5F, 32, 1, EntityParachute::new);
        BALLOON = registerEntity(ICBMEntities.BALLOON, 0.5F, 0.5F, 32, 1, EntityBalloon::new);

        IcbmLookups.EMP_RECEIVER_ENTITY.registerSelf(EXPLOSIVE_MISSILE, GENERIC_MISSILE, MISSILE_SAM, HELD_ITEM_MISSILE);
        IcbmLookups.MISSILE.registerSelf(EXPLOSIVE_MISSILE, GENERIC_MISSILE, MISSILE_SAM, HELD_ITEM_MISSILE);
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
