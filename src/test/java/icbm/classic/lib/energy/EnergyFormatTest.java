package icbm.classic.lib.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyFormatTest
{
    @Test
    void formatsMagnitudesWithAtMostOneDecimalPlace()
    {
        assertEquals("0 FE", EnergyFormat.format(0));
        assertEquals("999 FE", EnergyFormat.format(999));
        assertEquals("1k FE", EnergyFormat.format(1_000));
        assertEquals("12.5k FE", EnergyFormat.format(12_500));
        assertEquals("1M FE", EnergyFormat.format(1_000_000));
        assertEquals("2.5G FE", EnergyFormat.format(2_500_000_000L));
    }

    @Test
    void formatsStoredAndCapacityTogether()
    {
        assertEquals("12,500 / 2,500,000,000 FE", EnergyFormat.formatWithCapacity(12_500, 2_500_000_000L));
    }
}
