package com.github.ae2patterngen.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForgeConfigTest {

    @Test
    public void conflictBatchSizeUsesExpectedDefault() {
        assertEquals(6, ForgeConfig.DEFAULT_CONFLICT_BATCH_SIZE);
    }

    @Test
    public void normalizeConflictBatchSizeClampsOutOfRangeValues() {
        assertEquals(1, ForgeConfig.normalizeConflictBatchSize(0));
        assertEquals(1, ForgeConfig.normalizeConflictBatchSize(-5));
        assertEquals(64, ForgeConfig.normalizeConflictBatchSize(999));
    }

    @Test
    public void normalizeConflictBatchSizeKeepsValidValue() {
        assertEquals(12, ForgeConfig.normalizeConflictBatchSize(12));
    }
}
