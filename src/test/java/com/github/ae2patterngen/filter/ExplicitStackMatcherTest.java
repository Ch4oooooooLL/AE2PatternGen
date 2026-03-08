package com.github.ae2patterngen.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExplicitStackMatcherTest {

    @Test
    public void asteriskDisablesFiltering() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("*");

        assertTrue(matcher.isDisabled());
        assertFalse(matcher.isInvalid());
        assertFalse(matcher.matches("Copper Dust", 8119, 12, new String[] { "dustCopper" }));
    }

    @Test
    public void idTokenMatchesByIdOnly() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119]");

        assertFalse(matcher.isDisabled());
        assertFalse(matcher.isInvalid());
        assertTrue(matcher.matches("Copper Dust", 8119, 0, new String[0]));
        assertTrue(matcher.matches("Copper Dust", 8119, 12, new String[0]));
        assertFalse(matcher.matches("Copper Dust", 8120, 12, new String[0]));
    }

    @Test
    public void oreTokenMatchesOnlyOreNames() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("(dustCopper)");

        assertTrue(matcher.matches("Machine Part", 8119, 0, new String[] { "dustCopper" }));
        assertFalse(matcher.matches("dustCopper", 8119, 0, new String[0]));
    }

    @Test
    public void displayTokenMatchesOnlyDisplayName() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("{Copper Dust}");

        assertTrue(matcher.matches("Copper Dust", 8119, 0, new String[0]));
        assertFalse(matcher.matches("Machine Part", 8119, 0, new String[] { "Copper Dust" }));
    }

    @Test
    public void bareTextIsInvalid() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("dustCopper");

        assertFalse(matcher.isDisabled());
        assertTrue(matcher.isInvalid());
        assertFalse(matcher.matches("dustCopper", 8119, 0, new String[] { "dustCopper" }));
    }

    @Test
    public void explicitTokensSupportOrSemantics() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119] (dustTin) {Machine Part}");

        assertTrue(matcher.matches("Other", 8119, 0, new String[0]));
        assertTrue(matcher.matches("Other", 1, 0, new String[] { "dustTin" }));
        assertTrue(matcher.matches("Machine Part", 1, 0, new String[0]));
        assertFalse(matcher.matches("Other", 1, 0, new String[] { "dustCopper" }));
    }
}
