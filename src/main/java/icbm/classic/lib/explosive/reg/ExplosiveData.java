package icbm.classic.lib.explosive.reg;

import icbm.classic.api.EnumTier;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.data.IActionFieldProvider;
import icbm.classic.api.explosion.IBlastFactory;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.lib.actions.ActionDataBase;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Handles storing data about an explosive in the {@link ExplosiveRegistry}.
 * <p>
 * 1.12 also carried the numeric registry id (used for item metadata and for save compatibility).
 * ADR 0002 drops the numeric id layer, so the id field and {@code getRegistryID()} are gone; the
 * {@link Identifier} registry key is the only identity.
 *
 * @deprecated
 */
@Deprecated
public class ExplosiveData extends ActionDataBase implements IExplosiveData
{
    @NonNull
    private final Identifier regName;

    @NonNull
    private final EnumTier tier;

    @NonNull
    private final IBlastFactory blastCreationFactory;

    public ExplosiveData(@NonNull Identifier regName, @NonNull EnumTier tier, @NonNull IBlastFactory blastCreationFactory)
    {
        this.regName = regName;
        this.tier = tier;
        this.blastCreationFactory = blastCreationFactory;
    }

    @NonNull
    public IBlastFactory getBlastCreationFactory()
    {
        return blastCreationFactory;
    }

    @Override
    @NonNull
    public Identifier getRegistryKey()
    {
        return regName;
    }

    @Override
    @NonNull
    public EnumTier getTier()
    {
        return tier;
    }

    @Override
    @NonNull
    public IAction create(Level world, double x, double y, double z, @NonNull IActionSource source, @Nullable IActionFieldProvider fieldAccessors)
    {
        final IAction blast = blastCreationFactory.create(world, x, y, z, source);

        if (blast instanceof IBlastInit init)
        {
            init.setExplosiveData(this);
            init.setActionSource(source);

            if (fieldAccessors != null && fieldAccessors.hasField(ActionFields.AREA_SIZE))
            {
                init.setBlastSize(fieldAccessors.getValue(ActionFields.AREA_SIZE));
            }
        }

        blast.applyFields(fieldAccessors);
        blast.applyFields(this);

        return blast;
    }

    @Override
    public int compareTo(IExplosiveData o)
    {
        return getRegistryKey().toString().compareTo(o.getRegistryKey().toString());
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof IExplosiveData data && regName.equals(data.getRegistryKey());
    }

    @Override
    public int hashCode()
    {
        return regName.hashCode();
    }

    @Override
    public String toString()
    {
        return "ExplosiveData[" + regName + ", tier=" + tier + "]";
    }
}
