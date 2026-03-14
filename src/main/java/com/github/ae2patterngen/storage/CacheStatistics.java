package com.github.ae2patterngen.storage;

/**
 * Summary information for a built recipe cache.
 */
public class CacheStatistics {

    public static final CacheStatistics EMPTY = new CacheStatistics(false, 0, 0, 0, 0L, 0L, 0L);

    public final boolean available;
    public final int totalRecipeCount;
    public final int totalRecipeMaps;
    public final int totalModCount;
    public final long directoryBytes;
    public final long createdAt;
    public final long lastUpdated;

    public CacheStatistics(boolean available, int totalRecipeCount, int totalRecipeMaps, int totalModCount,
        long directoryBytes, long createdAt, long lastUpdated) {
        this.available = available;
        this.totalRecipeCount = totalRecipeCount;
        this.totalRecipeMaps = totalRecipeMaps;
        this.totalModCount = totalModCount;
        this.directoryBytes = directoryBytes;
        this.createdAt = createdAt;
        this.lastUpdated = lastUpdated;
    }
}
