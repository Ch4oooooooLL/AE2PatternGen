package com.github.ae2patterngen.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.ae2patterngen.config.ForgeConfig;
import com.github.ae2patterngen.filter.BlacklistFilter;
import com.github.ae2patterngen.filter.NCItemFilter;
import com.github.ae2patterngen.filter.OutputOreDictFilter;
import com.github.ae2patterngen.filter.TierFilter;
import com.github.ae2patterngen.recipe.RecipeEntry;

public class RecipeCacheStorageTest {

    private Path tempRoot;

    @Before
    public void setUp() throws Exception {
        initializeMinecraftBootstrap();
        tempRoot = Files.createTempDirectory("ae2patterngen-recipe-cache");
        RecipeCacheStorage.setDirectoryResolver(new RecipeCacheStorage.DirectoryResolver() {

            @Override
            public File getWorldSaveRoot() {
                return tempRoot.toFile();
            }
        });
    }

    @After
    public void tearDown() {
        RecipeCacheStorage.resetDirectoryResolver();
    }

    @Test
    public void metadataRoundTripsThroughCompressedNbt() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateModInfo("gregtech", "5.0.0", 2, 10);
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 5, "hash-a", "gt.recipe.assembler.dat");
        metadata.setConfigHash("config/ae2patterngen.cfg", "cfg-hash");

        assertTrue(RecipeCacheStorage.saveMetadata(metadata));

        RecipeCacheMetadata loaded = RecipeCacheStorage.loadMetadata();
        assertNotNull(loaded);
        assertEquals(metadata.cacheVersion, loaded.cacheVersion);
        assertEquals(metadata.totalRecipeCount, loaded.totalRecipeCount);
        assertEquals("5.0.0", loaded.mods.get("gregtech").version);
        assertEquals("cfg-hash", loaded.configHashes.get("config/ae2patterngen.cfg"));
    }

    @Test
    public void recipeMapPayloadRoundTripsAndSanitizesUnsafeMapIds() {
        String mapId = "gt.recipe:assembler/unsafe";
        RecipeEntry original = sampleRecipe(mapId);
        List<RecipeEntry> recipes = Collections.singletonList(original);
        RecipeCacheMetadata.RecipeMapInfo info = new RecipeCacheMetadata.RecipeMapInfo(mapId, "gregtech");
        info.cachedAt = 123L;
        info.contentHash = "hash-a";

        assertTrue(RecipeCacheStorage.saveRecipeMap(mapId, recipes, info));

        List<RecipeEntry> loaded = RecipeCacheStorage.loadRecipeMap(mapId);
        assertEquals(1, loaded.size());
        assertEquals(mapId, loaded.get(0).recipeMapId);
        assertEquals("Assembler", loaded.get(0).machineDisplayName);
        assertEquals(120, loaded.get(0).duration);
        assertEquals(30, loaded.get(0).euPerTick);
        assertTrue(
            RecipeCacheStorage.getRecipeMapFile(mapId)
                .getName()
                .contains("_"));
    }

    @Test
    public void deleteRecipeMapRemovesPersistedFile() {
        String mapId = "gt.recipe.assembler";
        assertTrue(RecipeCacheStorage.saveRecipeMap(mapId, Collections.singletonList(sampleRecipe(mapId))));
        assertTrue(
            RecipeCacheStorage.getRecipeMapFile(mapId)
                .exists());

        assertTrue(RecipeCacheStorage.deleteRecipeMap(mapId));
        assertTrue(
            !RecipeCacheStorage.getRecipeMapFile(mapId)
                .exists());
    }

    @Test
    public void cacheDirectoryUsesConfiguredDefaultSubdirectory() {
        File directory = RecipeCacheStorage.getCacheDirectory();
        assertTrue(
            directory.getAbsolutePath()
                .contains("ae2patterngen"));
        assertTrue(
            directory.getAbsolutePath()
                .contains(ForgeConfig.getRecipeCacheDirectoryName()));
    }

    @Test
    public void capturedWorldSaveRootKeepsCachePathStableWhenResolverTurnsNull() {
        final File[] currentRoot = new File[] { tempRoot.toFile() };
        RecipeCacheStorage.setDirectoryResolver(new RecipeCacheStorage.DirectoryResolver() {

            @Override
            public File getWorldSaveRoot() {
                return currentRoot[0];
            }
        });

        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateModInfo("gregtech", "5.0.0", 1, 5);
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 5, "hash-a", "gt.recipe.assembler.dat");
        RecipeCacheStorage.captureCurrentWorldSaveRoot();
        currentRoot[0] = null;

        assertTrue(RecipeCacheStorage.saveMetadata(metadata));

        RecipeCacheMetadata loaded = RecipeCacheStorage.loadMetadata();
        assertNotNull(loaded);
        assertEquals("5.0.0", loaded.mods.get("gregtech").version);
        assertTrue(
            RecipeCacheStorage.getMetadataFile()
                .getAbsolutePath()
                .contains(
                    tempRoot.toFile()
                        .getAbsolutePath()));
    }

    @Test
    public void writeItemStacksPreservesZeroCountForNcInputs() {
        ItemStack normalInput = new ItemStack(createTestItem("Normal Input"), 2, 0);
        ItemStack zeroUseInput = new ItemStack(createTestItem("Zero Use Input"), 1, 0);
        zeroUseInput.stackSize = 0;
        NBTTagList written = invokeWriteItemStacks(new ItemStack[] { normalInput, zeroUseInput });

        assertEquals(2, written.tagCount());
        NBTTagCompound zeroUseTag = written.getCompoundTagAt(1);
        assertEquals(0, zeroUseTag.getInteger("StackSize"));
    }

    @Test
    public void restoredZeroCountInputsRemainUsableForCurrentFilters() {
        Item normalInputItem = createTestItem("Normal Input");
        Item zeroUseInputItem = createTestItem("Zero Use Input");
        Item outputItem = createTestItem("Cache Output");
        Item specialItem = createTestItem("NC Catalyst");

        ItemStack normalInput = RecipeCacheStorage.createRestoredItemStack(normalInputItem, 2, 0);
        ItemStack zeroUseInput = RecipeCacheStorage.createRestoredItemStack(zeroUseInputItem, 0, 0);
        ItemStack output = RecipeCacheStorage.createRestoredItemStack(outputItem, 1, 0);
        ItemStack special = RecipeCacheStorage.createRestoredItemStack(specialItem, 1, 0);
        int zeroUseInputItemId = Item.getIdFromItem(zeroUseInputItem);

        RecipeEntry restored = new RecipeEntry(
            "gt",
            "gt.recipe.cache.filter.behavior",
            "Assembler",
            new ItemStack[] { normalInput, zeroUseInput },
            new ItemStack[] { output },
            new net.minecraftforge.fluids.FluidStack[0],
            new net.minecraftforge.fluids.FluidStack[0],
            new ItemStack[] { special },
            120,
            30);

        assertEquals(2, restored.inputs.length);
        assertEquals(0, restored.inputs[1].stackSize);
        assertEquals(zeroUseInputItem, restored.inputs[1].getItem());
        assertEquals(outputItem, restored.outputs[0].getItem());
        assertEquals(specialItem, restored.specialItems[0].getItem());
        assertTrue(new OutputOreDictFilter("{Cache Output}").matches(restored));
        assertTrue(new NCItemFilter("{NC Catalyst}").matches(restored));
        assertTrue(new NCItemFilter("[" + zeroUseInputItemId + ":0]").matches(restored));
        assertFalse(new BlacklistFilter("{Cache Output}", false, true).matches(restored));
        assertTrue(new TierFilter(1).matches(restored));
    }

    private RecipeEntry sampleRecipe(String mapId) {
        return new RecipeEntry(
            "gt",
            mapId,
            "Assembler",
            new ItemStack[] { new ItemStack(createTestItem("Sample Input"), 2) },
            new ItemStack[] { new ItemStack(createTestItem("Sample Output"), 1) },
            new net.minecraftforge.fluids.FluidStack[0],
            new net.minecraftforge.fluids.FluidStack[0],
            new ItemStack[] { new ItemStack(createTestItem("Sample Special"), 1) },
            120,
            30);
    }

    private static Item createTestItem(String displayName) {
        return new NamedTestItem(displayName);
    }

    @Test
    public void createRestoredItemStackPreservesZeroSizeForCustomItems() {
        Item restoredItem = createTestItem("Restored Zero Item");
        ItemStack restoredStack = RecipeCacheStorage.createRestoredItemStack(restoredItem, 0, 0);

        assertNotNull(restoredStack);
        assertEquals(restoredItem, restoredStack.getItem());
        assertEquals(0, restoredStack.stackSize);
        assertEquals(0, restoredStack.getItemDamage());
    }

    private static final class NamedTestItem extends Item {

        private final String displayName;

        private NamedTestItem(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return displayName;
        }
    }

    private static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    private static NBTTagList invokeWriteItemStacks(ItemStack[] stacks) {
        try {
            Method method = RecipeCacheStorage.class.getDeclaredMethod("writeItemStacks", ItemStack[].class);
            method.setAccessible(true);
            return (NBTTagList) method.invoke(null, new Object[] { stacks });
        } catch (NoSuchMethodException e) {
            fail("Expected private writeItemStacks helper to exist");
        } catch (IllegalAccessException e) {
            fail("Unable to access writeItemStacks helper: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("writeItemStacks helper threw unexpectedly: " + cause.getMessage());
        }
        return new NBTTagList();
    }
}
