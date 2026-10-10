package icbm.classic.lib.capability.ex;

import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.reg.IExplosiveCustomization;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.NBTConstants;
import icbm.classic.lib.saving.ISavable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 * <p>
 * 1.12 qualified as a Forge capability provider ({@code ICapabilitySerializable}); 26.3 has no
 * capability system so the class is a plain object and saves through {@link ISavable}
 * ({@code docs/architecture/porting-guide.md} §4).
 */
public class CapabilityExplosive implements IExplosive, ISavable
{
    // 1.12 stored the numeric explosive id (item metadata / registry id); 26.3 keys explosives by Identifier
    public Identifier explosiveId;

    public CapabilityExplosive()
    {
    }

    public CapabilityExplosive(Identifier explosiveId)
    {
        this.explosiveId = explosiveId;
    }

    @Nullable
    @Override
    public IExplosiveData getExplosiveData()
    {
        return explosiveId == null ? null : ICBMClassicHelpers.getExplosive(explosiveId, true);
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
        return null;
    }

    @Override
    public void save(ValueOutput out)
    {
        if (explosiveId != null)
        {
            out.store(NBTConstants.EXPLOSIVE_ID, Identifier.CODEC, explosiveId);
        }
    }

    @Override
    public void load(ValueInput in)
    {
        explosiveId = in.read(NBTConstants.EXPLOSIVE_ID, Identifier.CODEC).orElse(explosiveId);
    }
}
