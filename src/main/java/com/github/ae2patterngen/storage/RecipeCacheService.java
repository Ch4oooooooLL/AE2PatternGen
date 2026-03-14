package com.github.ae2patterngen.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * Coordinates recipe cache validation, rebuilds, and query access.
 */
public final class RecipeCacheService {

    private static final ExecutorService CACHE_EXECUTOR = Executors
        .newSingleThreadExecutor(runnable -> new Thread(runnable, "AE2PatternGen-RecipeCache"));

    private static volatile boolean caching = false;
    private static volatile StorageBackend storageBackend = new DefaultStorageBackend();
    private static volatile RecipeCollector recipeCollector = new DefaultRecipeCollector();
    private static volatile EnvironmentInspector environmentInspector = new DefaultEnvironmentInspector();

    private RecipeCacheService() {}

    public static boolean createOrRefreshCache(ProgressNotifier notifier) {
        storageBackend.prepareAccessContext();
        synchronized (RecipeCacheService.class) {
            if (caching) {
                return false;
            }
            caching = true;
        }

        CACHE_EXECUTOR.execute(() -> {
            try {
                CacheStatistics stats = rebuildNow(notifier);
                if (notifier != null) {
                    notifier.onComplete(stats);
                }
            } catch (RuntimeException e) {
                if (notifier != null) {
                    notifier.onError(e.getMessage() != null ? e.getMessage() : "recipe_cache_build_failed");
                }
            } finally {
                synchronized (RecipeCacheService.class) {
                    caching = false;
                }
            }
        });
        return true;
    }

    public static boolean isCaching() {
        return caching;
    }

    public static boolean validateCache() {
        storageBackend.prepareAccessContext();
        RecipeCacheMetadata metadata = storageBackend.loadMetadata();
        if (metadata == null || metadata.cacheVersion != RecipeCacheMetadata.CURRENT_VERSION
            || metadata.recipeMaps.isEmpty()) {
            return false;
        }

        if (ModVersionHelper.isModVersionChanged(metadata, environmentInspector.getLoadedModVersions())) {
            return false;
        }
        if (ModVersionHelper.isConfigHashChanged(metadata, environmentInspector.getConfigHashes())) {
            return false;
        }

        for (String mapId : metadata.recipeMaps.keySet()) {
            if (!storageBackend.recipeMapExists(mapId)) {
                return false;
            }
        }
        return true;
    }

    public static CacheQueryResult loadRecipes(String recipeMapKeyword) {
        return loadAndFilterRecipes(recipeMapKeyword, null);
    }

    public static CacheQueryResult loadAndFilterRecipes(String recipeMapKeyword, CompositeFilter filter) {
        if (!validateCache()) {
            return CacheQueryResult.invalid("cache_missing_or_invalid");
        }

        List<String> matchedMapIds = recipeCollector.findMatchingRecipeMaps(recipeMapKeyword);
        if (matchedMapIds.isEmpty()) {
            return CacheQueryResult.valid(matchedMapIds, new ArrayList<RecipeEntry>(), 0, 0);
        }

        List<RecipeEntry> filtered = new ArrayList<RecipeEntry>();
        int totalLoaded = 0;
        for (String mapId : matchedMapIds) {
            List<RecipeEntry> recipes = storageBackend.loadRecipeMap(mapId);
            totalLoaded += recipes.size();
            if (filter == null) {
                filtered.addAll(recipes);
                continue;
            }

            for (RecipeEntry recipe : recipes) {
                if (filter.matches(recipe)) {
                    filtered.add(recipe);
                }
            }
        }

        return CacheQueryResult.valid(matchedMapIds, filtered, totalLoaded, filtered.size());
    }

    public static CacheStatistics getStatistics() {
        storageBackend.prepareAccessContext();
        RecipeCacheMetadata metadata = storageBackend.loadMetadata();
        if (metadata == null || metadata.recipeMaps.isEmpty()) {
            return CacheStatistics.EMPTY;
        }
        return buildStatistics(metadata);
    }

    public static void clearCache() {
        storageBackend.prepareAccessContext();
        storageBackend.clearAll();
    }

