package com.github.ae2patterngen.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecipeCacheMetadataTest {

    @Test
    public void freshMetadataStartsWithCurrentVersionAndNoTotals() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();

        assertEquals(RecipeCacheMetadata.CURRENT_VERSION, metadata.cacheVersion);
        assertEquals(0, metadata.totalRecipeCount);
        assertEquals(0, metadata.totalRecipeMaps);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void updateRecipeMapInfoRecalculatesTotals() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();

        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 5, "hash-a", "gt.recipe.assembler.dat");
        metadata.updateRecipeMapInfo("gt.recipe.cutter", "gregtech", 7, "hash-b", "gt.recipe.cutter.dat");

        assertEquals(12, metadata.totalRecipeCount);
        assertEquals(2, metadata.totalRecipeMaps);
        assertFalse(metadata.isEmpty());
        assertEquals("gt.recipe.assembler.dat", metadata.recipeMaps.get("gt.recipe.assembler").cacheFileName);
    }

    @Test
    public void updateModInfoReplacesCountsInsteadOfAccumulating() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();

        metadata.updateModInfo("gregtech", "1.0.0", 2, 10);
        metadata.updateModInfo("gregtech", "1.0.1", 3, 11);

        RecipeCacheMetadata.ModInfo info = metadata.mods.get("gregtech");
        assertEquals("1.0.1", info.version);
        assertEquals(3, info.recipeMapCount);
        assertEquals(11, info.recipeCount);
    }

    @Test
    public void removeRecipeMapInfoUpdatesTotals() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 5, "hash-a");
        metadata.updateRecipeMapInfo("gt.recipe.cutter", "gregtech", 7, "hash-b");

        metadata.removeRecipeMapInfo("gt.recipe.assembler");

        assertEquals(7, metadata.totalRecipeCount);
        assertEquals(1, metadata.totalRecipeMaps);
        assertFalse(metadata.recipeMaps.containsKey("gt.recipe.assembler"));
    }

    @Test
    public void touchAdvancesLastUpdated() throws Exception {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        long original = metadata.lastUpdated;

        Thread.sleep(2L);
        metadata.touch();

        assertTrue(metadata.lastUpdated > original);
    }
}
