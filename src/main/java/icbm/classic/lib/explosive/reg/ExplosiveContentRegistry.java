package icbm.classic.lib.explosive.reg;

import com.google.common.collect.ImmutableSet;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.reg.content.IExplosiveContentRegistry;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Base for the content types an explosive can be placed as (block, missile, grenade, bomb cart).
 * <p>
 * 1.12 tracked the enabled explosives twice: once by name and once by numeric id (the id set was
 * derived while locking). With numeric ids gone (ADR 0002) only the name set remains.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/4/19.
 */
@Deprecated
public abstract class ExplosiveContentRegistry implements IExplosiveContentRegistry
{
    public final Identifier name;

    private boolean locked = false;

    private Set<Identifier> nameCache = new HashSet<>();
    private Set<IExplosiveData> dataCache = ImmutableSet.of();

    public ExplosiveContentRegistry(Identifier name)
    {
        this.name = name;
    }

    @Override
    public Identifier getRegistryName()
    {
        return name;
    }

    @Override
    public void enableContent(Identifier regName)
    {
        if (locked)
        {
            throw new IllegalStateException(this + ": no content can be registered after the registry phase");
        }
        nameCache.add(regName);
    }

    @Override
    public Set<Identifier> getExplosiveNames()
    {
        return nameCache;
    }

    @Override
    public Set<IExplosiveData> getExplosives()
    {
        return dataCache;
    }

    @Override
    public void lockRegistry()
    {
        if (locked)
        {
            throw new IllegalStateException(this + ": registry was locked twice");
        }
        locked = true;

        nameCache = ImmutableSet.copyOf(nameCache);

        final ImmutableSet.Builder<IExplosiveData> dataBuilder = ImmutableSet.builder();
        for (Identifier explosiveName : nameCache)
        {
            final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(explosiveName, true);
            if (data == null)
            {
                throw new IllegalStateException(this + ": failed to locate explosive by name " + explosiveName);
            }
            dataBuilder.add(data);
        }
        dataCache = dataBuilder.build();
    }

    public boolean isLocked()
    {
        return locked;
    }

    @Override
    public String toString()
    {
        return "ExplosiveContentRegistry[" + name + "]";
    }
}
