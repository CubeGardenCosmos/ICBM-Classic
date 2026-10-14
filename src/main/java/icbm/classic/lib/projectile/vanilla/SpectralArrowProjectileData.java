package icbm.classic.lib.projectile.vanilla;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.missiles.projectile.IProjectileData;
import icbm.classic.api.missiles.projectile.IProjectileDataRegistry;
import icbm.classic.lib.buildable.BuildableObject;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.level.Level;

public class SpectralArrowProjectileData extends BuildableObject<SpectralArrowProjectileData, IProjectileDataRegistry> implements IProjectileData<AbstractArrow> {

    public static final Identifier NAME = Identifier.fromNamespaceAndPath("minecraft", "arrow.spectral");

    public SpectralArrowProjectileData() {
        super(NAME, ICBMClassicAPI.PROJECTILE_DATA_REGISTRY, null);
    }

    @Override
    public AbstractArrow newEntity(Level world, boolean allowItemPickup) {
        // 1.12 这里复制粘贴成了 EntityTippedArrow；26.3 有独立的 SpectralArrow 类型，按 NAME 语义使用它
        final SpectralArrow arrow = new SpectralArrow(EntityTypes.SPECTRAL_ARROW, world);
        // 1.12 EntityArrow.pickupStatus → 26.3 AbstractArrow.pickup
        arrow.pickup = allowItemPickup ? AbstractArrow.Pickup.ALLOWED : AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }
}
