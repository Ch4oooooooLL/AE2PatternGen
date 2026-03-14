package com.github.ae2patterngen.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.config.ForgeConfig;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * Persistent storage for recipe cache metadata and per-map payloads.
 */
public final class RecipeCacheStorage {

    private static final String METADATA_FILE_NAME = "metadata.dat";

    private static final String KEY_VERSION = "Version";
    private static final String KEY_CREATED_AT = "CreatedAt";
    private static final String KEY_LAST_UPDATED = "LastUpdated";
    private static final String KEY_TOTAL_RECIPE_COUNT = "TotalRecipeCount";
    private static final String KEY_TOTAL_RECIPE_MAPS = "TotalRecipeMaps";
    private static final String KEY_MODS = "Mods";
    private static final String KEY_RECIPE_MAPS = "RecipeMaps";
    private static final String KEY_CONFIG_HASHES = "ConfigHashes";

    private static final String KEY_MOD_ID = "ModId";
    private static final String KEY_MOD_VERSION = "ModVersion";
    private static final String KEY_RECIPE_MAP_COUNT = "RecipeMapCount";
    private static final String KEY_RECIPE_COUNT = "RecipeCount";

    private static final String KEY_MAP_ID = "MapId";
    private static final String KEY_CACHED_AT = "CachedAt";
    private static final String KEY_HASH = "Hash";
    private static final String KEY_CACHE_FILE_NAME = "CacheFileName";
    private static final String KEY_PATH = "Path";
    private static final String KEY_VALUE = "Value";

    private static final String KEY_RECIPES = "Recipes";
    private static final String KEY_SOURCE_TYPE = "SourceType";
    private static final String KEY_MACHINE_DISPLAY_NAME = "MachineDisplayName";
    private static final String KEY_INPUTS = "Inputs";
    private static final String KEY_OUTPUTS = "Outputs";
    private static final String KEY_FLUID_INPUTS = "FluidInputs";
    private static final String KEY_FLUID_OUTPUTS = "FluidOutputs";
    private static final String KEY_SPECIAL_ITEMS = "SpecialItems";
    private static final String KEY_DURATION = "Duration";
    private static final String KEY_EU_PER_TICK = "EuPerTick";
    private static final String KEY_REGISTRY_NAME = "RegistryName";
    private static final String KEY_ITEM_ID = "ItemId";
    private static final String KEY_STACK_SIZE = "StackSize";
    private static final String KEY_DAMAGE = "Damage";
    private static final String KEY_TAG = "tag";

    private static volatile DirectoryResolver directoryResolver = new DirectoryResolver() {

        @Override
        public File getWorldSaveRoot() {
            return DimensionManager.getCurrentSaveRootDirectory();
        }
    };
    private static volatile File rememberedWorldSaveRoot;

    private RecipeCacheStorage() {}

