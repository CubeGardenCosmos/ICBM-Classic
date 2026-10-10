package icbm.classic.config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts a numeric config entry to the inclusive range {@code [min, max]}. Values read from the
 * file that fall outside the range are clamped and a warning is logged.
 *
 * <p>Replaces the Forge 1.12 {@code @Config.RangeInt} / {@code @Config.RangeDouble} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigRange {

    /** Inclusive minimum. */
    double min();

    /** Inclusive maximum. */
    double max();
}
