package com.github.ae2patterngen.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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

    @Test
    public void idOnlyMatcherDoesNotLoadOreOrDisplayData() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("[8119]");
        AtomicInteger oreLoads = new AtomicInteger();
        AtomicInteger displayLoads = new AtomicInteger();

        assertTrue(
            invokeLazyMatch(matcher, 8119, 12, oreLoads, displayLoads, new String[] { "dustCopper" }, "Copper Dust"));
        assertTrue("id-only matcher should not load ore names", oreLoads.get() == 0);
        assertTrue("id-only matcher should not load display name", displayLoads.get() == 0);
    }

    @Test
    public void oreOnlyMatcherDoesNotLoadDisplayData() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("(dustCopper)");
        AtomicInteger oreLoads = new AtomicInteger();
        AtomicInteger displayLoads = new AtomicInteger();

        assertTrue(
            invokeLazyMatch(matcher, 8119, 12, oreLoads, displayLoads, new String[] { "dustCopper" }, "Copper Dust"));
        assertTrue("ore-only matcher should load ore names", oreLoads.get() > 0);
        assertTrue("ore-only matcher should not load display name", displayLoads.get() == 0);
    }

    @Test
    public void displayOnlyMatcherDoesNotLoadOreData() {
        ExplicitStackMatcher matcher = new ExplicitStackMatcher("{Copper Dust}");
        AtomicInteger oreLoads = new AtomicInteger();
        AtomicInteger displayLoads = new AtomicInteger();

        assertTrue(
            invokeLazyMatch(matcher, 8119, 12, oreLoads, displayLoads, new String[] { "dustCopper" }, "Copper Dust"));
        assertTrue("display-only matcher should not load ore names", oreLoads.get() == 0);
        assertTrue("display-only matcher should load display name", displayLoads.get() > 0);
    }

    private boolean invokeLazyMatch(ExplicitStackMatcher matcher, int itemId, int meta, AtomicInteger oreLoads,
        AtomicInteger displayLoads, String[] oreNames, String displayName) {
        try {
            Method method = ExplicitStackMatcher.class
                .getDeclaredMethod("matches", int.class, int.class, Supplier.class, Supplier.class);
            method.setAccessible(true);

            Supplier<String[]> oreSupplier = () -> {
                oreLoads.incrementAndGet();
                return oreNames;
            };
            Supplier<String> displaySupplier = () -> {
                displayLoads.incrementAndGet();
                return displayName;
            };

            return ((Boolean) method.invoke(matcher, itemId, meta, oreSupplier, displaySupplier)).booleanValue();
        } catch (NoSuchMethodException e) {
            fail("Expected lazy matching overload for token-specific matching optimization");
        } catch (IllegalAccessException e) {
            fail("Unable to access lazy matching overload: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("Lazy matching overload threw unexpectedly: " + cause.getMessage());
        }
        return false;
    }
}
