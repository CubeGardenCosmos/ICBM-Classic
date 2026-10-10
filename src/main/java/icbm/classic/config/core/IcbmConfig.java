package icbm.classic.config.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a JSON configuration root. The {@link #file()} value is the file name (without
 * {@code .json}) resolved against the {@code config/icbmclassic} directory. The value may contain
 * {@code /} to place the file in a sub directory.
 *
 * <p>Replaces the Forge 1.12 {@code @Config(modid = ..., name = "icbmclassic/<file>")} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IcbmConfig {

    /** File name (without extension) relative to {@code config/icbmclassic}. */
    String file();
}
