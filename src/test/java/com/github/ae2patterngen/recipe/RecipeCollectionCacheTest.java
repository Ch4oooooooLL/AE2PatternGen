package com.github.ae2patterngen.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

public class RecipeCollectionCacheTest {

    @Test
    public void repeatedLookupsComputeOnlyOnce() throws Exception {
        Object cache = newCacheInstance();
        Method getOrCompute = getOrComputeMethod(cache);
        AtomicInteger computeCount = new AtomicInteger();

        Supplier<Object> supplier = () -> {
            computeCount.incrementAndGet();
            return "cached-value";
        };

        Object first = getOrCompute.invoke(cache, "assembler", supplier);
        Object second = getOrCompute.invoke(cache, "assembler", supplier);

        assertEquals("value should be computed once", 1, computeCount.get());
        assertSame("same cached instance should be returned", first, second);
    }

    @Test
    public void clearDropsCachedEntries() throws Exception {
        Object cache = newCacheInstance();
        Method getOrCompute = getOrComputeMethod(cache);
        Method clear = getMethod(cache.getClass(), "clear");
        AtomicInteger computeCount = new AtomicInteger();

        Supplier<Object> supplier = () -> {
            computeCount.incrementAndGet();
            return "cached-value-" + computeCount.get();
        };

        Object first = getOrCompute.invoke(cache, "assembler", supplier);
        clear.invoke(cache);
        Object second = getOrCompute.invoke(cache, "assembler", supplier);

        assertEquals("value should be recomputed after clear", 2, computeCount.get());
        org.junit.Assert.assertNotSame("cleared cache should not reuse old entry", first, second);
    }

    private Object newCacheInstance() throws Exception {
        try {
            Class<?> cacheClass = Class.forName("com.github.ae2patterngen.recipe.RecipeCollectionCache");
            Constructor<?> constructor = cacheClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ClassNotFoundException e) {
            fail("Expected RecipeCollectionCache helper for recipe pool reuse");
        }
        return null;
    }

    private Method getOrComputeMethod(Object cache) throws Exception {
        return getMethod(cache.getClass(), "getOrCompute", Object.class, Supplier.class);
    }

    private Method getMethod(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            fail("Expected method missing: " + name);
        }
        return null;
    }
}
