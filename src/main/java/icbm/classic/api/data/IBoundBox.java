package icbm.classic.api.data;

/**
 * Box defined by a lower and upper bound of a positional type.
 *
 * @param <POS> positional type, usually {@code BlockPos}
 */
public interface IBoundBox<POS> {
    POS lowerBound();
    POS upperBound();
}
