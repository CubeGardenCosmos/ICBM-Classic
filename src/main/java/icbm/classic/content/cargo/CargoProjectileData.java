package icbm.classic.content.cargo;

import com.google.common.collect.ImmutableList;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.data.EntityActionTypes;
import icbm.classic.api.data.meta.MetaTag;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.api.missiles.projectile.ProjectileTypes;
import icbm.classic.content.entity.flyingblock.BlockCaptureData;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import icbm.classic.lib.buildable.BuildableObject;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public abstract class CargoProjectileData<T extends CargoProjectileData<T, ENTITY>, ENTITY extends Entity>
        extends BuildableObject<T, IProjectileDataRegistry>
        implements IProjectileData<ENTITY>
{
    private static final ImmutableList<MetaTag> TYPE =
            ImmutableList.of(EntityActionTypes.ENTITY_CREATION, ProjectileTypes.TYPE_HOLDER, ProjectileTypes.TYPE_THROWABLE);

    @Getter
    @Setter
    @Accessors(chain = true)
    private ItemStack heldItem = ItemStack.EMPTY;

    @Getter
    @Setter
    @Accessors(chain = true)
    private ProjectileCargoMode parachuteMode = ProjectileCargoMode.ITEM;

    public CargoProjectileData(Identifier registryKey, SaveHandler<T> saveHandler)
    {
        super(registryKey, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, saveHandler);
    }

    public static <X extends CargoProjectileData<X, ?>> SaveHandler<X> createSaveHandler()
    {
        return new SaveHandler<X>()
                .mainRoot()
                .nodeItemStack("stack", CargoProjectileData::getHeldItem, CargoProjectileData::setHeldItem)
                .nodeEnumString("mode", CargoProjectileData::getParachuteMode, CargoProjectileData::setParachuteMode, ProjectileCargoMode::valueOf)
                .base();
    }

    @NonNull
    @Override
    public Collection<MetaTag> getTypeTags()
    {
        return TYPE;
    }

    @Override
    public Component getTooltip()
    {
        return Component.translatable(getTranslationKey() + ".info." + parachuteMode.name().toLowerCase());
    }

    @Override
    public void onEntitySpawned(@NonNull ENTITY entity, @Nullable Entity source, @Nullable InteractionHand hand)
    {
        if (!heldItem.isEmpty())
        {
            switch (parachuteMode)
            {
                case PROJECTILE -> spawnProjectile(entity);
                case ENTITY -> spawnEntity(entity);
                case BLOCK -> spawnBlockEntity(entity, source, hand);
                default -> spawnItemEntity(entity);
            }
        }
    }

    private void spawnProjectile(@NonNull ENTITY entity)
    {
        final Entity projectile = ICBMClassicAPI.PROJECTILE_DATA_REGISTRY.spawnProjectile(heldItem,
                entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                entity, true, proj -> {
                    proj.setXRot(entity.getXRot());
                    proj.setYRot(entity.getYRot());
                    proj.xRotO = entity.getXRot();
                    proj.yRotO = entity.getYRot();
                });

        if (projectile != null)
        {
            projectile.startRiding(entity);
        }
        else
        {
            spawnItemEntity(entity);
        }
    }

    private void spawnEntity(@NonNull ENTITY entity)
    {
        if (entity.level().isClientSide())
        {
            return;
        }

        if (heldItem.getItem() instanceof SpawnEggItem)
        {
            final EntityType<?> entityType = SpawnEggItem.getType(heldItem);
            if (entityType != null && entity.level() instanceof ServerLevel serverLevel)
            {
                final Entity mob = entityType.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
                if (mob != null)
                {
                    mob.setPos(entity.getX(), entity.getY(), entity.getZ());
                    if (heldItem.has(DataComponents.CUSTOM_NAME) && mob instanceof LivingEntity)
                    {
                        mob.setCustomName(heldItem.getHoverName());
                    }
                    final TypedEntityData<EntityType<?>> entityData = heldItem.get(DataComponents.ENTITY_DATA);
                    if (entityData != null)
                    {
                        entityData.loadInto(mob);
                    }
                    if (serverLevel.addFreshEntity(mob))
                    {
                        mob.startRiding(entity);
                        return;
                    }
                }
            }
        }
        spawnItemEntity(entity);
    }

    private void spawnItemEntity(@NonNull ENTITY entity)
    {
        final ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), heldItem.copy());
        itemEntity.setDefaultPickUpDelay();
        if (entity.level().addFreshEntity(itemEntity))
        {
            itemEntity.startRiding(entity);
        }
    }

    private void spawnBlockEntity(@NonNull ENTITY entity, @Nullable Entity source, @Nullable InteractionHand hand)
    {
        if (!(heldItem.getItem() instanceof BlockItem blockItem))
        {
            spawnItemEntity(entity);
            return;
        }

        final BlockState state = blockItem.getBlock().defaultBlockState();
        final BlockCaptureData blockCaptureData = new BlockCaptureData(state, heldItem.copy());
        if (!FlyingBlock.spawnFlyingBlock(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                blockCaptureData, flyingBlock -> flyingBlock.startRiding(entity), null, null))
        {
            spawnItemEntity(entity);
        }
    }
}
