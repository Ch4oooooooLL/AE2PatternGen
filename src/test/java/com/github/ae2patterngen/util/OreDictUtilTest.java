package com.github.ae2patterngen.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class OreDictUtilTest {

    @Test
    public void invalidOreIdsAreSkippedDuringNameMapping() {
        int[] oreIds = new int[] { 0, 32767, -1, 2 };
        String[] oreNames = new String[] { "oreCopper", "oreTin", "dustIron" };

        assertArrayEquals(new String[] { "oreCopper", "dustIron" }, OreDictUtil.getOreNamesSafe(oreIds, oreNames));
    }

    @Test
    public void emptySourceReturnsEmptyResult() {
        assertArrayEquals(new String[0], OreDictUtil.getOreNamesSafe(new int[0], new String[] { "oreAny" }));
        assertArrayEquals(new String[0], OreDictUtil.getOreNamesSafe(new int[] { 0 }, new String[0]));
    }
}
