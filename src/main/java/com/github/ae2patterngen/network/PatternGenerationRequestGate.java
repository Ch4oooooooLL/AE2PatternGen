package com.github.ae2patterngen.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Collapses duplicate generate requests emitted by a single GUI interaction.
 */
final class PatternGenerationRequestGate {

    static final long DUPLICATE_WINDOW_MS = 500L;

    private static final Map<UUID, RecentRequest> RECENT_REQUESTS = new HashMap<UUID, RecentRequest>();

    private PatternGenerationRequestGate() {}

    static synchronized boolean shouldProcess(UUID playerUUID, String fingerprint, long nowMillis) {
        cleanupExpired(nowMillis);

        RecentRequest previous = RECENT_REQUESTS.get(playerUUID);
        if (previous != null && previous.fingerprint.equals(fingerprint)
            && nowMillis - previous.timestampMillis <= DUPLICATE_WINDOW_MS) {
            return false;
        }

        RECENT_REQUESTS.put(playerUUID, new RecentRequest(fingerprint, nowMillis));
        return true;
    }

    static String fingerprint(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput, String replacements, int targetTier) {
        return normalize(recipeMapId) + '\u001F'
            + normalize(outputOreDict)
            + '\u001F'
            + normalize(inputOreDict)
            + '\u001F'
            + normalize(ncItem)
            + '\u001F'
            + normalize(blacklistInput)
            + '\u001F'
            + normalize(blacklistOutput)
            + '\u001F'
            + normalize(replacements)
            + '\u001F'
            + targetTier;
    }

    static synchronized void reset() {
        RECENT_REQUESTS.clear();
    }

    private static void cleanupExpired(long nowMillis) {
        Iterator<Map.Entry<UUID, RecentRequest>> iterator = RECENT_REQUESTS.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            RecentRequest request = iterator.next()
                .getValue();
            if (nowMillis - request.timestampMillis > DUPLICATE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class RecentRequest {

        private final String fingerprint;
        private final long timestampMillis;

        private RecentRequest(String fingerprint, long timestampMillis) {
            this.fingerprint = fingerprint;
            this.timestampMillis = timestampMillis;
        }
    }
}
