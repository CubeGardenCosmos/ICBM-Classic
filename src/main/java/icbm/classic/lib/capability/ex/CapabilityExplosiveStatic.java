package icbm.classic.lib.capability.ex;

import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;

/**
 * Used by any item that has a fixed explosive.
 * <p>
 * 1.12 qualified as a Forge {@code INBTSerializable}; 26.3 implements {@link ISavable} instead
 * (nothing was ever written by the 1.12 implementation either).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class CapabilityExplosiveStatic implements IExplosive, ISavable
{
    private final IExplosiveData data;
    private final Supplier<ItemStack> itemStackSupplier;
    private CompoundTag custom_ex_data;

    public CapabilityExplosiveStatic(IExplosiveData data, Supplier<ItemStack> itemStackSupplier) {
        this.data = data;
        this.itemStackSupplier = itemStackSupplier;
    }

    @Nullable
    @Override
    public IExplosiveData getExplosiveData()
    {
        return data;
    }

    @Override
    public void applyCustomizations(IBlast blast) {

    }

    @Override
    public void addCustomization(IExplosiveCustomization customization) {

    }

    @Nullable
    @Override
    public ItemStack toStack()
    {
        return itemStackSupplier.get();
    }

    @Override
    public void save(ValueOutput out)
    {
        //TODO save customizations
    }

    @Override
    public void load(ValueInput in)
    {

    }
}
