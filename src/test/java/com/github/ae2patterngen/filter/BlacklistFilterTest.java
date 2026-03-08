package com.github.ae2patterngen.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlacklistFilterTest {

    @Test
    public void idTokenWithoutMetaMatchesAnyMeta() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119]");

        assertTrue(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
        assertFalse(matcher.matches("Copper Dust", 8120, 12, new String[] { "dustCopper" }));
    }

    @Test
    public void idTokenWithMetaMatchesOnlyExactMeta() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119:12]");

        assertTrue(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
        assertFalse(matcher.matches("Copper Dust", 8119, 13, new String[] { "dustCopper" }));
    }

    @Test
    public void oreDictTokenMatchesOreNamesOnly() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("(dustCopper)");

        assertTrue(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
        assertFalse(matcher.matches("dustCopper", 8119, 12, new String[0]));
    }

    @Test
    public void displayNameTokenMatchesDisplayNameOnly() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("{Copper Dust}");

        assertTrue(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
        assertFalse(matcher.matches("Machine Part", 8119, 12, new String[] { "Copper Dust" }));
    }

    @Test
    public void multipleExplicitTokensUseOrSemantics() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119] (dustCopper) {Machine Part}");

        assertTrue(matcher.matches("Anything", 8119, 0, new String[0]));
        assertTrue(matcher.matches("Anything", 1, 0, new String[] { "dustCopper" }));
        assertTrue(matcher.matches("Machine Part", 1, 0, new String[0]));
        assertFalse(matcher.matches("Other", 1, 0, new String[] { "dustTin" }));
    }

    @Test
    public void nestedRegexGroupsRemainValidInsideExplicitToken() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("(dust(?:Copper|Tin))");

        assertTrue(matcher.matches("Anything", 1, 0, new String[] { "dustTin" }));
        assertFalse(matcher.matches("Anything", 1, 0, new String[] { "dustIron" }));
    }

    @Test
    public void bareTextNoLongerMatchesAnything() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("dustCopper");

        assertTrue(matcher.isInvalid());
        assertFalse(matcher.matches("dustCopper", 8119, 12, new String[] { "dustCopper" }));
    }

    @Test
    public void invalidMixedSyntaxDisablesActualMatching() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119] dustCopper");

        assertTrue(matcher.isInvalid());
        assertFalse(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
    }
}
