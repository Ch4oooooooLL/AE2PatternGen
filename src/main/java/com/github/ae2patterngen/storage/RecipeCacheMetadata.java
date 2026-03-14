package com.github.ae2patterngen.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata persisted alongside recipe-cache payloads.
 */
public class RecipeCacheMetadata {

    public static final int CURRENT_VERSION = 1;

    public int cacheVersion = CURRENT_VERSION;
    public long createdAt = System.currentTimeMillis();
    public long lastUpdated = System.currentTimeMillis();
    public int totalRecipeCount = 0;
    public int totalRecipeMaps = 0;

    public final Map<String, ModInfo> mods = new HashMap<String, ModInfo>();
    public final Map<String, RecipeMapInfo> recipeMaps = new HashMap<String, RecipeMapInfo>();
    public final Map<String, String> configHashes = new HashMap<String, String>();

    public void putModInfo(ModInfo info) {
        if (info == null || isBlank(info.modId)) {
            return;
        }
        mods.put(info.modId, info.copy());
        touch();
    }

    public void updateModInfo(String modId, String version, int recipeMapCount, int recipeCount) {
        if (isBlank(modId)) {
            return;
        }

        ModInfo info = mods.get(modId);
        if (info == null) {
            info = new ModInfo(modId, version);
            mods.put(modId, info);
        }

        info.version = version != null ? version : "";
        info.recipeMapCount = Math.max(0, recipeMapCount);
        info.recipeCount = Math.max(0, recipeCount);
        touch();
    }

    public void putRecipeMapInfo(RecipeMapInfo info) {
        if (info == null || isBlank(info.mapId)) {
            return;
        }

        recipeMaps.put(info.mapId, info.copy());
        recalculateTotals();
        touch();
    }

    public void updateRecipeMapInfo(String mapId, String modId, int recipeCount, String hash) {
        updateRecipeMapInfo(mapId, modId, recipeCount, hash, null);
    }

    public void updateRecipeMapInfo(String mapId, String modId, int recipeCount, String hash, String cacheFileName) {
        if (isBlank(mapId)) {
            return;
        }

        RecipeMapInfo info = recipeMaps.get(mapId);
        if (info == null) {
            info = new RecipeMapInfo(mapId, modId);
            recipeMaps.put(mapId, info);
        }

        info.mapId = mapId;
        info.modId = modId != null ? modId : "";
        info.recipeCount = Math.max(0, recipeCount);
        info.cachedAt = System.currentTimeMillis();
        info.contentHash = hash != null ? hash : "";
        if (cacheFileName != null) {
            info.cacheFileName = cacheFileName;
        }

        recalculateTotals();
        touch();
    }

    public void removeRecipeMapInfo(String mapId) {
        if (isBlank(mapId)) {
            return;
        }

        if (recipeMaps.remove(mapId) != null) {
            recalculateTotals();
            touch();
        }
    }

    public void setConfigHash(String path, String hash) {
        if (isBlank(path)) {
            return;
        }
        configHashes.put(path, hash != null ? hash : "");
        touch();
    }

    public void recalculateTotals() {
        totalRecipeMaps = recipeMaps.size();
        totalRecipeCount = 0;
        for (RecipeMapInfo info : recipeMaps.values()) {
            if (info != null) {
                totalRecipeCount += Math.max(0, info.recipeCount);
            }
        }
    }

    public boolean isEmpty() {
        return recipeMaps.isEmpty();
    }

    public void touch() {
        lastUpdated = System.currentTimeMillis();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim()
            .isEmpty();
    }

    /**
     * Cached recipe-map source information.
     */
    public static class ModInfo {

        public String modId = "";
        public String version = "";
        public int recipeMapCount = 0;
        public int recipeCount = 0;

        public ModInfo() {}

        public ModInfo(String modId, String version) {
            this.modId = modId != null ? modId : "";
            this.version = version != null ? version : "";
        }

        private ModInfo copy() {
            ModInfo copy = new ModInfo(modId, version);
            copy.recipeMapCount = recipeMapCount;
            copy.recipeCount = recipeCount;
            return copy;
        }
    }

    /**
     * Per recipe-map cache summary.
     */
    public static class RecipeMapInfo {

        public String mapId = "";
        public String modId = "";
        public int recipeCount = 0;
        public long cachedAt = 0L;
        public String contentHash = "";
        public String cacheFileName = "";

        public RecipeMapInfo() {}

        public RecipeMapInfo(String mapId, String modId) {
            this.mapId = mapId != null ? mapId : "";
            this.modId = modId != null ? modId : "";
        }

        private RecipeMapInfo copy() {
            RecipeMapInfo copy = new RecipeMapInfo(mapId, modId);
            copy.recipeCount = recipeCount;
            copy.cachedAt = cachedAt;
            copy.contentHash = contentHash;
            copy.cacheFileName = cacheFileName;
            return copy;
        }
    }
}
