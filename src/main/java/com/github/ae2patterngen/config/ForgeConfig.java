package com.github.ae2patterngen.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.FMLLog;

/**
 * 统一 Forge 配置入口。
 */
public final class ForgeConfig {

    private static final String CATEGORY_CONFLICT = "conflict";
    private static final int MIN_CONFLICT_BATCH_SIZE = 1;
    private static final int MAX_CONFLICT_BATCH_SIZE = 64;

    public static final int DEFAULT_CONFLICT_BATCH_SIZE = 6;

    private static volatile int conflictBatchSize = DEFAULT_CONFLICT_BATCH_SIZE;

    private ForgeConfig() {}

    public static void load(File suggestedConfigFile) {
        File file = suggestedConfigFile != null ? suggestedConfigFile : new File("config", "ae2patterngen.cfg");
        Configuration cfg = new Configuration(file);
        try {
            cfg.load();
            int configured = cfg.getInt(
                "batchSize",
                CATEGORY_CONFLICT,
                DEFAULT_CONFLICT_BATCH_SIZE,
                MIN_CONFLICT_BATCH_SIZE,
                MAX_CONFLICT_BATCH_SIZE,
                "How many conflict groups are sent to client per batch.");
            conflictBatchSize = normalizeConflictBatchSize(configured);
        } catch (RuntimeException e) {
            conflictBatchSize = DEFAULT_CONFLICT_BATCH_SIZE;
            FMLLog.warning("[AE2PatternGen] Failed to load Forge config: %s", e.getMessage());
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    public static int getConflictBatchSize() {
        return conflictBatchSize;
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
