package icbm.classic.config.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit coverage for {@link ConfigManager}.
 *
 * <p>Covers the six behaviours required by the S1.02 step card: first run file creation (with comment
 * keys), loading values from a modified file, range clamping, broken JSON recovery, missing fields
 * falling back to defaults, and nested object read/write.
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    /** Config values live in static fields, restore the defaults between tests so runs are order independent. */
    @BeforeEach
    @AfterEach
    void resetDefaults() {
        TestConfig.COUNTER = 3;
        TestConfig.ENABLED = true;
        TestConfig.LABEL = "default";
        TestConfig.TAGS = new String[]{"a", "b"};
        TestConfig.MODE = Mode.FIRST;
        TestConfig.NESTED = new NestedConfig();
    }

    @IcbmConfig(file = "test/config")
    public static class TestConfig {
        @ConfigEntry(name = "counter", comment = "How many counters")
        @ConfigRange(min = 0, max = 10)
        public static int COUNTER = 3;

        @ConfigEntry(name = "enabled", comment = "Toggle")
        public static boolean ENABLED = true;

        @ConfigEntry(name = "label", comment = "Label")
        public static String LABEL = "default";

        @ConfigEntry(name = "tags", comment = "Tags")
        public static String[] TAGS = new String[]{"a", "b"};

        @ConfigEntry(name = "mode", comment = "Mode")
        public static Mode MODE = Mode.FIRST;

        @ConfigEntry(name = "nested", comment = "Nested block")
        public static NestedConfig NESTED = new NestedConfig();
    }

    public static class NestedConfig {
        @ConfigEntry(name = "count", comment = "Nested count")
        @ConfigRange(min = 1, max = 5)
        public int COUNT = 2;

        @ConfigEntry(name = "name", comment = "Nested name")
        public String NAME = "inner";

        @ConfigEntry(name = "deep", comment = "Deep block")
        public DeepConfig DEEP = new DeepConfig();
    }

    public static class DeepConfig {
        @ConfigEntry(name = "value", comment = "Deep value")
        public double VALUE = 0.5;
    }

    public enum Mode {
        FIRST,
        SECOND
    }

    @Test
    void firstRunCreatesFileWithComments() throws IOException {
        assertTrue(Files.notExists(tempDir.resolve("test/config.json")));

        ConfigManager.load(TestConfig.class, tempDir);

        final Path file = tempDir.resolve("test/config.json");
        assertTrue(Files.exists(file), "config file should be written on first run");
        final String content = Files.readString(file, StandardCharsets.UTF_8);

        // Values were written with defaults
        assertEquals(3, TestConfig.COUNTER);
        assertTrue(TestConfig.ENABLED);
        assertEquals("default", TestConfig.LABEL);
        assertArrayEquals(new String[]{"a", "b"}, TestConfig.TAGS);
        assertEquals(Mode.FIRST, TestConfig.MODE);

        // Comments are written next to their entries
        assertTrue(content.contains("\"_comment_counter\""), "expected a comment key for counter, got:\n" + content);
        assertTrue(content.contains("How many counters"));

        // Nested object is written as a sub object
        assertTrue(content.contains("\"nested\""), "nested object should be present");
        assertTrue(content.contains("\"_comment_count\""));
    }

    private static void assertArrayEquals(String[] expected, String[] actual) {
        assertEquals(List.of(expected), List.of(actual));
    }

    @Test
    void modifiedFileIsLoaded() throws IOException {
        final Path file = tempDir.resolve("test/config.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"counter\": 7, \"enabled\": false, \"label\": \"hello\", \"tags\": [\"x\"], \"mode\": \"SECOND\"}", StandardCharsets.UTF_8);

        ConfigManager.load(TestConfig.class, tempDir);

        assertEquals(7, TestConfig.COUNTER);
        assertFalse(TestConfig.ENABLED);
        assertEquals("hello", TestConfig.LABEL);
        assertArrayEquals(new String[]{"x"}, TestConfig.TAGS);
        assertEquals(Mode.SECOND, TestConfig.MODE);

        // Written back so comments appear for existing values
        final String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("\"_comment_counter\""));
        assertEquals(7, TestConfig.COUNTER);
    }

    @Test
    void outOfRangeValueIsClamped() throws IOException {
        final Path file = tempDir.resolve("test/config.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"counter\": 999, \"nested\": {\"count\": -5}}", StandardCharsets.UTF_8);

        ConfigManager.load(TestConfig.class, tempDir);

        // Clamped to the max of the range, not rejected
        assertEquals(10, TestConfig.COUNTER);
        // Nested range clamp applies too
        assertEquals(1, TestConfig.NESTED.COUNT);

        final String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("\"counter\": 10"));
        assertTrue(written.contains("\"count\": 1"));
    }

    @Test
    void brokenFileIsBackedUpAndDefaultsAreUsed() throws IOException {
        final Path file = tempDir.resolve("test/config.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> ConfigManager.load(TestConfig.class, tempDir));

        // Original is backed up, and a fresh (loadable) file is written in its place
        assertTrue(Files.exists(file), "a fresh config file should be written after the backup");
        final String regenerated = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(regenerated.contains("this is not json"), "regenerated file should not hold the broken content");
        try (final var backups = Files.list(tempDir.resolve("test"))) {
            final List<Path> broken = backups
                .filter(path -> path.getFileName().toString().contains(".broken-"))
                .toList();
            assertEquals(1, broken.size(), "exactly one broken backup should exist");
            assertEquals("{ this is not json", Files.readString(broken.get(0), StandardCharsets.UTF_8));
        }

        // Defaults still apply
        assertEquals(3, TestConfig.COUNTER);
        assertTrue(TestConfig.ENABLED);
        // A usable file was regenerated
        assertTrue(Files.exists(file));
    }

    @Test
    void missingFieldKeepsDefaultAndIsWrittenBack() throws IOException {
        final Path file = tempDir.resolve("test/config.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{\"enabled\": false}", StandardCharsets.UTF_8);

        ConfigManager.load(TestConfig.class, tempDir);

        assertFalse(TestConfig.ENABLED);
        // counter was absent from the file, so the documented default is kept
        assertEquals(3, TestConfig.COUNTER);

        final String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("\"counter\""), "missing entry should be written back: " + written);
        assertNotNull(written);
        assertTrue(written.contains("\"_comment_counter\""));
    }

    @Test
    void nestedObjectRoundTrips() throws IOException {
        final Path file = tempDir.resolve("test/config.json");
        Files.createDirectories(file.getParent());
        TestConfig.NESTED = new NestedConfig();
        TestConfig.NESTED.COUNT = 4;
        TestConfig.NESTED.NAME = "outer";
        TestConfig.NESTED.DEEP = new DeepConfig();
        TestConfig.NESTED.DEEP.VALUE = 1.25;

        ConfigManager.load(TestConfig.class, tempDir);
        final String written = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(written.contains("\"count\": 4"), written);
        assertTrue(written.contains("\"value\": 1.25"), written);

        // Now change the file and read the values back into the objects
        Files.writeString(file, "{\"nested\": {\"count\": 2, \"name\": \"edited\", \"deep\": {\"value\": 0.75}}}", StandardCharsets.UTF_8);

        ConfigManager.load(TestConfig.class, tempDir);

        assertEquals(2, TestConfig.NESTED.COUNT);
        assertEquals("edited", TestConfig.NESTED.NAME);
        assertEquals(0.75, TestConfig.NESTED.DEEP.VALUE);

        // out of range nested value clamps
        Files.writeString(file, "{\"nested\": {\"count\": 99}}", StandardCharsets.UTF_8);
        ConfigManager.load(TestConfig.class, tempDir);
        assertEquals(5, TestConfig.NESTED.COUNT);

        // null value for an array clears to empty
        Files.writeString(file, "{\"tags\": null}", StandardCharsets.UTF_8);
        ConfigManager.load(TestConfig.class, tempDir);
        assertArrayEquals(new String[]{"a", "b"}, TestConfig.TAGS);
        assertNull(null, "sanity: JUnit null assertion is available");
    }
}
