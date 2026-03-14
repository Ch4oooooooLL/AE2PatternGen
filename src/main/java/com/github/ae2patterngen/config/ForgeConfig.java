package com.github.ae2patterngen.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.FMLLog;

/**
 * 统一 Forge 配置入口。
 */
public final class ForgeConfig {

    // ========== 配置分类 ==========
    private static final String CATEGORY_CONFLICT = "conflict";
    private static final String CATEGORY_DUPLICATE = "duplicate";
    private static final String CATEGORY_UI_PATTERN_GEN = "ui.patternGen";
    private static final String CATEGORY_UI_RECIPE_PICKER = "ui.recipePicker";
    private static final String CATEGORY_STORAGE = "storage";
    private static final String CATEGORY_ITEMS = "items";

    // ========== 冲突处理配置 ==========
    private static final int MIN_CONFLICT_BATCH_SIZE = 1;
    private static final int MAX_CONFLICT_BATCH_SIZE = 64;
    public static final int DEFAULT_CONFLICT_BATCH_SIZE = 6;

    private static final int DEFAULT_MAX_FILTERED_RECIPES = 4096;
    private static final int DEFAULT_MAX_CONFLICT_GROUPS = 256;

    private static volatile int conflictBatchSize = DEFAULT_CONFLICT_BATCH_SIZE;
    private static volatile int maxFilteredRecipes = DEFAULT_MAX_FILTERED_RECIPES;
    private static volatile int maxConflictGroups = DEFAULT_MAX_CONFLICT_GROUPS;

    // ========== 重复请求去重配置 ==========
    private static final int DEFAULT_DUPLICATE_WINDOW_MS = 500;

    private static volatile long duplicateWindowMs = DEFAULT_DUPLICATE_WINDOW_MS;

    // ========== UI 尺寸配置 - 主界面 ==========
    private static final int DEFAULT_PATTERN_GEN_GUI_WIDTH = 260;
    private static final int DEFAULT_PATTERN_GEN_GUI_HEIGHT = 315;

    private static volatile int patternGenGuiWidth = DEFAULT_PATTERN_GEN_GUI_WIDTH;
    private static volatile int patternGenGuiHeight = DEFAULT_PATTERN_GEN_GUI_HEIGHT;

    // ========== UI 尺寸配置 - 配方选择器 ==========
    private static final int DEFAULT_RECIPE_PICKER_GUI_WIDTH = 404;
    private static final int DEFAULT_RECIPE_PICKER_MIN_HEIGHT = 250;
    private static final int DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT = 296;
    private static final int DEFAULT_RECIPE_PICKER_ROW_HEIGHT = 30;
    private static final int DEFAULT_RECIPE_PICKER_ROW_GAP = 1;
    private static final int DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES = 90;

    private static volatile int recipePickerGuiWidth = DEFAULT_RECIPE_PICKER_GUI_WIDTH;
    private static volatile int recipePickerMinHeight = DEFAULT_RECIPE_PICKER_MIN_HEIGHT;
    private static volatile int recipePickerIdealHeight = DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT;
    private static volatile int recipePickerRowHeight = DEFAULT_RECIPE_PICKER_ROW_HEIGHT;
    private static volatile int recipePickerRowGap = DEFAULT_RECIPE_PICKER_ROW_GAP;
    private static volatile int recipePickerMaxDetailLines = DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES;

    // ========== 存储配置 ==========
    private static final String DEFAULT_STORAGE_DIRECTORY_NAME = "ae2patterngen";
    private static final String DEFAULT_RECIPE_CACHE_DIRECTORY_NAME = "recipe_cache";

    private static volatile String storageDirectoryName = DEFAULT_STORAGE_DIRECTORY_NAME;
    private static volatile String recipeCacheDirectoryName = DEFAULT_RECIPE_CACHE_DIRECTORY_NAME;

    // ========== 物品兼容性配置 ==========
    private static final String DEFAULT_ENCODED_PATTERN_ID = "appliedenergistics2:item.ItemEncodedPattern";

    private static volatile String encodedPatternId = DEFAULT_ENCODED_PATTERN_ID;

    private ForgeConfig() {}

