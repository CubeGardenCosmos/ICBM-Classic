package icbm.classic.config.util;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * List of BlockStates/Blocks to use in config purposes. This is meant for internal use by the mod and should
 * never be touched by other mods. Use events, integrations, or ask for changes before bypassing this system.
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>old block-state type → {@link BlockState}, old property type → {@link Property}</li>
 *     <li>Registry keys are plain {@code namespace:path} strings (see {@link ResourceConfigList}).</li>
 *     <li>Block meta values ({@code domain:path@meta}) no longer exist in 26.3. Such entries are parsed as
 *     the block's default state and a warning is logged.</li>
 * </ul>
 */
public abstract class BlockStateConfigList<VALUE> extends ResourceConfigList<BlockStateConfigList<VALUE>, BlockState, VALUE> {

    protected static final Pattern BLOCK_PROP_REGEX = Pattern.compile("^(.*):([^=\\s]*)\\[((?:[\\w.]+:[\\w.]+,?)+)\\](?:=(.*))?");
    protected static final Pattern CHANCE_REGEX = Pattern.compile("^@chance\\(([0-9]*[.][0-9]*)\\)$");
    protected static final Pattern NBT_REGEX = Pattern.compile("^@nbt\\((.*)\\)$");

    /** Legacy 1.12 {@code domain:path@meta} entries; meta is ignored in 26.3. */
    protected static final Pattern LEGACY_META_REGEX = Pattern.compile("^([^=\s@]+):([^=\s@]*)@([0-9]+)$");


    public BlockStateConfigList(String name, Consumer<BlockStateConfigList<VALUE>> reloadCallback) {
        super(name, "https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-block-states", reloadCallback);
        addMatcher(BLOCK_PROP_REGEX, this::handleBlockProps);
        addMatcher(LEGACY_META_REGEX, this::handleMeta);
        addMatcher(KEY_VALUE_REGEX, this::handleSimple);
    }

    @Override
    protected String getContentKey(BlockState blockState) {
        return blockState.getBlock().builtInRegistryHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
    }

