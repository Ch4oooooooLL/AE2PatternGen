# Recipe Cache Persistence Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a persistent recipe cache that becomes the only query source for preview and generation, with explicit cache creation, validation, statistics, and extension seams for future iterations.

**Architecture:** Keep live recipe collection inside `GTRecipeSource`, add a disk-backed `RecipeCacheStorage`, environment-signature logic in `ModVersionHelper`, and lifecycle/query orchestration in `RecipeCacheService`. Route GUI preview, cache creation, and generation packets through that service so preview and generate are both blocked when the cache is absent or invalid.

**Tech Stack:** Java 8, JUnit 4, Forge/FML simple network wrapper, Minecraft 1.7.10 NBT/GZip APIs, GTNH ModularUI, GregTech recipe maps

---

### Task 1: Replace the metadata model with a stable cache-state object

**Files:**
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/RecipeCacheMetadata.java`
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/storage/RecipeCacheMetadataTest.java`

**Step 1: Write the failing test**

Add tests that expect:
- a fresh metadata object to expose the current cache version and zero totals
- `updateRecipeMapInfo(...)` to recalculate totals from map entries
- mod and map state to be replaceable rather than blindly accumulated
- `touch()` to advance `lastUpdated`

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheMetadataTest`

Expected: FAIL because the current metadata file is incomplete and does not provide the intended stable behavior.

**Step 3: Write minimal implementation**

Replace the current broken half-implementation with:
- `cacheVersion`
- timestamp fields
- total counters
- nested `ModInfo` and `RecipeMapInfo`
- deterministic update helpers
- optional future-facing fields such as `cacheFileName`

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheMetadataTest`

Expected: PASS

### Task 2: Add storage tests for metadata and per-map payload persistence

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/RecipeCacheStorage.java`
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/storage/RecipeCacheStorageTest.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/config/ForgeConfig.java`
- Modify: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/config/ForgeConfigTest.java`

**Step 1: Write the failing test**

Add tests that expect:
- metadata round-trips through compressed NBT
- a `RecipeEntry` list can be saved and loaded for a single recipe map
- stale map files can be deleted
- cache directory naming is configurable and sanitized

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheStorageTest --tests com.github.ae2patterngen.config.ForgeConfigTest`

Expected: FAIL because storage and cache-directory support do not exist yet.

**Step 3: Write minimal implementation**

Implement:
- `RecipeCacheStorage.saveRecipeMap(...)`
- `RecipeCacheStorage.loadRecipeMap(...)`
- `RecipeCacheStorage.saveMetadata(...)`
- `RecipeCacheStorage.loadMetadata(...)`
- `RecipeCacheStorage.deleteRecipeMap(...)`
- `RecipeCacheStorage.clearAll(...)`
- config accessors for the cache subdirectory if needed

Use the same atomic temp-file move pattern already used by [`PatternStorage`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/PatternStorage.java).

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheStorageTest --tests com.github.ae2patterngen.config.ForgeConfigTest`

Expected: PASS

### Task 3: Add deterministic environment-signature helpers

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/ModVersionHelper.java`
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/storage/ModVersionHelperTest.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/config/ReplacementConfig.java`

**Step 1: Write the failing test**

Add tests that expect:
- config hash generation to be deterministic for the same file contents
- recipe-map hashing to be order-stable for the same recipe payload
- environment-signature helpers to tolerate missing config files cleanly

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.ModVersionHelperTest`

Expected: FAIL because helper APIs do not exist yet.

**Step 3: Write minimal implementation**

Implement helpers for:
- loaded mod versions
- config file discovery
- config file hashing
- recipe-map content hashing
- comparison against stored metadata snapshots

Keep the APIs static for now but isolate all signature logic here so future invalidation inputs stay local.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.ModVersionHelperTest`

Expected: PASS

### Task 4: Add service-level validation, rebuild, and query orchestration

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/RecipeCacheService.java`
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/storage/RecipeCacheServiceTest.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/CacheStatistics.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/CacheQueryResult.java`

**Step 1: Write the failing test**

Add tests that expect:
- cache validation to fail when metadata is missing or stale
- query APIs to reject invalid cache state
- refresh logic to rebuild only changed maps when metadata is present
- statistics to reflect metadata and on-disk cache state

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheServiceTest`

Expected: FAIL because no cache service exists yet.

**Step 3: Write minimal implementation**