    public static void load(File suggestedConfigFile) {
        File file = suggestedConfigFile != null ? suggestedConfigFile : new File("config", "ae2patterngen.cfg");
        Configuration cfg = new Configuration(file);
        try {
            cfg.load();

            loadConflictConfig(cfg);
            loadDuplicateConfig(cfg);
            loadUIConfig(cfg);
            loadStorageConfig(cfg);
            loadItemsConfig(cfg);

        } catch (RuntimeException e) {
            FMLLog.warning("[AE2PatternGen] Failed to load Forge config: %s", e.getMessage());
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    private static void loadConflictConfig(Configuration cfg) {
        int configuredBatchSize = cfg.getInt(
            "batchSize",
            CATEGORY_CONFLICT,
            DEFAULT_CONFLICT_BATCH_SIZE,
            MIN_CONFLICT_BATCH_SIZE,
            MAX_CONFLICT_BATCH_SIZE,
            "How many conflict groups are sent to client per batch. Larger values may cause network lag. / 每批发送到客户端的冲突组数量。数值过大可能造成网络卡顿。");
        conflictBatchSize = normalizeConflictBatchSize(configuredBatchSize);

        int configuredMaxFiltered = cfg.getInt(
            "maxFilteredRecipes",
            CATEGORY_CONFLICT,
            DEFAULT_MAX_FILTERED_RECIPES,
            512,
            65536,
            "Maximum number of filtered recipes allowed for interactive conflict selection. Exceeding this aborts interactive selection. / 允许进入交互式冲突选择的过滤后配方上限。超过后会直接取消交互选择。");
        maxFilteredRecipes = configuredMaxFiltered;

        int configuredMaxGroups = cfg.getInt(
            "maxConflictGroups",
            CATEGORY_CONFLICT,
            DEFAULT_MAX_CONFLICT_GROUPS,
            64,
            1024,
            "Maximum number of conflict groups allowed for interactive conflict selection. Exceeding this aborts interactive selection. / 允许进入交互式冲突选择的冲突组上限。超过后会直接取消交互选择。");
        maxConflictGroups = configuredMaxGroups;
    }

    private static void loadDuplicateConfig(Configuration cfg) {
        int configuredWindowMs = cfg.getInt(
            "windowMs",
            CATEGORY_DUPLICATE,
            DEFAULT_DUPLICATE_WINDOW_MS,
            100,
            5000,
            "Time window in milliseconds to collapse duplicate generation requests from the same player. Prevents rapid-fire duplicate submissions. / 合并同一玩家重复生成请求的时间窗口（毫秒）。用于防止短时间内重复提交。");
        duplicateWindowMs = configuredWindowMs;
    }

    private static void loadUIConfig(Configuration cfg) {
        int configuredPatternGenWidth = cfg.getInt(
            "guiWidth",
            CATEGORY_UI_PATTERN_GEN,
            DEFAULT_PATTERN_GEN_GUI_WIDTH,
            200,
            500,
            "Width of the main Pattern Generator GUI. / 主样板生成界面的宽度。");
        patternGenGuiWidth = configuredPatternGenWidth;

        int configuredPatternGenHeight = cfg.getInt(
            "guiHeight",
            CATEGORY_UI_PATTERN_GEN,
            DEFAULT_PATTERN_GEN_GUI_HEIGHT,
            200,
            600,
            "Height of the main Pattern Generator GUI. / 主样板生成界面的高度。");
        patternGenGuiHeight = configuredPatternGenHeight;

        int configuredPickerWidth = cfg.getInt(
            "guiWidth",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_GUI_WIDTH,
            300,
            600,
            "Width of the Recipe Picker GUI. / 配方选择器界面的宽度。");
        recipePickerGuiWidth = configuredPickerWidth;

        int configuredPickerMinHeight = cfg.getInt(
            "minHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_MIN_HEIGHT,
            150,
            400,
            "Minimum height of the Recipe Picker GUI. / 配方选择器界面的最小高度。");
        recipePickerMinHeight = configuredPickerMinHeight;

        int configuredPickerIdealHeight = cfg.getInt(
            "idealHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT,
            200,
            500,
            "Ideal height of the Recipe Picker GUI (used for initial sizing). / 配方选择器界面的理想高度（用于初始尺寸）。");
        recipePickerIdealHeight = configuredPickerIdealHeight;

        int configuredPickerRowHeight = cfg.getInt(
            "rowHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_ROW_HEIGHT,
            20,
            50,
            "Height of each row in the Recipe Picker list. / 配方选择器列表每一行的高度。");
        recipePickerRowHeight = configuredPickerRowHeight;

        int configuredPickerRowGap = cfg.getInt(
            "rowGap",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_ROW_GAP,
            0,
            5,
            "Gap between rows in the Recipe Picker list. / 配方选择器列表行与行之间的间距。");
        recipePickerRowGap = configuredPickerRowGap;

        int configuredPickerMaxDetailLines = cfg.getInt(
            "maxDetailLines",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES,
            20,
            200,
            "Maximum number of lines displayed in the recipe detail panel. / 配方详情面板最多显示的行数。");
        recipePickerMaxDetailLines = configuredPickerMaxDetailLines;
    }

    private static void loadStorageConfig(Configuration cfg) {
        String configuredDirectoryName = cfg.getString(
            "directoryName",
            CATEGORY_STORAGE,
            DEFAULT_STORAGE_DIRECTORY_NAME,
            "Name of the directory where generated patterns are stored (in save folder). / 生成样板存储目录的名称（位于世界存档目录下）。");
        storageDirectoryName = configuredDirectoryName;

        String configuredRecipeCacheDirectoryName = cfg.getString(
            "recipeCacheDirectoryName",
            CATEGORY_STORAGE,
            DEFAULT_RECIPE_CACHE_DIRECTORY_NAME,
            "Name of the subdirectory used for persisted recipe cache files (under the storage directory). / 持久化配方缓存文件使用的子目录名称（位于样板存储目录下）。");
        recipeCacheDirectoryName = configuredRecipeCacheDirectoryName;
    }

    private static void loadItemsConfig(Configuration cfg) {
        String configuredPatternId = cfg.getString(
            "encodedPatternId",
            CATEGORY_ITEMS,
            DEFAULT_ENCODED_PATTERN_ID,
            "Item ID of AE2 Encoded Pattern item. Used for compatibility with different AE2 versions. / AE2 编码样板物品的物品 ID，用于兼容不同 AE2 版本。");
        encodedPatternId = configuredPatternId;
    }

    public static int getConflictBatchSize() {
        return conflictBatchSize;
    }

    public static int getMaxFilteredRecipes() {
        return maxFilteredRecipes;
    }

    public static int getMaxConflictGroups() {
        return maxConflictGroups;
    }

    public static long getDuplicateWindowMs() {
        return duplicateWindowMs;
    }

    public static int getPatternGenGuiWidth() {
        return patternGenGuiWidth;
    }

    public static int getPatternGenGuiHeight() {
        return patternGenGuiHeight;
    }

    public static int getRecipePickerGuiWidth() {
        return recipePickerGuiWidth;
    }

    public static int getRecipePickerMinHeight() {
        return recipePickerMinHeight;
    }

    public static int getRecipePickerIdealHeight() {
        return recipePickerIdealHeight;
    }

    public static int getRecipePickerRowHeight() {
        return recipePickerRowHeight;
    }

    public static int getRecipePickerRowGap() {
        return recipePickerRowGap;
    }

    public static int getRecipePickerMaxDetailLines() {
        return recipePickerMaxDetailLines;
    }

    public static String getStorageDirectoryName() {
        return storageDirectoryName;
    }

    public static String getRecipeCacheDirectoryName() {
        return recipeCacheDirectoryName;
    }

    public static String getEncodedPatternId() {
        return encodedPatternId;
    }

    static int normalizeConflictBatchSize(int value) {
        if (value < MIN_CONFLICT_BATCH_SIZE) {
            return MIN_CONFLICT_BATCH_SIZE;
        }
        if (value > MAX_CONFLICT_BATCH_SIZE) {
            return MAX_CONFLICT_BATCH_SIZE;
        }
        return value;
    }
}
