package com.github.ae2patterngen.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.github.ae2patterngen.recipe.RecipeEntry;

public class ModVersionHelperTest {

    @After
    public void tearDown() {
        ModVersionHelper.resetProviders();
    }

    @Test
    public void configHashIsDeterministicForSameFileContents() throws Exception {
        File temp = File.createTempFile("ae2patterngen-config", ".cfg");
        Files.write(temp.toPath(), Arrays.asList("foo=bar"), StandardCharsets.UTF_8);

        String first = ModVersionHelper.calculateConfigHash(temp);
        String second = ModVersionHelper.calculateConfigHash(temp);

        assertEquals(first, second);
    }

    @Test
    public void missingConfigHashUsesStableSentinel() {
        File missing = new File("build/does-not-exist/ae2patterngen.cfg");
        assertEquals("MISSING", ModVersionHelper.calculateConfigHash(missing));
    }

    @Test
    public void recipeMapHashIsStableForEquivalentPayloads() {
        RecipeEntry first = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            null,
            null,
            null,
            null,
            null,
            20,
            5);
        RecipeEntry second = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            null,
            null,
            null,
            null,
            null,
            40,
            10);

        String hashA = ModVersionHelper.calculateRecipeMapHash("gt.recipe.assembler", Arrays.asList(first, second));
        String hashB = ModVersionHelper.calculateRecipeMapHash("gt.recipe.assembler", Arrays.asList(second, first));

        assertEquals(hashA, hashB);
    }

    @Test
    public void versionAndConfigComparisonDetectsChanges() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateModInfo("gregtech", "1.0.0", 1, 10);
        metadata.setConfigHash("config/ae2patterngen.cfg", "hash-a");

        Map<String, String> sameMods = Collections.singletonMap("gregtech", "1.0.0");
        Map<String, String> changedMods = Collections.singletonMap("gregtech", "1.0.1");
        Map<String, String> sameConfig = Collections.singletonMap("config/ae2patterngen.cfg", "hash-a");
        Map<String, String> changedConfig = Collections.singletonMap("config/ae2patterngen.cfg", "hash-b");

        assertFalse(ModVersionHelper.isModVersionChanged(metadata, sameMods));
        assertTrue(ModVersionHelper.isModVersionChanged(metadata, changedMods));
        assertFalse(ModVersionHelper.isConfigHashChanged(metadata, sameConfig));
        assertTrue(ModVersionHelper.isConfigHashChanged(metadata, changedConfig));
    }

    @Test
    public void loadedModVersionsUsesOverrideProvider() {
        ModVersionHelper.setLoadedModVersionsProvider(() -> {
            Map<String, String> versions = new LinkedHashMap<String, String>();
            versions.put("gregtech", "5.0.0");
            return versions;
        });

        assertEquals(
            "5.0.0",
            ModVersionHelper.getLoadedModVersions()
                .get("gregtech"));
    }
}
