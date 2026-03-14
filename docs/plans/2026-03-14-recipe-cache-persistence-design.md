# Recipe Cache Persistence Design

**Date:** 2026-03-14

## Goal

Implement persistent recipe caching so the pattern generator no longer rebuilds recipe collections on every preview or generate request, while keeping the cache pipeline extensible for later incremental rebuild, statistics, and index features.

## Scope

Apply the feature to the pattern-generator query flow rooted in these areas:

1. Recipe collection and filtering in [`GTRecipeSource`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/recipe/GTRecipeSource.java)
2. Persistent storage patterns established in [`PatternStorage`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/PatternStorage.java)
3. Server-side generation entry in [`PacketGeneratePatterns`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketGeneratePatterns.java)
4. GUI actions in [`GuiPatternGen`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/gui/GuiPatternGen.java)
5. Network registration in [`NetworkHandler`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/NetworkHandler.java)

Do not change recipe filtering semantics, pattern encoding behavior, or conflict-resolution policy beyond switching the recipe source from live collection to persistent cache.

## Confirmed UX

The feature should behave like this:

1. The player clicks a new cache button in the main pattern generator GUI to create or refresh the cache.
2. Cache creation runs on the server in a background worker and reports progress through chat without freezing the main thread.
3. Cache statistics are reported after the build finishes.
4. Both `Preview Count` and `Generate` are blocked when the cache is missing or invalid.
5. When blocked, both actions explicitly tell the player to create or refresh the cache first.
6. When the cache is valid, preview and generate read recipes only from the persisted cache.

## Design

### 1. Treat disk cache as the only query source

The persistent cache becomes the sole source for preview and generation queries. [`GTRecipeSource`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/recipe/GTRecipeSource.java) remains responsible for collecting live `RecipeEntry` data from GregTech and dynamic smelting sources, but that collection step only runs during cache construction.

This keeps runtime query behavior deterministic:

- `create/refresh cache` may touch live recipe registries
- `preview` and `generate` only read already-persisted cache data

That matches the required UX and gives a clean seam for future index-building or offline cache inspection.

### 2. Separate storage, environment-signature, and lifecycle services

Add three dedicated components:

- `RecipeCacheStorage`
  - file-level persistence for metadata and per-map recipe files
  - NBT + GZip serialization, mirroring the storage style already used by [`PatternStorage`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/storage/PatternStorage.java)
- `ModVersionHelper`
  - computes the current environment signature
  - loaded mod versions
  - config file hashes
  - recipe-map content hashes
- `RecipeCacheService`
  - cache lifecycle orchestration
  - validation, refresh, statistics, cleanup, and query entry points

The network layer and GUI should only call the service layer. They should never manipulate NBT, raw files, or hash logic directly.

### 3. Persist cache in two layers

Use this directory layout:

```text
<world>/<storageDirectory>/recipe_cache/
├── metadata.dat
├── gt.recipe.assembler.dat
├── gt.recipe.cutter.dat
└── ...
```

`metadata.dat` is the fast validation entry point. It stores:

- cache format version
- created and updated timestamps
- total recipe and recipe-map counts
- mod-version snapshot
- config hashes
- per-map summary state

Each `<map>.dat` file stores one recipe-map payload plus minimal file header information. The file name is derived from a sanitized `mapId`, while the original `mapId` is also stored inside the file so later naming-rule changes do not destroy meaning.

### 4. Use global validation plus per-map incremental refresh

Cache validity is split into two levels:

- global validity
  - metadata exists
  - cache version matches
  - required config hashes match
  - loaded mod versions match
- per-map validity
  - map file exists
  - stored `contentHash` matches the current hash

Preview and generate only care about global validity. If global validation fails, both actions are rejected immediately.

Cache creation uses per-map validation so it can rebuild only stale recipe maps. This keeps the initial implementation aligned with the spec while preserving a clean path to later optimizations.

### 5. Introduce query and statistics result objects

`RecipeCacheService` should return structured results instead of only raw lists.

Recommended types:

- `CacheStatistics`
  - total recipe count
  - total recipe-map count
  - cached mod count
  - cache directory size
  - created and updated timestamps
- `CacheQueryResult`
  - matched map ids
  - loaded recipes
  - total loaded count
  - total filtered count
  - cache source enum
  - optional warnings list

This is intentionally broader than the immediate feature set. It avoids painting later GUI or API work into a corner.

### 6. Keep the generation pipeline mostly intact

[`PacketGeneratePatterns`](D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/network/PacketGeneratePatterns.java) already owns:

- duplicate-request suppression
- storage-not-empty checks
- filter construction
- replacement rules
- conflict grouping
- final pattern generation

That flow should stay in place. The only meaningful change is the source of `RecipeEntry` data:

- before: live collection through `GTRecipeSource.collectRecipes(...)`
- after: persistent query through `RecipeCacheService.loadAndFilterRecipes(...)`

This reduces regression risk while moving recipe lookup behind the new cache boundary.

### 7. Give preview its own server query path

The current preview button computes results in the client GUI using live collection. That must change because the authoritative cache is server-side and preview must now be blocked when the cache is invalid.

Add a dedicated preview request/response packet pair so the server can:

- validate cache
- load cached recipes
- apply the same filter stack used by generation
- return the counts needed for the GUI status line

This preserves existing UX quality and avoids mixing preview-only behavior into the generate packet.

### 8. Report cache progress and completion through dedicated packets

Cache creation should use dedicated packets:

- `PacketCreateCache`
- `PacketCacheProgress`
- `PacketCacheStatistics`
- preview-specific request/result packets

Progress and completion are chat-driven so the GUI does not need a complex live progress panel. Statistics are still sent as a structured packet so future UI surfaces can reuse the same data.

### 9. Preserve explicit extension seams

The first iteration should leave these interfaces or abstractions in place:

- storage abstraction inside `RecipeCacheService`
- collector abstraction for recipe-map collection
- environment-signature helper for all invalidation inputs
- result objects for query and statistics payloads
- a single-thread executor hidden behind service methods

These seams make later work practical:

- parallel cache rebuild
- cache auto-refresh
- recipe indexes
- extra invalidation sources
- richer GUI statistics panels

## Error Handling

- Corrupt metadata or recipe-map files are treated as cache-invalid, not partially trusted.
- Cache build failures should send a clear chat error and leave the previous valid cache untouched whenever possible.
- If a single map rebuild fails, the service should surface the failure and mark the cache invalid rather than silently mixing stale and fresh data.
- A second cache-build request while one is running should be rejected with a clear “already building” message.

## Testing Strategy

1. Add unit tests for metadata state and NBT round-tripping.
2. Add storage tests for per-map save/load/delete and metadata persistence.
3. Add helper tests for config hashing and deterministic recipe-map hashing.
4. Add service tests for validation, stale-map detection, and query blocking behavior using injectable test doubles.
5. Add packet-level tests where practical for preview-count and cache-progress payloads.
6. Run focused tests first, then the full Gradle suite.

## Acceptance Criteria

- A cache can be created or refreshed from the pattern generator GUI.
- Cache files persist under the world save and survive restart.
- Both preview and generate are rejected when the cache is absent or invalid.
- Generation reads recipes from the persisted cache instead of collecting live recipes at request time.
- Cache completion reports usable statistics.
- The implementation exposes stable service/storage/helper seams for later iteration.
