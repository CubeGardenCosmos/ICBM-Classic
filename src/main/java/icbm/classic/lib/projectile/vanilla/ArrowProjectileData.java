package icbm.classic.lib.projectile.vanilla;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.lib.buildable.BuildableObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;

public class ArrowProjectileData extends BuildableObject<ArrowProjectileData, IProjectileDataRegistry> implements IProjectileData<AbstractArrow> {

    public static final Identifier NAME = Identifier.fromNamespaceAndPath("minecraft", "arrow.normal");

    public ArrowProjectileData() {
        super(NAME, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, null);
    }

    @Override
    public AbstractArrow newEntity(Level world, boolean allowItemPickup) {
        // 1.12 new EntityTippedArrow(world) → 26.3 用 EntityTypes.ARROW 构造 Arrow（实体类型常量已从 EntityType 移到 EntityTypes）
        final Arrow arrow = new Arrow(EntityTypes.ARROW, world);
        // 1.12 EntityArrow.pickupStatus → 26.3 AbstractArrow.pickup
        arrow.pickup = allowItemPickup ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }
}
