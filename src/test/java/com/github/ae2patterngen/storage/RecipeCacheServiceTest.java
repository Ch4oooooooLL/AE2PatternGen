package com.github.ae2patterngen.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.IRecipeFilter;
import com.github.ae2patterngen.recipe.RecipeEntry;

public class RecipeCacheServiceTest {

    private FakeStorageBackend storage;
    private FakeRecipeCollector collector;
    private FakeEnvironmentInspector inspector;

    @Before
    public void setUp() throws Exception {
        storage = new FakeStorageBackend(
            Files.createTempDirectory("ae2patterngen-cache-service")
                .toFile());
        collector = new FakeRecipeCollector();
        inspector = new FakeEnvironmentInspector();
        RecipeCacheService.setStorageBackend(storage);
        RecipeCacheService.setRecipeCollector(collector);
        RecipeCacheService.setEnvironmentInspector(inspector);
    }

    @After
    public void tearDown() {
        RecipeCacheService.resetTestHooks();
    }

    @Test
    public void validateCacheFailsWhenMetadataMissing() {
        assertFalse(RecipeCacheService.validateCache());
    }

    @Test
    public void loadAndFilterRecipesRejectsInvalidCache() {
        CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes("assembler", new CompositeFilter());

        assertFalse(result.cacheValid);
        assertEquals("cache_missing_or_invalid", result.failureReason);
    }

    @Test
    public void rebuildNowReusesExistingEntriesWhenEnvironmentIsUnchanged() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        RecipeCacheMetadata.RecipeMapInfo info = new RecipeCacheMetadata.RecipeMapInfo(
            "gt.recipe.assembler",
            "gregtech");
        info.recipeCount = 2;
        info.contentHash = "hash-a";
        info.cacheFileName = "gt.recipe.assembler.dat";
        metadata.putRecipeMapInfo(info);
        metadata.updateModInfo("gregtech", "5.0.0", 1, 2);
        metadata.setConfigHash("config/ae2patterngen.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Arrays.asList(sampleRecipe(20), sampleRecipe(40)));
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/ae2patterngen.cfg", "cfg-hash");

        CacheStatistics stats = RecipeCacheService.rebuildNow(null);

        assertEquals(2, stats.totalRecipeCount);
        assertEquals(0, collector.collectCalls);
        assertEquals(0, storage.saveRecipeMapCalls);
    }

    @Test
    public void validateCacheSucceedsAfterRebuildWithFullModSnapshot() {
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.modVersions.put("minecraft", "1.7.10");
        inspector.configHashes.put("config/ae2patterngen.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertTrue(RecipeCacheService.validateCache());
        assertEquals(2, storage.metadata.mods.size());
        assertTrue(storage.metadata.mods.containsKey("gregtech"));
        assertTrue(storage.metadata.mods.containsKey("minecraft"));
    }

    @Test
    public void validateCacheIgnoresRecipePrefixesThatAreNotRealModIds() {
        collector.availableMapIds = Collections.singletonList("ic.recipe.recycler");
        inspector.modVersions.put("IC2", "2.2.828");
        inspector.modVersions.put("minecraft", "1.7.10");
        inspector.configHashes.put("config/ae2patterngen.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertTrue(RecipeCacheService.validateCache());
        assertEquals(2, storage.metadata.mods.size());
        assertTrue(storage.metadata.mods.containsKey("IC2"));
        assertTrue(storage.metadata.mods.containsKey("minecraft"));
    }

    @Test
    public void loadAndFilterRecipesUsesStoredPayloadAndFilter() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 2, "hash-a", "gt.recipe.assembler.dat");
        metadata.updateModInfo("gregtech", "5.0.0", 1, 2);
        metadata.setConfigHash("config/ae2patterngen.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Arrays.asList(sampleRecipe(20), sampleRecipe(120)));
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        collector.matches.put("assembler", Collections.singletonList("gt.recipe.assembler"));
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/ae2patterngen.cfg", "cfg-hash");

        CompositeFilter filter = new CompositeFilter();
        filter.addFilter(new IRecipeFilter() {

            @Override
            public boolean matches(RecipeEntry recipe) {
                return recipe.duration >= 100;
            }

            @Override
            public String getDescription() {
                return "duration >= 100";
            }
        });

        CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes("assembler", filter);

        assertTrue(result.cacheValid);
        assertEquals(2, result.totalLoadedCount);
        assertEquals(1, result.totalFilteredCount);
        assertEquals(120, result.recipes.get(0).duration);
    }

    private static RecipeEntry sampleRecipe(int duration) {
        return new RecipeEntry("gt", "gt.recipe.assembler", "Assembler", null, null, null, null, null, duration, 30);
    }

    private static final class FakeStorageBackend implements RecipeCacheService.StorageBackend {

        private final File cacheDirectory;
        private RecipeCacheMetadata metadata;
        private final Map<String, List<RecipeEntry>> persistedRecipeMaps = new LinkedHashMap<String, List<RecipeEntry>>();
        private int saveRecipeMapCalls;

        private FakeStorageBackend(File cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
        }

        @Override
        public boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info) {
            saveRecipeMapCalls++;
            persistedRecipeMaps.put(mapId, new ArrayList<RecipeEntry>(recipes));
            return true;
        }

        @Override
        public List<RecipeEntry> loadRecipeMap(String mapId) {
            List<RecipeEntry> recipes = persistedRecipeMaps.get(mapId);
            return recipes != null ? new ArrayList<RecipeEntry>(recipes) : new ArrayList<RecipeEntry>();
        }

        @Override
        public boolean saveMetadata(RecipeCacheMetadata metadata) {
            this.metadata = metadata;
            return true;
        }

        @Override
        public RecipeCacheMetadata loadMetadata() {
            return metadata;
        }

        @Override
        public boolean deleteRecipeMap(String mapId) {
            persistedRecipeMaps.remove(mapId);
            return true;
        }

        @Override
        public void clearAll() {
            persistedRecipeMaps.clear();
            metadata = null;
        }

        @Override
        public File getCacheDirectory() {
            return cacheDirectory;
        }

        @Override
        public boolean recipeMapExists(String mapId) {
            return persistedRecipeMaps.containsKey(mapId);
        }
    }

    private static final class FakeRecipeCollector implements RecipeCacheService.RecipeCollector {

        private List<String> availableMapIds = new ArrayList<String>();
        private Map<String, List<String>> matches = new LinkedHashMap<String, List<String>>();
        private int collectCalls;

        @Override
        public List<String> getAvailableRecipeMapIds() {
            return new ArrayList<String>(availableMapIds);
        }

        @Override
        public List<String> findMatchingRecipeMaps(String keyword) {
            List<String> found = matches.get(keyword);
            return found != null ? new ArrayList<String>(found) : new ArrayList<String>();
        }

        @Override
        public List<RecipeEntry> collectRecipes(String mapId) {
            collectCalls++;
            return Arrays.asList(sampleRecipe(20), sampleRecipe(40));
        }
    }

    private static final class FakeEnvironmentInspector implements RecipeCacheService.EnvironmentInspector {

        private final Map<String, String> modVersions = new LinkedHashMap<String, String>();
        private final Map<String, String> configHashes = new LinkedHashMap<String, String>();

        @Override
        public Map<String, String> getLoadedModVersions() {
            return new LinkedHashMap<String, String>(modVersions);
        }

        @Override
        public Map<String, String> getConfigHashes() {
            return new LinkedHashMap<String, String>(configHashes);
        }

        @Override
        public String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes) {
            return mapId + ":" + recipes.size();
        }

        @Override
        public String resolveModId(String mapId) {
            return "gregtech";
        }
    }
}
