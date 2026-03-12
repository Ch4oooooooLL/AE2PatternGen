package com.github.ae2patterngen.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class FilterTextFieldWidgetTest {

    @Test
    public void successfulDropReplacesCurrentText() {
        Object widget = newWidget(stack -> "[8119:12]");

        invokeSetText(widget, "old");

        assertTrue(invokeHandleDragAndDrop(widget, new ItemStack(new Item()), 0));
        assertEquals("[8119:12]", invokeGetText(widget));
    }

    @Test
    public void nullStackIsRejected() {
        Object widget = newWidget(stack -> "[8119]");

        invokeSetText(widget, "keep");

        assertFalse(invokeHandleDragAndDrop(widget, null, 0));
        assertEquals("keep", invokeGetText(widget));
    }

    @Test
    public void emptyFormattedTokenIsRejected() {
        Object widget = newWidget(stack -> "");

        invokeSetText(widget, "keep");

        assertFalse(invokeHandleDragAndDrop(widget, new ItemStack(new Item()), 0));
        assertEquals("keep", invokeGetText(widget));
    }

    private Object newWidget(Function<ItemStack, String> formatter) {
        try {
            Class<?> clazz = Class.forName("com.github.ae2patterngen.gui.FilterTextFieldWidget");
            Constructor<?> ctor = clazz.getDeclaredConstructor(Function.class);
            ctor.setAccessible(true);
            return ctor.newInstance(formatter);
        } catch (ClassNotFoundException e) {
            fail("Expected FilterTextFieldWidget to exist");
        } catch (NoSuchMethodException e) {
            fail("Expected FilterTextFieldWidget(Function<ItemStack, String>) constructor");
        } catch (InstantiationException e) {
            fail("Unable to instantiate FilterTextFieldWidget: " + e.getMessage());
        } catch (IllegalAccessException e) {
            fail("Unable to access FilterTextFieldWidget constructor: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("FilterTextFieldWidget constructor threw unexpectedly: " + cause.getMessage());
        }
        return null;
    }

    private boolean invokeHandleDragAndDrop(Object widget, ItemStack stack, int button) {
        try {
            Method method = widget.getClass().getMethod("handleDragAndDrop", ItemStack.class, int.class);
            return ((Boolean) method.invoke(widget, stack, button)).booleanValue();
        } catch (NoSuchMethodException e) {
            fail("Expected FilterTextFieldWidget.handleDragAndDrop(ItemStack, int)");
        } catch (IllegalAccessException e) {
            fail("Unable to access handleDragAndDrop: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("handleDragAndDrop threw unexpectedly: " + cause.getMessage());
        }
        return false;
    }

    private void invokeSetText(Object widget, String text) {
        try {
            Method method = widget.getClass().getMethod("setText", String.class);
            method.invoke(widget, text);
        } catch (NoSuchMethodException e) {
            fail("Expected TextFieldWidget.setText(String)");
        } catch (IllegalAccessException e) {
            fail("Unable to access setText: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("setText threw unexpectedly: " + cause.getMessage());
        }
    }

    private String invokeGetText(Object widget) {
        try {
            Method method = widget.getClass().getMethod("getText");
            return (String) method.invoke(widget);
        } catch (NoSuchMethodException e) {
            fail("Expected TextFieldWidget.getText()");
        } catch (IllegalAccessException e) {
            fail("Unable to access getText: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("getText threw unexpectedly: " + cause.getMessage());
        }
        return "";
    }
}