Implement a service with:
- a single-thread worker
- an injectable storage/collector seam for tests
- `createOrRefreshCache(...)`
- `validateCache()`
- `loadRecipes(...)`
- `loadAndFilterRecipes(...)`
- `getStatistics()`
- `clearCache()`

Use a structured `CacheQueryResult` so preview and generate can share the same query output shape.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheServiceTest`

Expected: PASS

### Task 5: Add cache-build and preview network packets

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketCreateCache.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketCacheProgress.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketCacheStatistics.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketPreviewRecipeCount.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketPreviewRecipeCountResult.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/NetworkHandler.java`

**Step 1: Write the failing test or serialization check**

Add focused tests if practical, otherwise add round-trip assertions around packet payload fields for:
- preview request payload
- preview result payload
- cache statistics payload

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.network.PacketRecipeConflictBatchTest`

Expected: Either no dedicated packet test exists yet or a new focused packet test fails until the new packets are implemented.

**Step 3: Write minimal implementation**

Implement:
- a server packet for cache creation
- client packets for progress/statistics chat updates
- a preview request packet that uses the same filter payload shape as generation
- a preview result packet that can update the GUI status line
- network registration for all new packets

**Step 4: Run focused verification**

Run: `./gradlew test --tests com.github.ae2patterngen.network.PacketRecipeConflictBatchTest`

Expected: PASS, plus any new packet-focused tests you add.

### Task 6: Switch generation and preview to the cache service

**Files:**
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketGeneratePatterns.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PatternGenerationService.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/gui/GuiPatternGen.java`
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/recipe/GTRecipeSource.java`

**Step 1: Write the failing test**

Add or extend tests that expect:
- generation to reject invalid or missing cache
- generation to use cached recipes rather than direct live collection
- preview to request server-side counts instead of computing them locally

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.network.PatternGenerationRequestGateTest --tests com.github.ae2patterngen.storage.RecipeCacheServiceTest`

Expected: FAIL until the query path is switched to the cache service.

**Step 3: Write minimal implementation**

Change:
- `PacketGeneratePatterns` to call `RecipeCacheService.loadAndFilterRecipes(...)`
- preview button handling in `GuiPatternGen` to send the preview packet
- GUI cache button handling to send `PacketCreateCache`
- local status text updates so the user still sees preview results and cache-build request status

Keep conflict grouping and final pattern generation logic intact after the query source swap.

**Step 4: Run focused verification**

Run: `./gradlew test --tests com.github.ae2patterngen.network.PatternGenerationRequestGateTest --tests com.github.ae2patterngen.storage.RecipeCacheServiceTest`

Expected: PASS

### Task 7: Add language keys, user-facing messages, and documentation

**Files:**
- Modify: `D:/CODE/AE2PatternGen/src/main/resources/assets/ae2patterngen/lang/en_US.lang`
- Modify: `D:/CODE/AE2PatternGen/src/main/resources/assets/ae2patterngen/lang/zh_CN.lang`
- Modify: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/util/LangKeyCompletenessTest.java`
- Modify: `D:/CODE/AE2PatternGen/README.md`

**Step 1: Write the failing test**

Extend language-key completeness expectations for:
- cache-create button text
- cache status text
- cache invalid messages
- cache progress/statistics messages

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.util.LangKeyCompletenessTest`

Expected: FAIL until the new keys are added in both language files.

**Step 3: Write minimal implementation**

Add bilingual keys for:
- cache button
- preview blocked message
- generate blocked message
- cache building
- cache complete
- cache statistics
- cache already running
- cache internal error

Update the README to explain the new cache-first workflow.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.util.LangKeyCompletenessTest`

Expected: PASS

### Task 8: Run full verification and inspect the diff

**Files:**
- No code changes expected

**Step 1: Run focused cache-related tests**

Run: `./gradlew test --tests com.github.ae2patterngen.storage.RecipeCacheMetadataTest --tests com.github.ae2patterngen.storage.RecipeCacheStorageTest --tests com.github.ae2patterngen.storage.ModVersionHelperTest --tests com.github.ae2patterngen.storage.RecipeCacheServiceTest --tests com.github.ae2patterngen.util.LangKeyCompletenessTest`

Expected: PASS

**Step 2: Run the full test suite**

Run: `./gradlew test`

Expected: PASS

**Step 3: Review the diff**

Run: `git diff --stat`

Expected: only the intended cache, network, GUI, lang, docs, and test files are changed by this feature work.
