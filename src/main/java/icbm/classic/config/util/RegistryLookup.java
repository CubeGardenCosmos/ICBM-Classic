package icbm.classic.config.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import org.jspecify.annotations.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Lazily resolves {@code namespace:path} keys into blocks and items.
 * <p>
 * Replaces the Forge {@code ForgeRegistries.BLOCKS/ITEMS} lookups and 1.12 style resource keys used by the
 * config infra in 1.12. Keys are plain {@code namespace:path} strings (the ICBM config DSL supports fuzzy
 * markers such as {@code ~} which no longer fit {@link Identifier}), and are only parsed when a lookup is
 * actually needed, e.g. after the registries are frozen. See porting-guide.md §6.
 */
public final class RegistryLookup {

    private RegistryLookup() {
    }

    /** Registry keys of every block; iterate and resolve with {@link #getBlock(String)}. */
    public static Set<Identifier> blockKeys() {
        return BuiltInRegistries.BLOCK.keySet();
    }

    /** Registry keys of every item; iterate and resolve with {@link #getItem(String)}. */
    public static Set<Identifier> itemKeys() {
        return BuiltInRegistries.ITEM.keySet();
    }

    /** Block matching the {@code namespace:path} key, or {@code null} when unknown. Never throws on bad syntax. */
    @Nullable
    public static Block getBlock(String key) {
        return parse(key).map(BuiltInRegistries.BLOCK::getOptional).orElse(Optional.empty()).orElse(null);
    }

    /** True if a block with the given {@code namespace:path} key exists. */
    public static boolean isBlock(String key) {
        return parse(key).map(BuiltInRegistries.BLOCK::containsKey).orElse(false);
    }

    /** Item matching the {@code namespace:path} key, or {@code null} when unknown. Never throws on bad syntax. */
    @Nullable
    public static Item getItem(String key) {
        return parse(key).map(BuiltInRegistries.ITEM::getOptional).orElse(Optional.empty()).orElse(null);
    }

    /** True if an item with the given {@code namespace:path} key exists. */
    public static boolean isItem(String key) {
        return parse(key).map(BuiltInRegistries.ITEM::containsKey).orElse(false);
    }

    /** Registry key of a block as {@code namespace:path}. */
    @Nullable
    public static String keyOf(Block block) {
        return Optional.ofNullable(BuiltInRegistries.BLOCK.getKey(block)).map(Identifier::toString).orElse(null);
    }

    /** Registry key of an item as {@code namespace:path}. */
    @Nullable
    public static String keyOf(Item item) {
        return Optional.ofNullable(BuiltInRegistries.ITEM.getKey(item)).map(Identifier::toString).orElse(null);
    }

    /** Path part of a block's registry key, e.g. {@code iron_block} of {@code minecraft:iron_block}. */
    public static String blockPath(String key) {
        return pathOf(key);
    }

    /** Namespace part of a block's registry key. */
    public static String blockNamespace(String key) {
        return namespaceOf(key);
    }

    /** Path part of a {@code namespace:path} key, or the whole key when there is no namespace. */
    public static String pathOf(String key) {
        if (key == null) {
            return "";
        }
        final int index = key.indexOf(':');
        return index < 0 ? key : key.substring(index + 1);
    }

    /** Namespace part of a {@code namespace:path} key, or {@code ""} when there is none. */
    public static String namespaceOf(String key) {
        if (key == null) {
            return "";
        }
        final int index = key.indexOf(':');
        return index < 0 ? "" : key.substring(0, index);
    }

    /** Parses a key with {@link Identifier#tryParse(String)}; {@link Optional#empty()} for invalid syntax. */
    private static Optional<Identifier> parse(String key) {
        return Optional.ofNullable(key).map(Identifier::tryParse);
    }

    /** Registry key of a block without the namespace. */
    public static String keyPathOf(Block block) {
        return pathOf(keyOf(block));
    }

    /** Registry namespace of a block. */
    public static String keyNamespaceOf(Block block) {
        return namespaceOf(keyOf(block));
    }

    /** Registry key type used for blocks. */
    @Nullable
    public static ResourceKey<Block> blockKeyOf(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElse(null);
    }
}
