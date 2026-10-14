package icbm.classic.mixin;

import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private fuse counter of the vanilla TNT minecart.
 * <p>
 * 1.12 {@code EntityBombCart} reached {@code EntityMinecartTNT.minecartTNTFuse} through an access
 * transformer to override the fuse with the ICBM fuse time ({@code ConfigBlast.FUSE_TIMES.BOMB_CARTS.*}).
 * 26.3 has no setter for the counter, so this accessor is the counterpart (S4.05).
 */
@Mixin(MinecartTNT.class)
public interface MinecartTNTAccessor
{
    /** Sets the private {@code fuse} field of {@link MinecartTNT}. */
    @Accessor("fuse")
    void icbmclassic$setFuse(int fuse);
}