    public static boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes) {
        return saveRecipeMap(mapId, recipes, null);
    }

    public static boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes,
        RecipeCacheMetadata.RecipeMapInfo info) {
        if (isBlank(mapId)) {
            return false;
        }

        File tmpFile = null;
        try {
            File file = getRecipeMapFile(mapId);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }

            NBTTagCompound root = new NBTTagCompound();
            root.setInteger(KEY_VERSION, RecipeCacheMetadata.CURRENT_VERSION);
            root.setString(KEY_MAP_ID, mapId);
            root.setString(KEY_MOD_ID, info != null ? safe(info.modId) : "");
            root.setInteger(KEY_RECIPE_COUNT, recipes != null ? recipes.size() : 0);
            root.setLong(KEY_CACHED_AT, info != null ? info.cachedAt : System.currentTimeMillis());
            root.setString(KEY_HASH, info != null ? safe(info.contentHash) : "");
            root.setString(KEY_CACHE_FILE_NAME, file.getName());
            root.setTag(KEY_RECIPES, toRecipeListNBT(recipes));

            tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }
            moveIntoPlace(tmpFile, file);
            return true;
        } catch (Exception e) {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            System.err.println("[AE2PatternGen] Failed to save recipe cache map: " + e.getMessage());
            return false;
        }
    }

    public static List<RecipeEntry> loadRecipeMap(String mapId) {
        List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();
        if (isBlank(mapId)) {
            return recipes;
        }

        File file = getRecipeMapFile(mapId);
        if (!file.exists()) {
            return recipes;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            int version = root.getInteger(KEY_VERSION);
            if (version != RecipeCacheMetadata.CURRENT_VERSION) {
                return recipes;
            }
            return fromRecipeListNBT(root.getTagList(KEY_RECIPES, 10));
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to load recipe cache map: " + e.getMessage());
            return recipes;
        }
    }

    public static boolean saveMetadata(RecipeCacheMetadata metadata) {
        if (metadata == null) {
            return false;
        }

        File tmpFile = null;
        try {
            File file = getMetadataFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }

            NBTTagCompound root = metadataToNBT(metadata);
            tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }
            moveIntoPlace(tmpFile, file);
            return true;
        } catch (Exception e) {
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            System.err.println("[AE2PatternGen] Failed to save recipe cache metadata: " + e.getMessage());
            return false;
        }
    }

    public static RecipeCacheMetadata loadMetadata() {
        File file = getMetadataFile();
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return metadataFromNBT(CompressedStreamTools.readCompressed(fis));
        } catch (Exception e) {
            System.err.println("[AE2PatternGen] Failed to load recipe cache metadata: " + e.getMessage());
            return null;
        }
    }

    public static boolean deleteRecipeMap(String mapId) {
        if (isBlank(mapId)) {
            return false;
        }
        File file = getRecipeMapFile(mapId);
        return !file.exists() || file.delete();
    }

    public static void clearAll() {
        File directory = getCacheDirectory();
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    static void captureCurrentWorldSaveRoot() {
        File worldDir = directoryResolver.getWorldSaveRoot();
        if (worldDir != null) {
            rememberedWorldSaveRoot = worldDir;
        }
    }

    public static File getCacheDirectory() {
        File worldDir = resolveWorldSaveRoot();
        File storageDir = worldDir != null ? new File(worldDir, ForgeConfig.getStorageDirectoryName())
            : new File(ForgeConfig.getStorageDirectoryName());
        return new File(storageDir, ForgeConfig.getRecipeCacheDirectoryName());
    }

    public static File getMetadataFile() {
        return new File(getCacheDirectory(), METADATA_FILE_NAME);
    }

    public static File getRecipeMapFile(String mapId) {
        return new File(getCacheDirectory(), sanitizeFileComponent(mapId) + ".dat");
    }

    static void setDirectoryResolver(DirectoryResolver resolver) {
        directoryResolver = resolver != null ? resolver : directoryResolver;
        rememberedWorldSaveRoot = null;
    }

    static void resetDirectoryResolver() {
        directoryResolver = new DirectoryResolver() {

            @Override
            public File getWorldSaveRoot() {
                return DimensionManager.getCurrentSaveRootDirectory();
            }
        };
        rememberedWorldSaveRoot = null;
    }

    static String sanitizeFileComponent(String value) {
        if (isBlank(value)) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static File resolveWorldSaveRoot() {
        File worldDir = directoryResolver.getWorldSaveRoot();
        if (worldDir != null) {
            rememberedWorldSaveRoot = worldDir;
            return worldDir;
        }
        return rememberedWorldSaveRoot;
    }

    private static NBTTagCompound metadataToNBT(RecipeCacheMetadata metadata) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger(KEY_VERSION, metadata.cacheVersion);
        root.setLong(KEY_CREATED_AT, metadata.createdAt);
        root.setLong(KEY_LAST_UPDATED, metadata.lastUpdated);
        root.setInteger(KEY_TOTAL_RECIPE_COUNT, metadata.totalRecipeCount);
        root.setInteger(KEY_TOTAL_RECIPE_MAPS, metadata.totalRecipeMaps);

        NBTTagList mods = new NBTTagList();
        for (RecipeCacheMetadata.ModInfo info : metadata.mods.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(KEY_MOD_ID, safe(info.modId));
            tag.setString(KEY_MOD_VERSION, safe(info.version));
            tag.setInteger(KEY_RECIPE_MAP_COUNT, info.recipeMapCount);
            tag.setInteger(KEY_RECIPE_COUNT, info.recipeCount);
            mods.appendTag(tag);
        }
        root.setTag(KEY_MODS, mods);

        NBTTagList recipeMaps = new NBTTagList();
        for (RecipeCacheMetadata.RecipeMapInfo info : metadata.recipeMaps.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(KEY_MAP_ID, safe(info.mapId));
            tag.setString(KEY_MOD_ID, safe(info.modId));
            tag.setInteger(KEY_RECIPE_COUNT, info.recipeCount);
            tag.setLong(KEY_CACHED_AT, info.cachedAt);
            tag.setString(KEY_HASH, safe(info.contentHash));
            tag.setString(KEY_CACHE_FILE_NAME, safe(info.cacheFileName));
            recipeMaps.appendTag(tag);
        }
        root.setTag(KEY_RECIPE_MAPS, recipeMaps);

        NBTTagList configHashes = new NBTTagList();
        for (java.util.Map.Entry<String, String> entry : metadata.configHashes.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(KEY_PATH, safe(entry.getKey()));
            tag.setString(KEY_VALUE, safe(entry.getValue()));
            configHashes.appendTag(tag);
        }
        root.setTag(KEY_CONFIG_HASHES, configHashes);

        return root;
    }

    private static RecipeCacheMetadata metadataFromNBT(NBTTagCompound root) {
        if (root == null) {
            return null;
        }

        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.cacheVersion = root.getInteger(KEY_VERSION);
        metadata.createdAt = root.getLong(KEY_CREATED_AT);
        metadata.lastUpdated = root.getLong(KEY_LAST_UPDATED);
        metadata.totalRecipeCount = root.getInteger(KEY_TOTAL_RECIPE_COUNT);
        metadata.totalRecipeMaps = root.getInteger(KEY_TOTAL_RECIPE_MAPS);

        NBTTagList mods = root.getTagList(KEY_MODS, 10);
        for (int i = 0; i < mods.tagCount(); i++) {
            NBTTagCompound tag = mods.getCompoundTagAt(i);
            RecipeCacheMetadata.ModInfo info = new RecipeCacheMetadata.ModInfo(
                tag.getString(KEY_MOD_ID),
                tag.getString(KEY_MOD_VERSION));
            info.recipeMapCount = tag.getInteger(KEY_RECIPE_MAP_COUNT);
            info.recipeCount = tag.getInteger(KEY_RECIPE_COUNT);
            metadata.putModInfo(info);
        }

        NBTTagList recipeMaps = root.getTagList(KEY_RECIPE_MAPS, 10);
        for (int i = 0; i < recipeMaps.tagCount(); i++) {
            NBTTagCompound tag = recipeMaps.getCompoundTagAt(i);
            RecipeCacheMetadata.RecipeMapInfo info = new RecipeCacheMetadata.RecipeMapInfo(
                tag.getString(KEY_MAP_ID),
                tag.getString(KEY_MOD_ID));
            info.recipeCount = tag.getInteger(KEY_RECIPE_COUNT);
            info.cachedAt = tag.getLong(KEY_CACHED_AT);
            info.contentHash = tag.getString(KEY_HASH);
            info.cacheFileName = tag.getString(KEY_CACHE_FILE_NAME);
            metadata.putRecipeMapInfo(info);
        }

        NBTTagList configHashes = root.getTagList(KEY_CONFIG_HASHES, 10);
        for (int i = 0; i < configHashes.tagCount(); i++) {
            NBTTagCompound tag = configHashes.getCompoundTagAt(i);
            metadata.configHashes.put(tag.getString(KEY_PATH), tag.getString(KEY_VALUE));
        }

        metadata.totalRecipeCount = root.getInteger(KEY_TOTAL_RECIPE_COUNT);
        metadata.totalRecipeMaps = root.getInteger(KEY_TOTAL_RECIPE_MAPS);
        return metadata;
    }

    private static NBTTagList toRecipeListNBT(List<RecipeEntry> recipes) {
        NBTTagList list = new NBTTagList();
        if (recipes == null) {
            return list;
        }
        for (RecipeEntry recipe : recipes) {
            if (recipe != null) {
                list.appendTag(toRecipeNBT(recipe));
            }
        }
        return list;
    }

    private static List<RecipeEntry> fromRecipeListNBT(NBTTagList list) {
        List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();
        for (int i = 0; i < list.tagCount(); i++) {
            recipes.add(fromRecipeNBT(list.getCompoundTagAt(i)));
        }
        return recipes;
    }

    private static NBTTagCompound toRecipeNBT(RecipeEntry recipe) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_SOURCE_TYPE, safe(recipe.sourceType));
        tag.setString(KEY_MAP_ID, safe(recipe.recipeMapId));
        tag.setString(KEY_MACHINE_DISPLAY_NAME, safe(recipe.machineDisplayName));
        tag.setTag(KEY_INPUTS, writeItemStacks(recipe.inputs));
        tag.setTag(KEY_OUTPUTS, writeItemStacks(recipe.outputs));
        tag.setTag(KEY_FLUID_INPUTS, writeFluidStacks(recipe.fluidInputs));
        tag.setTag(KEY_FLUID_OUTPUTS, writeFluidStacks(recipe.fluidOutputs));
        tag.setTag(KEY_SPECIAL_ITEMS, writeItemStacks(recipe.specialItems));
        tag.setInteger(KEY_DURATION, recipe.duration);
        tag.setInteger(KEY_EU_PER_TICK, recipe.euPerTick);
        return tag;
    }

    private static RecipeEntry fromRecipeNBT(NBTTagCompound tag) {
        return new RecipeEntry(
            tag.getString(KEY_SOURCE_TYPE),
            tag.getString(KEY_MAP_ID),
            tag.getString(KEY_MACHINE_DISPLAY_NAME),
            readItemStacks(tag.getTagList(KEY_INPUTS, 10)),
            readItemStacks(tag.getTagList(KEY_OUTPUTS, 10)),
            readFluidStacks(tag.getTagList(KEY_FLUID_INPUTS, 10)),
            readFluidStacks(tag.getTagList(KEY_FLUID_OUTPUTS, 10)),
            readItemStacks(tag.getTagList(KEY_SPECIAL_ITEMS, 10)),
            tag.getInteger(KEY_DURATION),
            tag.getInteger(KEY_EU_PER_TICK));
    }

    private static NBTTagList writeItemStacks(ItemStack[] stacks) {
        NBTTagList list = new NBTTagList();
        if (stacks == null) {
            return list;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null) {
                continue;
            }

            NBTTagCompound tag = new NBTTagCompound();
            Item item = stack.getItem();
            Object registryName = Item.itemRegistry.getNameForObject(item);
            if (registryName != null) {
                tag.setString(KEY_REGISTRY_NAME, registryName.toString());
            }
            tag.setInteger(KEY_ITEM_ID, Item.getIdFromItem(item));
            tag.setInteger(KEY_STACK_SIZE, stack.stackSize);
            tag.setShort(KEY_DAMAGE, (short) stack.getItemDamage());
            if (stack.hasTagCompound()) {
                tag.setTag(
                    KEY_TAG,
                    stack.getTagCompound()
                        .copy());
            }
            list.appendTag(tag);
        }
        return list;
    }

    private static ItemStack[] readItemStacks(NBTTagList list) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for (int i = 0; i < list.tagCount(); i++) {
            ItemStack stack = readItemStack(list.getCompoundTagAt(i));
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new ItemStack[stacks.size()]);
    }

    private static NBTTagList writeFluidStacks(FluidStack[] stacks) {
        NBTTagList list = new NBTTagList();
        if (stacks == null) {
            return list;
        }
        for (FluidStack stack : stacks) {
            if (stack != null) {
                NBTTagCompound tag = new NBTTagCompound();
                stack.writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        return list;
    }

    private static FluidStack[] readFluidStacks(NBTTagList list) {
        List<FluidStack> stacks = new ArrayList<FluidStack>();
        for (int i = 0; i < list.tagCount(); i++) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(list.getCompoundTagAt(i));
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new FluidStack[stacks.size()]);
    }

    private static void moveIntoPlace(File source, File target) throws Exception {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim()
            .isEmpty();
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static ItemStack readItemStack(NBTTagCompound tag) {
        Item item = null;
        String registryName = tag.getString(KEY_REGISTRY_NAME);
        if (!isBlank(registryName)) {
            Object resolved = Item.itemRegistry.getObject(registryName);
            if (resolved instanceof Item) {
                item = (Item) resolved;
            }
        }

        if (item == null) {
            int itemId = tag.getInteger(KEY_ITEM_ID);
            if (itemId >= 0) {
                item = Item.getItemById(itemId);
            }
        }

        if (item == null) {
            return null;
        }

        int stackSize = tag.hasKey(KEY_STACK_SIZE) ? tag.getInteger(KEY_STACK_SIZE) : tag.getByte("Count");
        int damage = tag.getShort(KEY_DAMAGE);
        ItemStack fallback = createRestoredItemStack(item, stackSize, damage);
        if (tag.hasKey(KEY_TAG, 10)) {
            fallback.setTagCompound(
                (NBTTagCompound) tag.getCompoundTag(KEY_TAG)
                    .copy());
        }
        return fallback;
    }

    static ItemStack createRestoredItemStack(Item item, int stackSize, int damage) {
        if (item == null) {
            return null;
        }

        int preservedSize = Math.max(0, stackSize);
        ItemStack restored = new ItemStack(item, preservedSize > 0 ? preservedSize : 1, damage);
        restored.stackSize = preservedSize;
        return restored;
    }

    interface DirectoryResolver {

        File getWorldSaveRoot();
    }
}
