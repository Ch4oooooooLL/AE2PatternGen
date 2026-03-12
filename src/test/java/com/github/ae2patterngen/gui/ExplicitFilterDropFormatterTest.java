package com.github.ae2patterngen.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

public class ExplicitFilterDropFormatterTest {

    @Test
    public void formatsIdAndMetaWhenMetaIsPresent() {
        assertEquals("[8119:12]", invokeFormat(8119, Integer.valueOf(12), new String[0], "Copper Dust"));
    }

    @Test
    public void formatsIdOnlyWhenMetaIsMissing() {
        assertEquals("[8119]", invokeFormat(8119, null, new String[] { "dustCopper" }, "Copper Dust"));
    }

    @Test
    public void fallsBackToFirstOreNameWhenIdIsUnavailable() {
        assertEquals("(dustCopper)", invokeFormat(-1, null, new String[] { "dustCopper", "dustAnyCopper" }, ""));
    }

    @Test
    public void fallsBackToEscapedDisplayNameWhenOnlyNameIsAvailable() {
        assertEquals("{Copper Dust \\[Refined\\]}", invokeFormat(-1, null, new String[0], "Copper Dust [Refined]"));
    }

    @Test
    public void escapesRegexSpecialCharactersInOreTokens() {
        assertEquals("(dustCopper\\+)", invokeFormat(-1, null, new String[] { "dustCopper+" }, ""));
    }

    private String invokeFormat(int itemId, Integer meta, String[] oreNames, String displayName) {
        try {
            Class<?> clazz = Class.forName("com.github.ae2patterngen.gui.ExplicitFilterDropFormatter");
            Method method = clazz.getDeclaredMethod("format", int.class, Integer.class, String[].class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, itemId, meta, oreNames, displayName);
        } catch (ClassNotFoundException e) {
            fail("Expected ExplicitFilterDropFormatter to exist");
        } catch (NoSuchMethodException e) {
            fail("Expected ExplicitFilterDropFormatter.format(int, Integer, String[], String) helper");
        } catch (IllegalAccessException e) {
            fail("Unable to access ExplicitFilterDropFormatter helper: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("ExplicitFilterDropFormatter helper threw unexpectedly: " + cause.getMessage());
        }
        return "";
    }
}
