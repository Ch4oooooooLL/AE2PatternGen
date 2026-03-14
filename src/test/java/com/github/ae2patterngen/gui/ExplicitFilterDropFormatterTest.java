package com.github.ae2patterngen.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void buildChoicesPreservesPriorityAcrossAllSources() {
        Object choices = invokeBuildChoices(8119, Integer.valueOf(12), new String[] { "dustCopper" }, "Copper Dust");

        assertNotNull("Expected non-null choices object", choices);
        assertEquals(0, invokeDefaultIndex(choices));
        assertEquals(listOf("[8119:12]", "(dustCopper)", "{Copper Dust}"), invokeChoiceTokens(choices));
        assertEquals(listOf("ITEM_ID", "ORE_DICT", "DISPLAY_NAME"), invokeChoiceSources(choices));
    }

    @Test
    public void buildChoicesSkipsBlankSources() {
        Object choices = invokeBuildChoices(-1, null, new String[] { "", "   " }, "  Copper Dust [Refined]  ");

        assertNotNull("Expected non-null choices object", choices);
        assertEquals(0, invokeDefaultIndex(choices));
        assertEquals(listOf("{Copper Dust \\[Refined\\]}"), invokeChoiceTokens(choices));
        assertEquals(listOf("DISPLAY_NAME"), invokeChoiceSources(choices));
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

    private Object invokeBuildChoices(int itemId, Integer meta, String[] oreNames, String displayName) {
        try {
            Class<?> clazz = Class.forName("com.github.ae2patterngen.gui.ExplicitFilterDropFormatter");
            Method method = clazz
                .getDeclaredMethod("buildChoices", int.class, Integer.class, String[].class, String.class);
            method.setAccessible(true);
            return method.invoke(null, itemId, meta, oreNames, displayName);
        } catch (ClassNotFoundException e) {
            fail("Expected ExplicitFilterDropFormatter to exist");
        } catch (NoSuchMethodException e) {
            fail("Expected ExplicitFilterDropFormatter.buildChoices(int, Integer, String[], String)");
        } catch (IllegalAccessException e) {
            fail("Unable to access ExplicitFilterDropFormatter.buildChoices: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("ExplicitFilterDropFormatter.buildChoices threw unexpectedly: " + cause.getMessage());
        }
        return null;
    }

    private int invokeDefaultIndex(Object choices) {
        try {
            Method method = choices.getClass()
                .getDeclaredMethod("getDefaultIndex");
            method.setAccessible(true);
            return ((Integer) method.invoke(choices)).intValue();
        } catch (NoSuchMethodException e) {
            fail("Expected choices.getDefaultIndex()");
        } catch (IllegalAccessException e) {
            fail("Unable to access getDefaultIndex: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("getDefaultIndex threw unexpectedly: " + cause.getMessage());
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeChoiceTokens(Object choices) {
        try {
            Method method = choices.getClass()
                .getDeclaredMethod("getOptions");
            method.setAccessible(true);
            List<Object> options = (List<Object>) method.invoke(choices);
            List<String> tokens = new ArrayList<String>();
            for (Object option : options) {
                Method tokenMethod = option.getClass()
                    .getDeclaredMethod("getToken");
                tokenMethod.setAccessible(true);
                tokens.add((String) tokenMethod.invoke(option));
            }
            return tokens;
        } catch (NoSuchMethodException e) {
            fail("Expected choices.getOptions() and option.getToken()");
        } catch (IllegalAccessException e) {
            fail("Unable to access token methods: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("Token access threw unexpectedly: " + cause.getMessage());
        }
        return new ArrayList<String>();
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeChoiceSources(Object choices) {
        try {
            Method method = choices.getClass()
                .getDeclaredMethod("getOptions");
            method.setAccessible(true);
            List<Object> options = (List<Object>) method.invoke(choices);
            List<String> sources = new ArrayList<String>();
            for (Object option : options) {
                Method sourceMethod = option.getClass()
                    .getDeclaredMethod("getSource");
                sourceMethod.setAccessible(true);
                Object source = sourceMethod.invoke(option);
                sources.add(String.valueOf(source));
            }
            return sources;
        } catch (NoSuchMethodException e) {
            fail("Expected choices.getOptions() and option.getSource()");
        } catch (IllegalAccessException e) {
            fail("Unable to access source methods: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("Source access threw unexpectedly: " + cause.getMessage());
        }
        return new ArrayList<String>();
    }

    private List<String> listOf(String... values) {
        List<String> result = new ArrayList<String>();
        if (values != null) {
            for (String value : values) {
                result.add(value);
            }
        }
        return result;
    }
}
