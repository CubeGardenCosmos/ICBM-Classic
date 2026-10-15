package icbm.classic.lib.energy;

import java.util.Locale;

/** Formats FE values for compact machine readouts. */
public final class EnergyFormat
{
    private EnergyFormat()
    {
    }

    /** Formats an FE amount with at most one decimal digit and a magnitude suffix. */
    public static String format(long fe)
    {
        return formatValue(fe) + " FE";
    }

    /** Formats stored and maximum energy together for an exact-value tooltip. */
    public static String formatWithCapacity(long stored, long capacity)
    {
        return String.format(Locale.ROOT, "%,d / %,d FE", stored, capacity);
    }

    private static String formatValue(long fe)
    {
        final long magnitude = Math.abs(fe == Long.MIN_VALUE ? Long.MAX_VALUE : fe);
        if (magnitude < 1_000)
        {
            return Long.toString(fe);
        }
        final long divisor;
        final String suffix;
        if (magnitude < 1_000_000)
        {
            divisor = 1_000;
            suffix = "k";
        }
        else if (magnitude < 1_000_000_000)
        {
            divisor = 1_000_000;
            suffix = "M";
        }
        else
        {
            divisor = 1_000_000_000;
            suffix = "G";
        }
        final String amount = String.format(Locale.ROOT, "%.1f", (double) fe / divisor);
        return (amount.endsWith(".0") ? amount.substring(0, amount.length() - 2) : amount) + suffix;
    }

}
