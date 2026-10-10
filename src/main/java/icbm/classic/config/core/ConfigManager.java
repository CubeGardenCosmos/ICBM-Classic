package icbm.classic.config.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import icbm.classic.config.ConfigBattery;
import icbm.classic.config.ConfigClient;
import icbm.classic.config.ConfigDebug;
import icbm.classic.config.ConfigFlyingBlocks;
import icbm.classic.config.ConfigItems;
import icbm.classic.config.ConfigMain;
import icbm.classic.config.ConfigThread;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.config.blocks.ConfigBlocks;
import icbm.classic.config.machines.ConfigEmpTower;
import icbm.classic.config.machines.ConfigLauncher;
import icbm.classic.config.machines.ConfigRadar;
import icbm.classic.config.missile.ConfigMissile;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reads and writes the JSON configuration files located in {@code config/icbmclassic}.
 *
 * <p>Each configuration class declares its file using {@link IcbmConfig} and each configurable field
 * uses {@link ConfigEntry} (optionally with {@link ConfigRange}). After applying the values the file is
 * written back so newly added fields appear on disk, missing values are filled with the field defaults
 * and out of range numbers are clamped.
 *
 * <p>Replaces the Forge 1.12 {@code @Config} system (see ADR 0006 and porting-guide.md §6).
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ICBM-Classic/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter BROKEN_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String COMMENT_PREFIX = "_comment_";

    /** Folder under the Fabric config directory that holds the ICBM JSON files. */
    public static final String CONFIG_FOLDER = "icbmclassic";

    /**
     * All configuration classes, sorted by their 1.12 config file name (so load order is stable).
     * New config classes must be added here, otherwise they are never loaded.
     */
    public static final List<Class<?>> CONFIG_CLASSES = List.of(
            ConfigBattery.class,      // battery
            ConfigBlast.class,        // blast
            ConfigBlocks.class,       // blocks
            ConfigClient.class,       // client
            ConfigDebug.class,        // debug
            ConfigEmpTower.class,     // emp_tower
            ConfigFlyingBlocks.class, // flying_blocks
            ConfigItems.class,        // item
            ConfigLauncher.class,     // launcher
            ConfigMain.class,         // main
            ConfigMissile.class,      // missile
            ConfigRadar.class,        // radar
            ConfigThread.class        // thread
    );

    private ConfigManager() {
    }

    /** Reads (and writes back) every config file into the Fabric config directory. */
    public static void loadAll() {
        loadAll(FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FOLDER));
    }

    /** Reads every config class from the given directory. */
    public static void loadAll(Path dir) {
        for (Class<?> configClass : CONFIG_CLASSES) {
            load(configClass, dir);
        }
    }

    /**
     * Reads a single config class from {@code dir/<file>.json}, applying the values to its fields and
     * writing the canonical file back. Visible for tests, which pass a temporary directory.
     */
    static void load(Class<?> cls, Path dir) {
        final IcbmConfig meta = cls.getAnnotation(IcbmConfig.class);
        if (meta == null) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is missing @IcbmConfig");
        }

        final Path path = dir.resolve(meta.file() + ".json");
        try {
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory for {}", path, e);
            return;
        }

        JsonObject input = new JsonObject();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                final JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    input = element.getAsJsonObject();
                } else {
                    throw new JsonSyntaxException("Config root is not a JSON object");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read config file {} ({}); backing up and using defaults", path, e.toString());
                backupBroken(path);
                input = new JsonObject();
            }
        }

        final JsonObject output = new JsonObject();
        processFields(cls, null, input, output);

        try {
            Files.writeString(path, GSON.toJson(output), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to write config file {}", path, e);
        }
    }

    /**
     * Walks the fields of {@code cls} in declaration order, copying values from {@code input} onto the
     * matching fields and producing the canonical {@code output} object (including comment entries).
     *
     * @param instance null when processing a top level (static) config class, otherwise the nested object
     */
    private static void processFields(Class<?> cls, Object instance, JsonObject input, JsonObject output) {
        for (Field field : cls.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            // Top level classes use static fields, nested objects use instance fields.
            if (Modifier.isStatic(field.getModifiers()) != (instance == null)) {
                continue;
            }
            final ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) {
                continue;
            }

            final String name = entry.name();
            try {
                if (isLeaf(field.getType())) {
                    final Object value = readLeaf(field, name, input, field.get(instance));
                    field.set(instance, value);
                    addComment(output, name, entry.comment());
                    output.add(name, toJson(value));
                } else {
                    final Object nested = field.get(instance);
                    if (nested == null) {
                        LOGGER.warn("Config field {}.{} is null; skipping", cls.getName(), field.getName());
                        continue;
                    }
                    final JsonObject nestedInput = input.has(name) && input.get(name).isJsonObject()
                            ? input.getAsJsonObject(name)
                            : new JsonObject();
                    final JsonObject nestedOutput = new JsonObject();
                    processFields(field.getType(), nested, nestedInput, nestedOutput);
                    addComment(output, name, entry.comment());
                    output.add(name, nestedOutput);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to process config field {}.{}", cls.getName(), field.getName(), e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object readLeaf(Field field, String name, JsonObject input, Object current) {
        final JsonElement element = input.get(name);
        if (element == null || element.isJsonNull()) {
            return current;
        }
        final Class<?> type = field.getType();

        if (type == int.class) {
            if (isNumber(element)) {
                return (int) clamp(field, name, element.getAsDouble());
            }
        } else if (type == long.class) {
            if (isNumber(element)) {
                return (long) clamp(field, name, element.getAsDouble());
            }
        } else if (type == float.class) {
            if (isNumber(element)) {
                return (float) clamp(field, name, element.getAsDouble());
            }
        } else if (type == double.class) {
            if (isNumber(element)) {
                return clamp(field, name, element.getAsDouble());
            }
        } else if (type == boolean.class) {
            if (isBoolean(element)) {
                return element.getAsBoolean();
            }
        } else if (type == String.class) {
            if (isString(element)) {
                return element.getAsString();
            }
        } else if (type == String[].class) {
            if (element.isJsonArray()) {
                final JsonArray array = element.getAsJsonArray();
                final String[] values = new String[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    final JsonElement item = array.get(i);
                    if (!isString(item)) {
                        warnType(field, element);
                        return current;
                    }
                    values[i] = item.getAsString();
                }
                return values;
            }
        } else if (type.isEnum()) {
            if (isString(element)) {
                try {
                    return Enum.valueOf((Class) type, element.getAsString());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Config {}.{} has unknown enum value '{}'; keeping default",
                            field.getDeclaringClass().getSimpleName(), name, element.getAsString());
                    return current;
                }
            }
        }

        warnType(field, element);
        return current;
    }

    private static double clamp(Field field, String name, double value) {
        final ConfigRange range = field.getAnnotation(ConfigRange.class);
        if (range == null) {
            return value;
        }
        if (value < range.min()) {
            LOGGER.warn("Config {}.{} value {} is below minimum {}; clamped",
                    field.getDeclaringClass().getSimpleName(), name, value, range.min());
            return range.min();
        }
        if (value > range.max()) {
            LOGGER.warn("Config {}.{} value {} is above maximum {}; clamped",
                    field.getDeclaringClass().getSimpleName(), name, value, range.max());
            return range.max();
        }
        return value;
    }

    private static boolean isLeaf(Class<?> type) {
        return type == int.class || type == long.class || type == float.class || type == double.class
                || type == boolean.class || type == String.class || type == String[].class || type.isEnum();
    }

    private static boolean isNumber(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static boolean isBoolean(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
    }

    private static void warnType(Field field, JsonElement element) {
        LOGGER.warn("Config {}.{} has wrong JSON type ({}); keeping default",
                field.getDeclaringClass().getSimpleName(), field.getName(), element);
    }

    private static void addComment(JsonObject output, String name, String comment) {
        if (comment != null && !comment.isEmpty()) {
            output.addProperty(COMMENT_PREFIX + name, comment);
        }
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        }
        if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        }
        if (value instanceof String) {
            return new JsonPrimitive((String) value);
        }
        if (value instanceof Enum) {
            return new JsonPrimitive(((Enum<?>) value).name());
        }
        if (value instanceof String[]) {
            final JsonArray array = new JsonArray();
            for (String entry : (String[]) value) {
                array.add(entry);
            }
            return array;
        }
        return JsonNull.INSTANCE;
    }

    private static void backupBroken(Path path) {
        try {
            final String stamp = LocalDateTime.now().format(BROKEN_STAMP);
            Path broken = path.resolveSibling(path.getFileName().toString() + ".broken-" + stamp);
            // Same minute can be hit twice, keep looking for a free name instead of overwriting a backup
            int attempt = 1;
            while (Files.exists(broken)) {
                broken = path.resolveSibling(path.getFileName().toString() + ".broken-" + stamp + "-" + attempt++);
            }
            Files.move(path, broken);
        } catch (IOException e) {
            LOGGER.error("Failed to back up broken config {}", path, e);
        }
    }
}
