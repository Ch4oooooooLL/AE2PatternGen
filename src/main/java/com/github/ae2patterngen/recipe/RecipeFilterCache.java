package com.github.ae2patterngen.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cache for filtered recipe results.
 * <p>
 * This cache stores filtered recipe lists based on recipe map key and filter key combination,
 * avoiding repeated expensive filtering operations when the same filtering criteria are used.
 */
public final class RecipeFilterCache {

    private static final Map<String, List<RecipeEntry>> CACHE = new HashMap<>();

    private RecipeFilterCache() {}

    /**
     * Get filtered recipes from cache or compute them using the provided supplier.
     *
     * @param recipeMapKey The recipe map identifier (e.g., "gt.recipe.assembler")
     * @param filterKey    The combined filter key representing all filter conditions
     * @param supplier     The supplier to compute filtered recipes if not cached
     * @return The cached or computed list of filtered recipes
     */
    public static List<RecipeEntry> getOrCompute(String recipeMapKey, String filterKey,
        Supplier<List<RecipeEntry>> supplier) {
        String cacheKey = buildCacheKey(recipeMapKey, filterKey);

        List<RecipeEntry> result = CACHE.get(cacheKey);
        if (result == null) {
            result = supplier.get();
            CACHE.put(cacheKey, result);
        }
        return result;
    }

    /**
     * Clear all cached filter results.
     * <p>
     * Call this when recipe maps are reloaded or when cached results may become stale.
     */
    public static synchronized void clear() {
        CACHE.clear();
    }

    /**
     * Build a unique cache key from recipe map key and filter key.
     */
    private static String buildCacheKey(String recipeMapKey, String filterKey) {
        return recipeMapKey + "||" + filterKey;
    }
}
