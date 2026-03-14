package com.github.ae2patterngen.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Test;

import com.github.ae2patterngen.config.ForgeConfig;

public class PatternGenerationRequestGateTest {

    @After
    public void tearDown() {
        PatternGenerationRequestGate.reset();
    }

    @Test
    public void blocksDuplicateRequestsWithinGuardWindow() {
        UUID playerUUID = UUID.randomUUID();
        String fingerprint = PatternGenerationRequestGate
            .fingerprint("gt.recipe.assembler", "(dustCopper)", "", "", "", "", "", 3);
        long windowMs = ForgeConfig.getDuplicateWindowMs();

        assertTrue(PatternGenerationRequestGate.shouldProcess(playerUUID, fingerprint, 1_000L));
        assertFalse(PatternGenerationRequestGate.shouldProcess(playerUUID, fingerprint, 1_000L + windowMs));
        assertTrue(PatternGenerationRequestGate.shouldProcess(playerUUID, fingerprint, 1_000L + windowMs + 1L));
    }

    @Test
    public void allowsDifferentRequestsOrPlayers() {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        String baseFingerprint = PatternGenerationRequestGate
            .fingerprint("gt.recipe.assembler", "(dustCopper)", "", "", "", "", "", 3);
        String changedFingerprint = PatternGenerationRequestGate
            .fingerprint("gt.recipe.assembler", "(dustTin)", "", "", "", "", "", 3);

        assertTrue(PatternGenerationRequestGate.shouldProcess(firstPlayer, baseFingerprint, 2_000L));
        assertTrue(PatternGenerationRequestGate.shouldProcess(firstPlayer, changedFingerprint, 2_100L));
        assertTrue(PatternGenerationRequestGate.shouldProcess(secondPlayer, baseFingerprint, 2_100L));
    }
}
