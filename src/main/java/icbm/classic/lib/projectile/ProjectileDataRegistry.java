package icbm.classic.lib.projectile;

import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.lib.buildable.BuildableObjectRegistry;
import icbm.classic.lib.projectile.vanilla.ArrowProjectileData;
import icbm.classic.lib.projectile.vanilla.EntitySpawnProjectileData;
import icbm.classic.lib.projectile.vanilla.ItemProjectileData;
import icbm.classic.lib.projectile.vanilla.SpectralArrowProjectileData;
import icbm.classic.lib.projectile.vanilla.TippedArrowProjectileData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProjectileDataRegistry extends BuildableObjectRegistry<IProjectileData> implements IProjectileDataRegistry {

    private final Map<Item, List<ProjectileItemConversion>> itemConversions = new HashMap();
    public ProjectileDataRegistry() {
        super("PROJECTILE", "projectile");
    }

    @Override
    public void lock() {
        super.lock();

        // TODO sort conversions from most complex to least complex
    }

    public void registerVanillaDefaults() {
        if(!this.isLocked()) {

            //General Purpose
            this.register(ItemProjectileData.NAME, ItemProjectileData::new);
            this.register(EntitySpawnProjectileData.NAME, EntitySpawnProjectileData::new);

            // Basic arrow
            final IProjectileData<?> arrowData = new ArrowProjectileData();
            this.register(ArrowProjectileData.NAME, () -> arrowData);
            registerItemStackConversation(new ItemStack(Items.ARROW), (itemStack) -> arrowData);

            // Spectral arrow
            final IProjectileData<?> spectralArrowData = new SpectralArrowProjectileData();
            this.register(SpectralArrowProjectileData.NAME, () -> spectralArrowData);
            registerItemStackConversation(new ItemStack(Items.SPECTRAL_ARROW), (itemStack) -> spectralArrowData);

            // Tipped arrow
            this.register(TippedArrowProjectileData.NAME, TippedArrowProjectileData::new);
            registerItemStackConversation(new ItemStack(Items.TIPPED_ARROW), (itemStack) -> new TippedArrowProjectileData().setArrowItem(itemStack)); //TODO cache ?

            //Items
            registerItemStackConversation(new ItemStack(Items.SNOWBALL), new CachedProjectileData(() -> new EntitySpawnProjectileData(Identifier.withDefaultNamespace("snowball"))));
            registerItemStackConversation(new ItemStack(Items.EGG), new CachedProjectileData(() -> new EntitySpawnProjectileData(Identifier.withDefaultNamespace("egg"))));

            // Spawn eggs
            // 1.12 had one item (Items.SPAWN_EGG) carrying the entity in metadata. 26.3 registers one spawn
            // egg item per entity, so every spawn egg item gets the same conversion.
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof SpawnEggItem) {
                    registerItemStackConversation(new ItemStack(item), ProjectileDataRegistry::buildSpawnEggData);
                }
            }
            //TODO tools as projectiles... because diggy diggy dwarf

            // TODO implement simple block renders as EntityFallingBlock
        }
    }

    /**
     * Builds the projectile data for a spawn egg stack, the 26.3 version of
     * {@code ItemMonsterPlacer.getNamedIdFrom(stack)} plus the EntityTag handling.
     */
    private static @Nullable IProjectileData buildSpawnEggData(ItemStack itemStack) {
        final EntityType<?> entityType = SpawnEggItem.getType(itemStack);
        if (entityType == null) {
            return null;
        }
        final EntitySpawnProjectileData projectileData = new EntitySpawnProjectileData(EntityType.getKey(entityType));
        if (itemStack.has(DataComponents.CUSTOM_NAME)) {
            projectileData.setEntityDisplayTag(itemStack.getHoverName().getString());
        }
        final TypedEntityData<EntityType<?>> entityData = itemStack.get(DataComponents.ENTITY_DATA);
        if (entityData != null) {
            projectileData.setEntityData(entityData.copyTagWithoutId());
        }
        return projectileData;
    }

    public IProjectileData find(ItemStack itemStack) {
        if(itemConversions.containsKey(itemStack.getItem())) {
            final List<ProjectileItemConversion> possibleConversions = itemConversions.get(itemStack.getItem());
            for(ProjectileItemConversion conversion : possibleConversions) {
                final IProjectileData data = conversion.getBuilder().apply(itemStack);
                if(data != null) {
                    return data;
                }
            }
        }
        // TODO(S4.06) 1.12 queried the IProjectileStack Forge capability of the stack here. 26.3 drops the
        //  capability (porting-guide.md §2.6) in favour of the item exposing its projectile data directly,
        //  which is wired with the cargo/cluster content.
        return new ItemProjectileData().setItemStack(itemStack.copy());
    }

    @Override
    public void registerItemStackConversation(ItemStack itemStack, Function<ItemStack, IProjectileData> builder) {
        itemConversions.computeIfAbsent(itemStack.getItem(), k -> new ArrayList<>());
        itemConversions.get(itemStack.getItem()).add(new ProjectileItemConversion(itemStack, builder));
    }

    @Override
    public Entity spawnProjectile(Identifier key, Level world, double x, double y, double z, @Nullable Entity source,
                                  boolean allowItemPickup, @Nullable Consumer<Entity> preSpawnCallback) {
        return spawnProjectile(this.getOrBuild(key), world, x, y, z, source, allowItemPickup, preSpawnCallback);
    }

    @Override
    public Entity spawnProjectile(ItemStack item, Level world, double x, double y, double z, @Nullable Entity source,
                                  boolean allowItemPickup, @Nullable Consumer<Entity> preSpawnCallback) {
        return spawnProjectile(find(item), world, x, y, z, source, allowItemPickup, preSpawnCallback);
    }

    @Override
    public <E extends Entity> E  spawnProjectile(IProjectileData<E> data, Level world, double x, double y, double z, Entity source,
                                  boolean allowItemPickup, Consumer<E> preSpawnCallback) {
        if (data == null) {
            return null;
        }
        final E entity = data.newEntity(world, allowItemPickup);
        if (entity == null) {
            return null;
        }

        entity.setPos(x, y, z);
        if (preSpawnCallback != null) {
            preSpawnCallback.accept(entity);
        }
        if (world.addFreshEntity(entity)) {
            data.onEntitySpawned(entity, source, InteractionHand.MAIN_HAND);
            return entity;
        }
        return null;
    }
}