    static CacheStatistics rebuildNow(ProgressNotifier notifier) {
        RecipeCacheMetadata existing = storageBackend.loadMetadata();
        Map<String, String> currentModVersions = environmentInspector.getLoadedModVersions();
        Map<String, String> currentConfigHashes = environmentInspector.getConfigHashes();
        boolean canReuseExisting = existing != null && existing.cacheVersion == RecipeCacheMetadata.CURRENT_VERSION
            && !ModVersionHelper.isModVersionChanged(existing, currentModVersions)
            && !ModVersionHelper.isConfigHashChanged(existing, currentConfigHashes);

        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.createdAt = existing != null && existing.createdAt > 0 ? existing.createdAt : metadata.createdAt;
        metadata.configHashes.putAll(currentConfigHashes);

        List<String> availableMapIds = recipeCollector.getAvailableRecipeMapIds();
        java.util.Collections.sort(availableMapIds);

        Map<String, int[]> modCounters = new LinkedHashMap<String, int[]>();
        int total = availableMapIds.size();
        for (int index = 0; index < availableMapIds.size(); index++) {
            String mapId = availableMapIds.get(index);
            if (notifier != null) {
                notifier.onProgress("Caching " + mapId, index + 1, total);
            }

            RecipeCacheMetadata.RecipeMapInfo info;
            if (canReuseExisting && existing.recipeMaps.containsKey(mapId) && storageBackend.recipeMapExists(mapId)) {
                RecipeCacheMetadata.RecipeMapInfo oldInfo = existing.recipeMaps.get(mapId);
                info = new RecipeCacheMetadata.RecipeMapInfo(oldInfo.mapId, oldInfo.modId);
                info.recipeCount = oldInfo.recipeCount;
                info.cachedAt = oldInfo.cachedAt;
                info.contentHash = oldInfo.contentHash;
                info.cacheFileName = oldInfo.cacheFileName;
            } else {
                List<RecipeEntry> recipes = recipeCollector.collectRecipes(mapId);
                info = new RecipeCacheMetadata.RecipeMapInfo(mapId, environmentInspector.resolveModId(mapId));
                info.recipeCount = recipes.size();
                info.cachedAt = System.currentTimeMillis();
                info.contentHash = environmentInspector.calculateRecipeMapHash(mapId, recipes);
                info.cacheFileName = RecipeCacheStorage.getRecipeMapFile(mapId)
                    .getName();
                if (!storageBackend.saveRecipeMap(mapId, recipes, info)) {
                    throw new IllegalStateException("failed_to_save_recipe_map:" + mapId);
                }
            }

            metadata.putRecipeMapInfo(info);
            incrementModCounter(modCounters, info.modId, info.recipeCount);
        }

        if (existing != null) {
            for (String staleMapId : existing.recipeMaps.keySet()) {
                if (!metadata.recipeMaps.containsKey(staleMapId)) {
                    storageBackend.deleteRecipeMap(staleMapId);
                }
            }
        }

        for (Map.Entry<String, String> entry : currentModVersions.entrySet()) {
            int[] counts = modCounters.get(entry.getKey());
            metadata.updateModInfo(
                entry.getKey(),
                entry.getValue(),
                counts != null ? counts[0] : 0,
                counts != null ? counts[1] : 0);
        }

        metadata.recalculateTotals();
        metadata.touch();
        if (!storageBackend.saveMetadata(metadata)) {
            throw new IllegalStateException("failed_to_save_recipe_cache_metadata");
        }

        return buildStatistics(metadata);
    }

    static void setStorageBackend(StorageBackend backend) {
        storageBackend = backend != null ? backend : storageBackend;
    }

    static void setRecipeCollector(RecipeCollector collector) {
        recipeCollector = collector != null ? collector : recipeCollector;
    }

    static void setEnvironmentInspector(EnvironmentInspector inspector) {
        environmentInspector = inspector != null ? inspector : environmentInspector;
    }

    static void resetTestHooks() {
        storageBackend = new DefaultStorageBackend();
        recipeCollector = new DefaultRecipeCollector();
        environmentInspector = new DefaultEnvironmentInspector();
    }

