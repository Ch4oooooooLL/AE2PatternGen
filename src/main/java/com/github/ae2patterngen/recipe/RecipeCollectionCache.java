package com.github.ae2patterngen.recipe;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Small synchronized cache for recipe collection results.
 */
final class RecipeCollectionCache<K, V> {

    private final Map<K, V> cache = new HashMap<K, V>();

    synchronized V getOrCompute(K key, Supplier<V> supplier) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        V value = supplier.get();
        cache.put(key, value);
        return value;
    }

    synchronized void clear() {
        cache.clear();
    }
}
