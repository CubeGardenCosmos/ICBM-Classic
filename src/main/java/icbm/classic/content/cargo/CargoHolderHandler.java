package icbm.classic.content.cargo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class CargoHolderHandler
{
    public static boolean isAllowed(ItemStack itemStack)
    {
        return !itemStack.isEmpty();
    }
}