    private static CacheStatistics buildStatistics(RecipeCacheMetadata metadata) {
        return new CacheStatistics(
            true,
            metadata.totalRecipeCount,
            metadata.totalRecipeMaps,
            metadata.mods.size(),
            calculateDirectoryBytes(storageBackend.getCacheDirectory()),
            metadata.createdAt,
            metadata.lastUpdated);
    }

    private static long calculateDirectoryBytes(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }

        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        for (File child : children) {
            total += calculateDirectoryBytes(child);
        }
        return total;
    }

    private static void incrementModCounter(Map<String, int[]> counters, String modId, int recipeCount) {
        String key = modId != null && !modId.isEmpty() ? modId : "unknown";
        int[] counts = counters.get(key);
        if (counts == null) {
            counts = new int[] { 0, 0 };
            counters.put(key, counts);
        }
        counts[0]++;
        counts[1] += Math.max(0, recipeCount);
    }

    public interface ProgressNotifier {

        void onProgress(String message, int current, int total);

        void onComplete(CacheStatistics statistics);

        void onError(String message);
    }

    interface StorageBackend {

        default void prepareAccessContext() {}

        boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info);

        List<RecipeEntry> loadRecipeMap(String mapId);

        boolean saveMetadata(RecipeCacheMetadata metadata);

        RecipeCacheMetadata loadMetadata();

        boolean deleteRecipeMap(String mapId);

        void clearAll();

        File getCacheDirectory();

        boolean recipeMapExists(String mapId);
    }

    interface RecipeCollector {

        List<String> getAvailableRecipeMapIds();

        List<String> findMatchingRecipeMaps(String keyword);

        List<RecipeEntry> collectRecipes(String mapId);
    }

    interface EnvironmentInspector {

        Map<String, String> getLoadedModVersions();

        Map<String, String> getConfigHashes();

        String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes);

        String resolveModId(String mapId);
    }

    private static final class DefaultStorageBackend implements StorageBackend {

        @Override
        public void prepareAccessContext() {
            RecipeCacheStorage.captureCurrentWorldSaveRoot();
        }

        @Override
        public boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info) {
            return RecipeCacheStorage.saveRecipeMap(mapId, recipes, info);
        }

        @Override
        public List<RecipeEntry> loadRecipeMap(String mapId) {
            return RecipeCacheStorage.loadRecipeMap(mapId);
        }

        @Override
        public boolean saveMetadata(RecipeCacheMetadata metadata) {
            return RecipeCacheStorage.saveMetadata(metadata);
        }

        @Override
        public RecipeCacheMetadata loadMetadata() {
            return RecipeCacheStorage.loadMetadata();
        }

        @Override
        public boolean deleteRecipeMap(String mapId) {
            return RecipeCacheStorage.deleteRecipeMap(mapId);
        }

        @Override
        public void clearAll() {
            RecipeCacheStorage.clearAll();
        }

        @Override
        public File getCacheDirectory() {
            return RecipeCacheStorage.getCacheDirectory();
        }

        @Override
        public boolean recipeMapExists(String mapId) {
            return RecipeCacheStorage.getRecipeMapFile(mapId)
                .exists();
        }
    }

    private static final class DefaultRecipeCollector implements RecipeCollector {

        @Override
        public List<String> getAvailableRecipeMapIds() {
            return new ArrayList<String>(
                GTRecipeSource.getAvailableRecipeMaps()
                    .keySet());
        }

        @Override
        public List<String> findMatchingRecipeMaps(String keyword) {
            return GTRecipeSource.findMatchingRecipeMaps(keyword);
        }

        @Override
        public List<RecipeEntry> collectRecipes(String mapId) {
            return GTRecipeSource.collectRecipes(mapId);
        }
    }

    private static final class DefaultEnvironmentInspector implements EnvironmentInspector {

        @Override
        public Map<String, String> getLoadedModVersions() {
            return ModVersionHelper.getLoadedModVersions();
        }

        @Override
        public Map<String, String> getConfigHashes() {
            return ModVersionHelper.calculateConfigHashes();
        }

        @Override
        public String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes) {
            return ModVersionHelper.calculateRecipeMapHash(mapId, recipes);
        }

        @Override
        public String resolveModId(String mapId) {
            return ModVersionHelper.resolveModId(mapId);
        }
    }
}
