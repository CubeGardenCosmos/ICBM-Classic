package icbm.classic.content.missile.entity.itemstack.item;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class HeldItemMissileHandler
{
    public static boolean isAllowed(ItemStack itemStack)
    {
        return !itemStack.isEmpty();
    }
}
