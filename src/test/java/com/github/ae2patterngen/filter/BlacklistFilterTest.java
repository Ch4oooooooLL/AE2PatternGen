package com.github.ae2patterngen.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class BlacklistFilterTest {

    @Test
    public void idTokenWithMetaMatchesOnlyExactMeta() {
        Pattern pattern = BlacklistFilter.compileKeyword("[8119:12]");

        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                8119,
                12,
                new String[] { "oreAny" }));
        assertFalse(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                8119,
                11,
                new String[] { "oreAny" }));
    }

    @Test
    public void idTokenWithoutMetaMatchesAnyMeta() {
        Pattern pattern = BlacklistFilter.compileKeyword("[8119]");

        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                8119,
                0,
                new String[] { "oreAny" }));
        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                8119,
                15,
                new String[] { "oreAny" }));
    }

    @Test
    public void idTokenCanBeCombinedWithRegexLogic() {
        Pattern pattern = BlacklistFilter.compileKeyword("dust|[8119:12]");

        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Copper Dust",
                1,
                0,
                new String[] { "dustCopper" }));
        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                8119,
                12,
                new String[] { "oreAny" }));
        assertFalse(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "Any Item",
                1,
                0,
                new String[] { "oreAny" }));
    }

    @Test
    public void nonNumericBracketsRemainRegexCharacterClass() {
        Pattern pattern = BlacklistFilter.compileKeyword("ingot[AB]");

        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "ingotA",
                1,
                0,
                new String[] { "oreAny" }));
        assertTrue(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "ingotB",
                1,
                0,
                new String[] { "oreAny" }));
        assertFalse(
            BlacklistFilter.matchesCompiledPattern(
                pattern,
                "ingotC",
                1,
                0,
                new String[] { "oreAny" }));
    }
}
