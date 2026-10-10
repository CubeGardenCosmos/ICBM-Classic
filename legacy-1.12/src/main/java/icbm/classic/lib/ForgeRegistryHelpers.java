package icbm.classic.lib;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class ForgeRegistryHelpers {

    /**
     * Checks if a forge registry contains the value.
     *
     * @param registry to check
     * @param targetKey to look for, supports resource path fuzzy logic (contains, start, end)
     * @return true if at least one entry matches
     */
    public static <V extends IForgeRegistryEntry<V>> boolean contains(IForgeRegistry<V> registry, ResourceLocation targetKey) {
        // Contains
        if(targetKey.getResourcePath().startsWith("~") && targetKey.getResourcePath().endsWith("~")) {
            final String checkStr = targetKey.getResourcePath().substring(1, targetKey.getResourcePath().length() - 1);
            return pathContains(registry, targetKey.getResourceDomain(), checkStr);
        }
        // Ends
        else if(targetKey.getResourcePath().startsWith("~")) {
            final String checkStr = targetKey.getResourcePath().substring(1);
            return pathEndsWith(registry, targetKey.getResourceDomain(), checkStr);
        }
        // Starts
        else if(targetKey.getResourcePath().endsWith("~")) {
            final String checkStr = targetKey.getResourcePath().substring(0, targetKey.getResourcePath().length() - 1);
            return pathStartsWith(registry, targetKey.getResourceDomain(), checkStr);
        }
        // exact match
        return registry.containsKey(targetKey) && registry.getValue(targetKey) != null;
    }

    private static <V extends IForgeRegistryEntry<V>> boolean pathContains(IForgeRegistry<V> registry, String domain, String checkStr) {
        return registry.getKeys()
            .stream()
            .anyMatch(contentKey -> contentKey.getResourceDomain().equalsIgnoreCase(domain)
                && contentKey.getResourcePath().contains(checkStr)
                && registry.getValue(contentKey) != null);
    }

    private static <V extends IForgeRegistryEntry<V>> boolean pathEndsWith(IForgeRegistry<V> registry, String domain, String checkStr) {
        return registry.getKeys()
            .stream()
            .anyMatch(contentKey -> contentKey.getResourceDomain().equalsIgnoreCase(domain)
                && contentKey.getResourcePath().endsWith(checkStr)
                && registry.getValue(contentKey) != null);
    }

    private static <V extends IForgeRegistryEntry<V>> boolean pathStartsWith(IForgeRegistry<V> registry, String domain, String checkStr) {
        return registry.getKeys()
            .stream()
            .anyMatch(contentKey -> contentKey.getResourceDomain().equalsIgnoreCase(domain)
                && contentKey.getResourcePath().startsWith(checkStr)
                && registry.getValue(contentKey) != null);
    }
}
