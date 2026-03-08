package com.github.ae2patterngen.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConflictSelectionPolicyTest {

    @Test
    public void abortsInteractiveSelectionForLogSizedConflictSet() {
        assertTrue(ConflictSelectionPolicy.shouldAbortInteractiveSelection(14291, 1365));
    }

    @Test
    public void allowsInteractiveSelectionForSmallConflictSet() {
        assertFalse(ConflictSelectionPolicy.shouldAbortInteractiveSelection(120, 24));
    }
}
