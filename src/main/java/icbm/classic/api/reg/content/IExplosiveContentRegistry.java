package icbm.classic.api.reg.content;

import icbm.classic.api.reg.IExplosiveData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * @deprecated being removed in 1.13
 */
@Deprecated
public interface IExplosiveContentRegistry
{
    /**
     * Unique name of the content registry
     *
     * @return
     */
    Identifier getRegistryName();

    /**
     * Set of all ids enabled for the content
     * <p>
     * Set should be immutable after {@link #lockRegistry()} is called
     *
     * @return set of enabled explosives
     */
    Set<Integer> getExplosivesIDs();

    /**
     * Enables content type for the explosion
     *
     * @param explosiveID - registry name of the explosion
     */
    void enableContent(Identifier explosiveID);

    /**
     * Gets a list of all explosives by name.
     * <p>
     * List should be immutable after {@link #lockRegistry()} is called
     *
     * @return
     */
    Set<Identifier> getExplosiveNames();

    /**
     * Gets a list of all explosives
     * <p>
     * List should be immutable after {@link #lockRegistry()} is called
     *
     * @return
     */
    Set<IExplosiveData> getExplosives();

    /**
     * Creates a new explosive device represented by
     * this registry.
     * <p>
     * Ex: Missile stack
     * <p>
     * Note each registry should only have 1 item mapped to
     * it as a time. If another item is desired then a
     * registry should be created to mirror the previous
     * registry.
     *
     * @param data for explosive instance
     * @return new device stack
     */
    default ItemStack getDeviceStack(IExplosiveData data) {
        return getDeviceStack(data.getRegistryKey());
    }

    /**
     * Creates a new explosive device represented by
     * this registry.
     * <p>
     * Ex: Missile stack
     * <p>
     * Note each registry should only have 1 item mapped to
     * it as a time. If another item is desired then a
     * registry should be created to mirror the previous
     * registry.
     *
     * @param regName - registry name of the explosive
     * @return new device stack
     */
    ItemStack getDeviceStack(Identifier regName);

    /**
     * Called at the end of registry phase to
     * lock the registry, convert lists to immutable,
     * and prevent late registration of content.
     */
    void lockRegistry();

    /**
     * Is the content enabled for the explosive
     *
     * @param explosiveData - explosive data
     * @return true if enabled, can also be false if the registry is not setup yet
     */
    default boolean isEnabled(IExplosiveData explosiveData)
    {
        return getExplosivesIDs() != null && getExplosivesIDs().contains(explosiveData.getRegistryID());
    }

    /**
     * Is the content enabled for the explosive
     *
     * @param exName - explosive data name
     * @return true if enabled, can also be false if the registry is not setup yet
     */
    default boolean isEnabled(Identifier exName)
    {
        return getExplosiveNames() != null && getExplosiveNames().contains(exName);
    }

    /**
     * Is the content enabled for the explosive
     *
     * @param exID - explosive data ID
     * @return true if enabled, can also be false if the registry is not setup yet
     */
    default boolean isEnabled(int exID)
    {
        return getExplosivesIDs() != null && getExplosivesIDs().contains(exID);
    }

}