    protected ResourceConfigEntry<BlockState, VALUE> handleMeta(Matcher regexMatcher, String source, String entry, Integer index) {
        //26.3 has no block metadata, we can only resolve the block itself and use the default state
        final String domain = regexMatcher.group(1);
        final String resource = regexMatcher.group(2);
        final String key = domain + ":" + resource;

        if (!isDomainValid(domain)) {
            error(source, entry, "No matching mod domain found for '" + key + "'");
            return null;
        } else if (!isValidKey(key)) {
            error(source, entry, "No matching content found for '" + key + "'");
            return null;
        }

        warn(source, entry, "Meta values are not supported in 26.3, '" + key + "' will use the default block state");
        final Function<BlockState, VALUE> matcher = getSimpleValue(key, parseValue(source, entry, regexMatcher.group(3)));
        return new ResourceConfigEntry<>("resource_simple", index, matcher).setKey(key);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Function<BlockState, VALUE> getPropValue(String key, Map<Property<?>, Function<Comparable, Boolean>> propMatchers, @Nullable VALUE value) {
        return (blockState) -> {
            if (key.equals(getContentKey(blockState))) {
                for (Property propKey : propMatchers.keySet()) {
                    if (!blockState.getProperties().contains(propKey)) {
                        return null;
                    }

                    final Function<Comparable, Boolean> check = propMatchers.get(propKey);
                    if (check != null && !check.apply(blockState.getValue(propKey))) {
                        return null;
                    }
                }
                return value;
            }
            return null;
        };
    }

    protected ResourceConfigEntry<BlockState, VALUE> handleBlockProps(Matcher domainMatcher, String source, String entry, int index) {
        final String domain = domainMatcher.group(1);
        final String resource = domainMatcher.group(2);
        final String key = domain + ":" + resource;

        if (!isDomainValid(domain)) {
            error(source, entry, "No matching mod domain found for '" + key + "'");
            return null;
        } else if (!isValidKey(key)) {
            error(source, entry, "No matching content found for '" + key + "'");
            return null;
        }

        final net.minecraft.world.level.block.Block block = getBlock(key);
        final String[] props = domainMatcher.group(3).split(",");
        final Map<Property<?>, Function<Comparable, Boolean>> propMatchers = collectBlockProps(source, entry, block, props);
        if (propMatchers == null) {
            return null;
        }

        final String valueStr = domainMatcher.group(4);
        final VALUE value = parseValue(source, entry, valueStr);

        final Function<BlockState, VALUE> matcher = getPropValue(key, propMatchers, value);
        return new ResourceConfigEntry<>("resource_block_state", index, matcher);
    }

    @Override
    protected boolean isValidKey(String targetKey) {
        return RegistryLookup.isBlock(targetKey);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Map<Property<?>, Function<Comparable, Boolean>> collectBlockProps(String source, String entry, net.minecraft.world.level.block.Block block, String[] props) {
        final Map<Property<?>, Function<Comparable, Boolean>> matchers = new HashMap<>();
        for (String propEntry : props) {
            final Matcher propMatcher = KEY_VALUE_REGEX.matcher(propEntry);
            if (!propMatcher.matches()) {
                // TODO error log
                return null;
            }
            final String propKey = propMatcher.group(1);
            final String propValue = propMatcher.group(2);
            //TODO consider using group 3 as a boolean contains check

            final Property<?> property = block.getStateDefinition().getProperty(propKey);
            if (property == null) {
                error(source, entry, String.format("Failed to find property '%s' for block '%s' matching entry `%s`", propKey, getBlockKey(block), propEntry));
                return null;
            }

            if (propValue.equals("~")) {
                matchers.put(property, (o) -> true);
            } else if (propValue.startsWith("~") || propValue.endsWith("~")) {
                final String stringMatch = propValue.substring(1).trim();
                @SuppressWarnings({"unchecked", "rawtypes"})
                final List<Comparable<?>> valuesToMatch = (List<Comparable<?>>) ((Property) property).getPossibleValues().stream()
                    .filter(o -> {
                        final String name = ((Property) property).getName((Comparable) o);
                        if (propValue.endsWith("~")) {
                            return name.endsWith(stringMatch);
                        }
                        return name.startsWith(stringMatch);
                    }).collect(Collectors.toList());
                if (valuesToMatch.isEmpty()) {
                    error(source, entry, String.format("Failed to find values matching '%s' for property '%s' and block '%s' matching entry '%s'", propValue, propKey, getBlockKey(block), propEntry));
                    return null;
                }
                matchers.put(property, valuesToMatch::contains);
            } else {
                // Simple value matcher
                final Optional value = rawPossibleValues(property).stream()
                    .filter(o -> rawName(property, o).equalsIgnoreCase(propValue)).findFirst();
                if (!value.isPresent()) {
                    error(source, entry, String.format("Failed to find values matching '%s' for property '%s' and block '%s' matching entry '%s'", propValue, propKey, getBlockKey(block), propEntry));
                    return null;
                }
                matchers.put(property, (o) -> Objects.equals(value.get(), o));
            }
        }
        return matchers;
    }

    protected BlockState parseBlockState(String source, String entry, String valueToParse) {
        final Matcher protoMatcher = BLOCK_PROP_REGEX.matcher(valueToParse);
        if (protoMatcher.matches()) {
            return parseBlockState(source, entry, protoMatcher.group(1) + ":" + protoMatcher.group(2));
        }

        final Matcher metaMatcher = LEGACY_META_REGEX.matcher(valueToParse);
        if (metaMatcher.matches()) {
            final String key = metaMatcher.group(1) + ":" + metaMatcher.group(2);
            if (!isDomainValid(metaMatcher.group(1))) {
                error(source, entry, "No matching mod domain found for '" + key + "'");
                return null;
            } else if (!isValidKey(key)) {
                error(source, entry, "No matching content found for '" + key + "'");
                return null;
            }
            warn(source, entry, "Meta values are not supported in 26.3, '" + key + "' will use the default block state");
            return getBlock(key).defaultBlockState();
        }

        final Matcher matcher = KEY_VALUE_REGEX.matcher(valueToParse);
        if (matcher.matches()) {
            final String key = matcher.group(1) + ":" + matcher.group(2);
            if (!isDomainValid(matcher.group(1))) {
                error(source, entry, "No matching mod domain found for '" + key + "'");
                return null;
            } else if (!isValidKey(key)) {
                error(source, entry, "No matching content found for '" + key + "'");
                return null;
            }

            return getBlock(key).defaultBlockState();
        }
        return null;
    }

    /** Resolves a block from a {@code namespace:path} key. Registry must be frozen (always true at load time). */
    protected net.minecraft.world.level.block.Block getBlock(String key) {
        return RegistryLookup.getBlock(key);
    }

    protected String getBlockKey(net.minecraft.world.level.block.Block block) {
        return RegistryLookup.keyOf(block);
    }

    public static class BlockOut extends BlockStateConfigList<BlockState> {

        public BlockOut(String name, Consumer<BlockStateConfigList<BlockState>> reloadCallback) {
            super(name, reloadCallback);
        }

        @Override
        protected BlockState parseValue(String source, String entry, @Nullable String value) {
            return super.parseBlockState(source, entry, value);
        }
    }

    public static class BlockChanceOut extends BlockStateConfigList<BlockReplacementData> {

        public BlockChanceOut(String name, Consumer<BlockStateConfigList<BlockReplacementData>> reloadCallback) {
            super(name, reloadCallback);
        }

        @Override
        protected BlockReplacementData parseValue(String source, String key, String value) {

            //minecraft:dirt=minecraft:stone
            //minecraft:dirt=minecraft:stone[variant=stone]
            //minecraft:dirt=minecraft:stone;@chance(0.5);@nbt({"energy":0})

            final BlockReplacementData blockReplacementData = new BlockReplacementData();

            // TODO refactor to be part of normal block out, then have this extend that adding chance check
            if (value.contains(";")) {
                final String[] entries = value.split(";");

                // First entry should always be block, @keep will skip replacement and use the existing block
                if (!entries[0].equals("~")) {
                    blockReplacementData.setBlockState(super.parseBlockState(source, key, entries[0]));
                }

                for (int i = 1; i < entries.length; i++) {
                    final String subEntry = entries[i];

                    //TODO add ability to selectively copy fields from source and target blockEntity
                    //TODO add ability to conditionally set data based on source blockEntity field

                    final Matcher chanceMatcher = CHANCE_REGEX.matcher(subEntry);
                    if (chanceMatcher.matches()) {
                        blockReplacementData.setChance(Math.min(1, Math.max(0, Float.parseFloat(chanceMatcher.group(1)))));
                        continue;
                    }

                    final Matcher nbtMatcher = NBT_REGEX.matcher(subEntry);
                    if (nbtMatcher.matches()) {
                        //TODO regex detect if JSON is valid
                        try {
                            blockReplacementData.setBlockNBT(parseNbt(nbtMatcher.group(1)));
                        } catch (Exception e) {
                            error(source, key, "Failed to read NBT json provided. Cause: " + e.getMessage());
                            return null;
                        }
                        continue;
                    }

                    // Log regex entries that can't be matched
                    error(source, key, "Unknown entry '" + subEntry + "' from value '" + value + "'");
                }

                return blockReplacementData;
            }
            // Legacy block state & chance pair TODO remove after 1.12
            else if (value.contains(",")) {
                final String[] split = value.split(",");
                return blockReplacementData
                    .setChance(Math.min(1, Math.max(0, Float.parseFloat(split[1]))))
                    .setBlockState(super.parseBlockState(source, key, split[0]));
            }

            return blockReplacementData.setBlockState(super.parseBlockState(source, key, value));
        }
    }

    public static class FloatOut extends BlockStateConfigList<Float> {

        public FloatOut(String name, Consumer<BlockStateConfigList<Float>> reloadCallback) {
            super(name, reloadCallback);
        }

        @Override
        protected Float parseValue(String source, String entry, String value) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                error(source, entry, "Value is not a Float");
            }
            return null;
        }
    }

    /** Possible values of a property without generic capture issues. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static List<Comparable<?>> rawPossibleValues(Property<?> property) {
        return (List<Comparable<?>>) (List<?>) ((Property) property).getPossibleValues();
    }

    /** Display name of a property value without generic capture issues. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static String rawName(Property<?> property, Comparable<?> value) {
        return ((Property) property).getName((Comparable) value);
    }

    /** Reads NBT from a JSON string. Replaces the 1.12 {@code JsonToNBT}. */
    protected static net.minecraft.nbt.CompoundTag parseNbt(String json) {
        try {
            return net.minecraft.nbt.TagParser.parseCompoundFully(json);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return null;
        }
    }
}