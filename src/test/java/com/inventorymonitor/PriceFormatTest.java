package com.inventorymonitor;

import static org.junit.jupiter.api.Assertions.*;

import com.inventorymonitor.ui.PriceFormat;

import org.junit.jupiter.api.Test;

class PriceFormatTest {
    @Test
    void formatsWholeShillingsWithThousandsSeparators() {
        assertEquals("4,000", PriceFormat.format(4000));
        assertEquals("15,000", PriceFormat.format(15000));
        assertEquals("1,000,000", PriceFormat.format(1000000));
        assertEquals("2,500,000", PriceFormat.format(2500000));
        assertEquals("0", PriceFormat.format(0));
    }

    @Test
    void acceptsPlainAndFormattedPriceInputs() {
        assertEquals(1000000L, PriceFormat.parse("1000000"));
        assertEquals(1000000L, PriceFormat.parse("1,000,000"));
        assertEquals(15000L, PriceFormat.parse(" 15,000 "));
    }

    @Test
    void rejectsInvalidPriceInputs() {
        for (String input : new String[] {"", "-1", "1.50", "1,00", "1 000", "999999999999999999999"})
            assertThrows(IllegalArgumentException.class, () -> PriceFormat.parse(input), input);
    }
}
