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
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.NonNull;

public class TippedArrowProjectileData extends BuildableObject<TippedArrowProjectileData, IProjectileDataRegistry> implements IProjectileData<AbstractArrow> {

    public static final Identifier NAME = Identifier.fromNamespaceAndPath("minecraft", "arrow.tipped");

    @Getter @Setter @Accessors(chain = true)
    private ItemStack arrowItem = ItemStack.EMPTY;

    public TippedArrowProjectileData() {
        super(NAME, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, SAVE_LOGIC);
    }
    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return NAME;
    }

    @Override
    public AbstractArrow newEntity(Level world, boolean allowItemPickup) {
        // 1.12 new EntityTippedArrow(world) + setPotionEffect(arrowItem) → 26.3 箭的药水效果来自拾取物品栈
        final Arrow arrow = new Arrow(world, 0, 0, 0, arrowItem.copy(), ItemStack.EMPTY);
        // 1.12 EntityArrow.pickupStatus → 26.3 AbstractArrow.pickup
        arrow.pickup = allowItemPickup ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }

    private static final SaveHandler<TippedArrowProjectileData> SAVE_LOGIC = new SaveHandler<TippedArrowProjectileData>()
        .mainRoot()
        /* */.nodeItemStack("item", TippedArrowProjectileData::getArrowItem, TippedArrowProjectileData::setArrowItem)
        .base();
}
