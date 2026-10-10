package icbm.classic.config.util;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * Documentation <a href="https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-itemstack">ItemStack Config</a>
 * @param <VALUE> to use
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>Registry keys are plain {@code namespace:path} strings, see {@link ResourceConfigList}.</li>
 *     <li>Item meta values ({@code domain:path@meta}) no longer exist in 26.3, such entries fall back to
 *     the plain item key.</li>
 * </ul>
 */
public abstract class ItemStackConfigList<VALUE> extends ResourceConfigList<ItemStackConfigList, ItemStack, VALUE> {

    /** Legacy 1.12 {@code domain:path@meta} entries; meta is ignored in 26.3. */
    protected static final java.util.regex.Pattern LEGACY_META_REGEX =
        java.util.regex.Pattern.compile("^([^=\s@]+):([^=\s@]*)@([0-9]+)$");

    public ItemStackConfigList(String name, Consumer<ItemStackConfigList> reloadCallback) {
        super(name, "https://github.com/BuiltBrokenModding/ICBM-Classic/wiki/config-itemstack", reloadCallback);
        addMatcher(LEGACY_META_REGEX, this::handleMeta);
        addMatcher(KEY_VALUE_REGEX, this::handleSimple);
    }

    @Override
    public VALUE getValue(ItemStack state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        //TODO find a way to cache for faster performance
        return super.getValue(state);
    }

    public void setDefaultMeta(ItemStack content, VALUE value, int order) {
        final String key = getContentKey(content);
        defaultMatchers
            .computeIfAbsent(key, k -> new java.util.ArrayList<>())
            .add(new ResourceConfigEntry<>("default_metadata", order, (itemStack) -> ItemStack.isSameItem(itemStack, content) ? value : null));
    }

    //TODO add support for NBT

    protected ResourceConfigEntry<ItemStack, VALUE> handleMeta(Matcher regexMatcher, String source, String entry, Integer index) {
        //26.3 has no item meta, we can only resolve the item itself
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

        warn(source, entry, "Meta values are not supported in 26.3, '" + key + "' will match all sub-items");
        final Function<ItemStack, VALUE> matcher = getSimpleValue(key, parseValue(source, entry, regexMatcher.group(3)));
        return new ResourceConfigEntry<>("resource_simple", index, matcher).setKey(key);
    }

    @Override
    protected boolean isValidKey(String targetKey) {
        return RegistryLookup.isItem(targetKey);
    }

    @Override
    protected String getContentKey(ItemStack itemStack) {
        return RegistryLookup.keyOf(itemStack.getItem());
    }

    public static class ContainsCheck extends ItemStackConfigList<Boolean> {

        public ContainsCheck(String name, Consumer<ItemStackConfigList> reloadCallback) {
            super(name, reloadCallback);
        }

        public boolean isContained(ItemStack stack) {
            Boolean value = super.getValue(stack);
            return value != null && value;
        }

        public boolean isAllowed(ItemStack stack, boolean ban) {
            return ban && !isContained(stack) || !ban && isContained(stack);
        }

        @Override
        protected Function<ItemStack, Boolean> getDomainValue(String domain, Boolean disable) {
            if(Boolean.TRUE.equals(disable)) {
                return null;
            }
            return super.getDomainValue(domain, true);
        }

        @Override
        protected Function<ItemStack, Boolean> getSimpleValue(String targetKey, Boolean disable) {
            if(Boolean.TRUE.equals(disable)) {
                return null;
            }
            return super.getSimpleValue(targetKey, true);
        }

        @Override
        protected Boolean parseValue(String source, String entry, String value) {
            return Boolean.parseBoolean(value);
        }
    }

    public static class IntOut extends ItemStackConfigList<Integer> {

        public IntOut(String name, Consumer<ItemStackConfigList> reloadCallback) {
            super(name, reloadCallback);
        }

        @Override
        protected Integer parseValue(String source, String entry, String value) {
            try {
                return Integer.parseInt(value, 10);
            }
            catch (NumberFormatException e) {
                error(source, entry, "Value is not an integer");
            }
            return null;
        }
    }

    public static class FloatOut extends ItemStackConfigList<Float> {

        public FloatOut(String name, Consumer<ItemStackConfigList> reloadCallback) {
            super(name, reloadCallback);
        }

        @Override
        protected Float parseValue(String source, String entry, String value) {
            try {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException e) {
                error(source, entry, "Value is not a Float");
            }
            return null;
        }
    }
}
