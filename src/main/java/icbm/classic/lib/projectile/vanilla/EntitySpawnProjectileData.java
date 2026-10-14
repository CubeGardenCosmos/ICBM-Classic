package icbm.classic.lib.projectile.vanilla;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.lib.buildable.BuildableObject;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.UUID;

public class EntitySpawnProjectileData extends BuildableObject<EntitySpawnProjectileData, IProjectileDataRegistry> implements IProjectileData<Entity> {

    public static final Identifier NAME = Identifier.fromNamespaceAndPath("minecraft", "entity");

    private Identifier entityKey;

    @Getter @Setter @Accessors(chain = true)
    private String entityDisplayTag;
    @Getter @Setter @Accessors(chain = true)
    private CompoundTag entityData;

    public EntitySpawnProjectileData() {
        super(NAME, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, SAVE_LOGIC);
    }

    public EntitySpawnProjectileData(Identifier key) {
        this();
        this.entityKey = key;
    }

    @Override
    public Entity newEntity(Level world, boolean allowItemPickup) {
        if(entityKey != null) {
            // 1.12 ForgeRegistries.ENTITIES.getValue(entityKey) → 26.3 BuiltInRegistries.ENTITY_TYPE
            // 必须先 containsKey：ENTITY_TYPE 是 DefaultedRegistry，未知 id 的 getValue 会返回默认实体(猪)而不是 null
            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey) ? BuiltInRegistries.ENTITY_TYPE.getValue(entityKey) : null;
            if(type != null) {
                // 1.12 entry.newInstance(world) → 26.3 EntityType.create(level, reason)
                final Entity entity = type.create(world, EntitySpawnReason.TRIGGERED);
                if(entity != null) {
                    // 1.12 setCustomNameTag(String) → 26.3 setCustomName(Component)
                    if(entityDisplayTag != null) {
                        entity.setCustomName(Component.literal(entityDisplayTag));
                    }
                    if(entityData != null) {
                        // 1.12 把实体自身的 NBT merge 进 entityData 再 readFromNBT 读回；26.3 的等价做法是
                        // 先记住 UUID，直接 load 存储的 tag，然后还原 UUID（1.12 的步骤保证 UUID 不被覆盖）
                        final UUID uuid = entity.getUUID();
                        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), entityData));
                        entity.setUUID(uuid);
                    }
                }
                return entity;
            }
        }
        return null;
    }

    private static final SaveHandler<EntitySpawnProjectileData> SAVE_LOGIC = new SaveHandler<EntitySpawnProjectileData>()
        .mainRoot()
        /* */.nodeIdentifier("entity_id", (e) -> e.entityKey, (e, r) -> e.entityKey = r)
        /* */.nodeString("custom_name", EntitySpawnProjectileData::getEntityDisplayTag, EntitySpawnProjectileData::setEntityDisplayTag)
        /* */.nodeCompoundTag("entity_data", EntitySpawnProjectileData::getEntityData, EntitySpawnProjectileData::setEntityData)
        .base();
}
