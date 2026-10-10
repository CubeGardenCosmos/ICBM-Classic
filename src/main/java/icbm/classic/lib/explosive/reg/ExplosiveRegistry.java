package icbm.classic.lib.explosive.reg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import icbm.classic.api.EnumTier;
import icbm.classic.api.explosion.IBlastFactory;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.api.reg.IExplosiveRegistry;
import icbm.classic.api.reg.content.IExplosiveContentRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores every explosive of the mod plus the content types (block, missile, grenade, bomb cart) that
 * can represent them.
 * <p>
 * 1.12 also assigned numeric ids, persisted them to {@code config/icbmclassic/explosive_reg.json} and
 * remapped old ids on load. ADR 0002 drops legacy save migration, so the numeric id layer
 * ({@code forceID}, {@code id_to_name}, {@code loadReg}/{@code saveReg}, {@code nextID}) is gone and
 * everything is keyed by {@link Identifier} (registry-ids.md §1, S2.02).
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/4/19.
 */
@Deprecated
public class ExplosiveRegistry implements IExplosiveRegistry
{
    /** Raised when {@link #register} or {@link #lock()} runs after {@link #lock()}. */
    public static final String LOCKED_MESSAGE = "ICBM explosive registry is locked";

    /** Explosives by id, insertion ordered so iteration matches the registration order. */
    public final Map<Identifier, IExplosiveData> explosiveData = new LinkedHashMap<>();

    /** Content types that can represent an explosive (block, missile, grenade, bomb cart). */
    public final Map<Identifier, IExplosiveContentRegistry> contentRegistry = new LinkedHashMap<>();

    private boolean locked = false;
    private boolean lockNewContentTypes = false;

    private ImmutableSet<IExplosiveData> allExplosives = ImmutableSet.of();

    @Override
    public IExplosiveData register(Identifier name, EnumTier tier, IBlastFactory blastFactory)
    {
        if (locked)
        {
            throw new IllegalStateException(LOCKED_MESSAGE);
        }
        if (explosiveData.containsKey(name))
        {
            throw new IllegalArgumentException("ExplosiveRegistry: duplicate explosive '" + name + "'");
        }

        final IExplosiveData data = new ExplosiveData(name, tier, blastFactory);
        explosiveData.put(name, data);
        return data;
    }

    /**
     * Locks the registry: no new explosives and no new content types can be registered afterwards.
     * Also locks every content registry, which freezes the per explosive content sets and turns them
     * into immutable caches. Called once at the end of {@code ExplosiveInit#init()}.
     */
    public void lock()
    {
        if (locked)
        {
            throw new IllegalStateException(LOCKED_MESSAGE);
        }
        locked = true;
        allExplosives = ImmutableSet.copyOf(explosiveData.values());
        getContentRegistries().forEach(IExplosiveContentRegistry::lockRegistry);
    }

    /** Prevents new content types (but not new explosives). Kept because third parties register content types first. */
    public void lockNewContentTypes()
    {
        if (lockNewContentTypes)
        {
            throw new IllegalStateException("ExplosiveRegistry: content types were locked twice");
        }
        lockNewContentTypes = true;
    }

    @Override
    public IExplosiveData getExplosiveData(Identifier name, boolean allowNull)
    {
        final IExplosiveData data = explosiveData.get(name);
        if (data == null && !allowNull)
        {
            return ICBMExplosives.CONDENSED;
        }
        return data;
    }

    @Override
    public Collection<IExplosiveContentRegistry> getContentRegistries()
    {
        return ImmutableList.copyOf(contentRegistry.values());
    }

    @Override
    public IExplosiveContentRegistry getContentRegistry(Identifier contentID)
    {
        return contentRegistry.get(contentID);
    }

    @Override
    public void registerContentRegistry(Identifier name, IExplosiveContentRegistry registry)
    {
        if (locked || lockNewContentTypes)
        {
            throw new IllegalStateException("ExplosiveRegistry: new explosive content types can not be registered after init phase");
        }
        if (registry == null)
        {
            throw new IllegalArgumentException("ExplosiveRegistry: content type should not be null, Name: " + name);
        }
        if (name == null)
        {
            throw new IllegalArgumentException("ExplosiveRegistry: name is required for content registry type, " + registry);
        }
        if (contentRegistry.containsKey(name) && contentRegistry.get(name) != null)
        {
            throw new IllegalStateException("ExplosiveRegistry: duplicate content type detected for '" + name
                    + "' Prev: " + contentRegistry.get(name) + "  New: " + registry);
        }

        contentRegistry.put(name, registry);
    }

    @Override
    public Set<IExplosiveData> getExplosives()
    {
        return allExplosives;
    }

    @Override
    public String toString()
    {
        return "ExplosiveRegistry[" + explosiveData.size() + " explosives]";
    }
}
