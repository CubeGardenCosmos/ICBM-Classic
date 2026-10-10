package icbm.classic.config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a configurable entry. {@link #name()} is the JSON key and {@link #comment()} is
 * written to the sibling {@code _comment_<name>} entry (omitted when empty).
 *
 * <p>Replaces the Forge 1.12 {@code @Config.Name} + {@code @Config.Comment} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    /** JSON key for the entry. */
    String name();

    /** Human readable comment written next to the entry. */
    String comment() default "";
}
