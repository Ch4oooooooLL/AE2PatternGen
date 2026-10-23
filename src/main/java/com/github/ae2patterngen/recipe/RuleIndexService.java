package com.github.ae2patterngen.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.ae2patterngen.storage.CacheQueryResult;
import com.github.ae2patterngen.storage.RecipeCacheMetadata;
import com.github.ae2patterngen.storage.RecipeCacheService;

/**
 * 配方池规则索引服务。
 * <p>
 * 分类结果完全由服务端配方缓存派生：缓存重建即失效，重新访问时按需重新解析。
 * 结果按池做小型 LRU 缓存，避免同一个池被反复解析（例如反复展开规则组）。
 */
public final class RuleIndexService {

    private static final int MAX_CACHED_POOLS = 6;

    private static final Map<String, PoolRuleIndex> CACHE = new LinkedHashMap<String, PoolRuleIndex>(16, 0.75f, true) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PoolRuleIndex> eldest) {
            return size() > MAX_CACHED_POOLS;
        }
    };

    private RuleIndexService() {}

    /** 已缓存配方表摘要（按配方数量降序），供"选择配方池"列表使用。 */
    public static List<RecipeCacheMetadata.RecipeMapInfo> listPools() {
        return RecipeCacheService.getCachedRecipeMaps();
    }

    /**
     * 取某个配方池的规则索引；缓存缺失或失效时返回 null。
     */
    public static PoolRuleIndex getIndex(String mapId) {
        if (mapId == null || mapId.trim()
            .isEmpty()) {
            return null;
        }

        String key = mapId.trim();
        synchronized (CACHE) {
            PoolRuleIndex cached = CACHE.get(key);
            if (cached != null) {
                return cached;
            }
        }

        CacheQueryResult query = RecipeCacheService.loadSingleRecipeMap(key);
        if (!query.cacheValid) {
            return null;
        }

        PoolRuleIndex index = RuleClassifier.classify(key, query.recipes, RuleClassifier.defaultCircuitDetector());
        synchronized (CACHE) {
            CACHE.put(key, index);
        }
        return index;
    }

    /** 配方缓存发生变化时清空派生索引。 */
    public static void invalidate() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
