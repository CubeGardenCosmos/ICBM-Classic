package icbm.classic.lib.projectile.vanilla;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.lib.buildable.BuildableObject;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemProjectileData extends BuildableObject<ItemProjectileData, IProjectileDataRegistry> implements IProjectileData<ItemEntity> {

    public static final Identifier NAME = Identifier.fromNamespaceAndPath("minecraft", "item");

    @Getter @Setter @Accessors(chain = true)
    private ItemStack itemStack = ItemStack.EMPTY;

    public ItemProjectileData() {
        super(NAME, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, SAVE_LOGIC);
    }

    @Override
    public ItemEntity newEntity(Level world, boolean allowItemPickup) {
        // 1.12 new EntityItem(world) + setItem(itemStack) → 26.3 位置构造器直接接收物品栈
        final ItemEntity entityItem = new ItemEntity(world, 0, 0, 0, itemStack);
        if(!allowItemPickup) {
            // 1.12 setInfinitePickupDelay() → 26.3 setNeverPickUp()
            entityItem.setNeverPickUp();
        }
        return entityItem;
    }

    private static final SaveHandler<ItemProjectileData> SAVE_LOGIC = new SaveHandler<ItemProjectileData>()
        .mainRoot()
        /* */.nodeItemStack("item", ItemProjectileData::getItemStack, ItemProjectileData::setItemStack)
        .base();
}
