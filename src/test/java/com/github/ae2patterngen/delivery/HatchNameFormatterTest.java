package com.github.ae2patterngen.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * 总成命名格式测试：拼接、去空、NC 折叠与长度收口。
 */
public class HatchNameFormatterTest {

    @Test
    public void circuitOnlyName() {
        assertEquals(
            "Bender_Circuit 1",
            HatchNameFormatter.format("Bender", "Circuit 1", Collections.<String>emptyList()));
    }

    @Test
    public void noCircuitName() {
        assertEquals("Bender_NoCircuit", HatchNameFormatter.format("Bender", "NoCircuit", null));
    }

    @Test
    public void ncItemsAreAppendedWithPrefix() {
        assertEquals(
            "Bender_Circuit 2_NC_MoldA+MoldB",
            HatchNameFormatter.format("Bender", "Circuit 2", Arrays.asList("MoldA", "MoldB")));
    }

    @Test
    public void extraNcItemsCollapseIntoCount() {
        String name = HatchNameFormatter.format("Bender", "Circuit 1", Arrays.asList("A", "B", "C", "D", "E"));

        assertTrue(name, name.startsWith("Bender_Circuit 1_NC_A+B+C+"));
        assertTrue(name, name.endsWith("2more"));
    }

    @Test
    public void blankTokensAreSkipped() {
        assertEquals("Bender", HatchNameFormatter.format("Bender", "  ", Arrays.asList("  ", "")));
        assertEquals("", HatchNameFormatter.format(null, null, null));
    }

    @Test
    public void longNamesAreTruncated() {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            base.append("LongName");
        }

        String name = HatchNameFormatter.format(base.toString(), "Circuit 1", null);

        assertEquals(HatchNameFormatter.MAX_NAME_LENGTH, name.length());
    }

    @Test
    public void ncTokenHandlesNullList() {
        assertEquals("", HatchNameFormatter.ncToken(null));
        assertEquals("", HatchNameFormatter.ncToken(Collections.<String>emptyList()));
    }
}
