package com.github.ae2patterngen.storage;

import java.util.Collections;
import java.util.List;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * Query result returned by the recipe cache service.
 */
public class CacheQueryResult {

    public static final String SOURCE_DISK = "DISK";

    public final boolean cacheValid;
    public final String failureReason;
    public final List<String> matchedMapIds;
    public final List<RecipeEntry> recipes;
    public final int totalLoadedCount;
    public final int totalFilteredCount;
    public final String cacheSource;
    public final List<String> warnings;

    private CacheQueryResult(boolean cacheValid, String failureReason, List<String> matchedMapIds,
        List<RecipeEntry> recipes, int totalLoadedCount, int totalFilteredCount, String cacheSource,
        List<String> warnings) {
        this.cacheValid = cacheValid;
        this.failureReason = failureReason;
        this.matchedMapIds = matchedMapIds;
        this.recipes = recipes;
        this.totalLoadedCount = totalLoadedCount;
        this.totalFilteredCount = totalFilteredCount;
        this.cacheSource = cacheSource;
        this.warnings = warnings;
    }

    public static CacheQueryResult invalid(String failureReason) {
        return new CacheQueryResult(
            false,
            failureReason,
            Collections.<String>emptyList(),
            Collections.<RecipeEntry>emptyList(),
            0,
            0,
            SOURCE_DISK,
            Collections.<String>emptyList());
    }

    public static CacheQueryResult valid(List<String> matchedMapIds, List<RecipeEntry> recipes, int totalLoadedCount,
        int totalFilteredCount) {
        return new CacheQueryResult(
            true,
            "",
            matchedMapIds,
            recipes,
            totalLoadedCount,
            totalFilteredCount,
            SOURCE_DISK,
            Collections.<String>emptyList());
    }
}
