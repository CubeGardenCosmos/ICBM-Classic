package icbm.classic.config.util;

import icbm.classic.ICBMClassic;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * List of BlockStates/Blocks to use in config purposes. This is meant for internal use by the mod and should
 * never be touched by other mods. Use events, integrations, or ask for changes before bypassing this system.
 * <p>
 * Life Cycle:
 * - unlock
 * - clear existing
 * - load(external inputs)
 * - batch(block-registry)
 * - lock
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>old block-state type → {@link BlockState}, old property type → {@link Property}</li>
 *     <li>{@code ForgeRegistries.BLOCKS} → {@link RegistryLookup#getBlock(String)}/{@link RegistryLookup#isBlock(String)}</li>
 *     <li>Block metadata ({@code mod:key@number}) no longer exists in 26.3, such entries resolve to the
 *     block's default state and a warning is logged.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class BlockStateConfigListOld {
    // TODO once ContentBuilder Json system is published for MC replace this with JSON version to support programmatic and more complex entries
    //      entries to consider once JSON is allowed: time/date specific, conditional statements such as IF(MOD) IF(WORLD) IF(MATH) IF(GEO_AREA), block sets/lists

    // Constructor
    @Getter
    private final String name;
    private final Consumer<BlockStateConfigListOld> reloadCallback;

    // Temporary storage
    final List<String> mods = new ArrayList<>();
    final Map<String, List<Function<Block, Boolean>>> fuzzyBlockChecks = new HashMap<>();

    // Block lists
    final HashSet<BlockState> blockStates = new HashSet<>();
    final HashSet<Block> blocks = new HashSet<>();
    final Map<Block, List<Function<BlockState, Boolean>>> blockStateMatchers = new HashMap<>();

    // States
    @Getter
    private boolean isLocked = false;

    public void reload() {
        this.unlock();
        this.reset();
        // Let the parent load defaults and pull in configs
        this.reloadCallback.accept(this);
        this.batchBlockRegistry();
        this.lock();
    }

    private void unlock() {
        this.isLocked = false;
    }

    private void reset() {
        blockStates.clear();
        blocks.clear();
        blockStateMatchers.clear();
    }

    private void lock() {
        this.isLocked = true;

        // Clear temp data
        mods.clear();
        fuzzyBlockChecks.clear();
    }

    public void batchBlockRegistry() {
        if (isLocked) {
            ICBMClassic.logger().error(name + ": list is locked but loading was invoked!!", new IllegalArgumentException());
            return;
        }

        // Handle mods by looping entire registry
        for (Identifier blockKey : RegistryLookup.blockKeys()) {

            final Block block = RegistryLookup.getBlock(blockKey.toString());

            // Mods
            final String modId = blockKey.getNamespace();
            if (mods.contains(modId)) {
                blocks.add(block);
            }

            // Blocks generic fuzzy, allows for any logic to be run per block
            if (fuzzyBlockChecks.containsKey(modId)) {
                if (fuzzyBlockChecks.get(modId).stream().anyMatch(func -> func.apply(block))) {
                    blocks.add(block);
                }
            }
        }

        lock();
    }

    public boolean contains(BlockState state) {
        if (state == null) {
            return false;
        }
        if (blocks.contains(state.getBlock()) || blockStates.contains(state)) {
            return true;
        }
        if (Optional.ofNullable(blockStateMatchers.get(state.getBlock()))
            .map(l -> l.stream().anyMatch(f -> f.apply(state))).orElse(false)) {
            blockStates.add(state);
            return true;
        }
        return false;
    }

    /**
     * Loads block states from a collection of strings. Strings can contain nearly anything
     * so long as it results in block(s) or block-state(s).
     *
     * @param entries to process
     */
    public void loadBlockStates(Iterable<String> entries) {
        if (checkLock("blocks", () -> String.join(", ", entries))) {
            return;
        }

        entries.forEach(str -> {
            final String entry = str.trim();
            handleEntry(entry);
        });
    }

    /**
     * Loads block states from a collection of strings. Strings can contain nearly anything
     * so long as it results in block(s) or block-state(s).
     *
     * @param entries to process
     */
    public void loadBlockStates(String... entries) {
        if (checkLock("blocks", () -> String.join(", ", entries))) {
            return;
        }

        for (String str : entries) {
            final String entry = str.trim();
            handleEntry(entry);
        }
    }

    public void addMod(String mod) {
        if (checkLock("mod", () -> mod)) {
            return;
        }
        this.mods.add(mod);
    }

    private boolean checkLock(String type, Supplier<String> entry) {
        if (this.isLocked) {
            ICBMClassic.logger().error(name + ": list is locked. Unable to add '" + type + "' entry '" + entry.get() + "'", new IllegalArgumentException());
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static List<Comparable<?>> rawPossibleValues(Property<?> property) {
        return (List<Comparable<?>>) (List<?>) ((Property) property).getPossibleValues();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static String rawName(Property<?> property, Comparable<?> value) {
        return ((Property) property).getName((Comparable) value);
    }

    boolean handleEntry(String entry) {
        try {
            // TODO replace with regex for better match detection
            // TODO remove spaces from entries so we don't need to do string.trim() everywhere, really don't care if `m i n e c r a f t : s t o    n e' is valid as a result

            // Metadata sugar for 1.12
            if (entry.contains("@")) {
                return handleMetaData(entry);
            }
            // Block states, also supports ~
            else if (entry.contains("[")) {
                return handleBlockState(entry);
            }
            // Blocks with fuzzy checks
            else if (entry.contains("~")) {
                return handleFuzzyBlocks(entry);
            }
            //TODO add data-driven block properties using `@prop:` likely can do keywords using `@word` such as `@contains:` or `@regex:`
            return handleSimpleBlock(entry);
        }
        // Catch all if something fails with block states in other mods
        catch (Exception e) {
            ICBMClassic.logger().error(name + ": Unexpected error parsing `" + entry + "` for banAllow list.", e);
        }
        return false;
    }

    boolean handleSimpleBlock(String entry) {
        final String blockKey = this.getBlockKey(entry);
        if (blockKey == null) {
            return false;
        }

        if (!RegistryLookup.isBlock(blockKey)) {
            ICBMClassic.logger().error(name + ": Failed to find block matching entry `" + entry + "`");
            return false;
        }

        return blocks.add(RegistryLookup.getBlock(blockKey));
    }

    String getBlockKey(String entry) {
        // TODO convert to regex
        final String[] keySplit = entry.split(":", -1);
        final String domain = keySplit.length == 2 ? keySplit[0].trim() : null;
        final String key = keySplit.length == 2 ? keySplit[1].trim() : null;
        if (keySplit.length != 2 || domain.isEmpty() || key.isEmpty()) {
            return null;
        }
        return domain + ":" + key;
    }

    boolean handleMetaData(String entry) {
        // TODO replace validation sections with regex for cleaner extraction and linting

        // Validate general format
        final String[] metaSplit = entry.split("@");
        if (metaSplit.length != 2 || !metaSplit[1].matches("\\d+") || !metaSplit[0].contains(":")) {
            ICBMClassic.logger().error(name + ": Detected invalid metadata format for `" + entry + "`  for banAllow list. Expected `mod:key` example: `minecraft:stone`");
            return false; //TODO maybe throw instead so we can unit test the errors?
        }

        // Validate key format
        final String blockKey = this.getBlockKey(metaSplit[0]);
        if (blockKey == null) {
            ICBMClassic.logger().error(name + ": Detected invalid metadata format for `" + entry + "`  for banAllow list. Expected `mod:key` example: `minecraft:stone`");
            return false;
        }

        // Get block from provided key
        if (!RegistryLookup.isBlock(blockKey)) {
            ICBMClassic.logger().error(name + ": Failed to find block matching entry `" + entry + "` for banAllow list.");
            return false;
        }
        final Block block = RegistryLookup.getBlock(blockKey);

        //26.3 has no block metadata, we use the default state and warn about the change in behavior
        ICBMClassic.logger().warn(name + ": metadata values are not supported in 26.3, `" + entry + "` will use the block's default state");
        return blockStates.add(block.defaultBlockState());
    }

    boolean handleBlockState(String entry) {

        final String[] nameVsPropSplit = entry.split("\\[");

        // Block data
        final String regName = this.getBlockKey(nameVsPropSplit[0]);
        if (regName == null) {
            // TODO log formatting issue
            return false;
        }

        final Block block = RegistryLookup.getBlock(regName);

        if (block == null) {
            ICBMClassic.logger().error("Config Flying Block: Failed to find block '" + regName + "' matching entry `" + entry + "` for banAllow list.");
            return false;
        }

        // Properties
        final String[] properties = nameVsPropSplit[1].replace("]", "").split(",");

        final Map<Property<?>, Function<Comparable, Boolean>> matchers = new HashMap<>();

        for (String propEntry : properties) {
            final String[] stateSplit = propEntry.split(":");
            final String propName = stateSplit[0].trim();
            final String propValue = stateSplit[1].trim();

            final Property<?> property = block.getStateDefinition().getProperty(propName);
            if (property == null) {
                ICBMClassic.logger().error("Config Flying Block: Failed to find property '" + propName + "' for block '" + regName + "' matching entry `" + entry + "` for banAllow list.");
                return false;
            }

            if (propValue.equals("~")) {
                matchers.put(property, (o) -> true);
            } else if (propValue.startsWith("~")) {
                final String stringMatch = propValue.substring(1).trim();
                @SuppressWarnings({"unchecked", "rawtypes"})
                final List<Comparable<?>> valuesToMatch = (List<Comparable<?>>) ((Property) property).getPossibleValues().stream()
                    .filter(o -> ((Property) property).getName((Comparable) o).endsWith(stringMatch)).collect(Collectors.toList());
                if (valuesToMatch.isEmpty()) {
                    ICBMClassic.logger().error("Config Flying Block: Failed to find values matching '" + propValue + "' for property '" + propName + "' and block '" + regName + "' matching entry `" + entry + "`");
                    return false;
                }
                matchers.put(property, valuesToMatch::contains);
            } else if (propValue.endsWith("~")) {
                final String stringMatch = propValue.substring(0, propValue.length() - 1).trim();
                @SuppressWarnings({"unchecked", "rawtypes"})
                final List<Comparable<?>> valuesToMatch = (List<Comparable<?>>) ((Property) property).getPossibleValues().stream()
                    .filter(o -> ((Property) property).getName((Comparable) o).startsWith(stringMatch)).collect(Collectors.toList());
                if (valuesToMatch.isEmpty()) {
                    ICBMClassic.logger().error("Config Flying Block: Failed to find values matching '" + propValue + "' for property '" + propName + "' and block '" + regName + "' matching entry `" + entry + "`");
                    return false;
                }
                matchers.put(property, valuesToMatch::contains);
            } else {
                // Simple value matcher
                final Optional value = rawPossibleValues(property).stream()
                    .filter(o -> rawName(property, o).equalsIgnoreCase(propValue)).findFirst();
                if (!value.isPresent()) {
                    ICBMClassic.logger().error("Config Flying Block: Failed to find value '" + propValue + "' for property '" + propName + "' and block '" + regName + "' matching entry `" + entry + "`");
                    return false;
                }
                matchers.put(property, (o) -> Objects.equals(value.get(), o));
            }
        }

        if (matchers.isEmpty()) {
            return false;
        }

        if (!blockStateMatchers.containsKey(block)) {
            blockStateMatchers.put(block, new ArrayList<>());
        }
        return blockStateMatchers.get(block).add((blockState) -> matchesFuzzyState(blockState, matchers));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    boolean matchesFuzzyState(BlockState state, Map<Property<?>, Function<Comparable, Boolean>> matchers) {
        for (Property propKey : matchers.keySet()) {
            if (!state.getProperties().contains(propKey)) {
                return false;
            }

            final Function<Comparable, Boolean> check = matchers.get(propKey);
            if (check != null && !check.apply(state.getValue(propKey))) {
                return false;
            }
        }
        return true;
    }

    boolean handleFuzzyBlocks(String entry) {
        final String[] split = entry.split(":");

        // TODO replace with regex
        //FORMAT CHECK: Requires single ':' and should contain only a single '~'
        if (split.length != 2 || split[1].lastIndexOf("~") != split[1].indexOf("~") || split[0].contains("~") || split[0].isEmpty()) {
            ICBMClassic.logger().error(name + ": Detected invalid fuzzy format for `" + entry + "`  for banAllow list. Expected `mod:~`, `mod:key~` or `mod:~key`");
            return false;
        }

        // TODO add fuzzy without domain such as `~stone`

        final String domain = split[0].trim();
        final String resource = split[1].trim();

        // case 1: General mod addition, technically a fuzzy check but really is a modID==value for sanity reasons
        if (Objects.equals(resource, "~")) {
            return mods.add(domain);
        }
        // case 2: fuzzy leading word check, Ex: `minecraft:iron~` would match all iron blocks such as `minecraft:iron_bars`
        else if (resource.endsWith("~")) {
            addFuzzyForBlock(domain, (block) -> blockPathStartsWith(block, resource.substring(0, resource.length() - 1)));
            return true;
        }
        // case 3: fuzzy trailing word check, EX: `minecraft:~door` would match all doors such as `minecraft:oak_door`
        else if (resource.startsWith("~")) {
            addFuzzyForBlock(domain, (block) -> blockPathEndsWith(block, resource.substring(1)));
            return true;
        }

        ICBMClassic.logger().error(name + ": Couldn't match fuzzy format for `" + entry + "`  for banAllow list. Expected `mod:~`, `mod:key~` or `mod:~key`");
        return false;
    }

    boolean blockPathStartsWith(Block block, String value) {
        return RegistryLookup.keyPathOf(block).startsWith(value);
    }

    boolean blockPathEndsWith(Block block, String value) {
        return RegistryLookup.keyPathOf(block).endsWith(value);
    }

    void addFuzzyForBlock(String domain, Function<Block, Boolean> check) {
        if (!this.fuzzyBlockChecks.containsKey(domain)) {
            this.fuzzyBlockChecks.put(domain, new ArrayList<>());
        }
        this.fuzzyBlockChecks.get(domain).add(check);
    }

    public List<String> dumpBlocksContained() {
        final List<String> list = new ArrayList<>();
        for (Identifier blockKey : RegistryLookup.blockKeys()) {
            final Block block = RegistryLookup.getBlock(blockKey.toString());
            if (block != null && this.contains(block.defaultBlockState())) {
                list.add(blockKey.toString());
            }
        }
        return list;
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get();
    }
}
