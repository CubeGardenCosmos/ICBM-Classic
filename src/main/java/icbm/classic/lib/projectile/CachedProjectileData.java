package icbm.classic.lib.projectile;

import icbm.classic.api.missiles.projectile.IProjectileData;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class CachedProjectileData implements Function<ItemStack, IProjectileData> {

    private final Supplier<IProjectileData> builder;

    private IProjectileData instance;
    @Override
    public IProjectileData apply(ItemStack itemStack) {
        // 1.12 的原始写法照抄：条件写成 != null（缓存永远不生效）属于 1.12 既有行为，本次移植保持等价，不修 bug
        if(instance != null) {
            instance = builder.get();
        }
        return instance;
    }
}
