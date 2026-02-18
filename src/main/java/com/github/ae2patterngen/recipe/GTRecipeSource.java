package com.github.ae2patterngen.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * GT 配方收集器 — 从 RecipeMap 读取所有配方并转为 RecipeEntry
 * <p>
 * 支持模糊匹配: 输入 "blender" 可匹配 "gt.recipe.metablender"
 */
public class GTRecipeSource {

    /**
     * 获取所有已注册的 GT 配方表名称
     *
     * @return Map: unlocalizedName -> unlocalizedName
     */
    public static Map<String, String> getAvailableRecipeMaps() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            result.put(entry.getKey(), entry.getKey());
        }
        return result;
    }

    /**
     * 模糊匹配配方表 ID
     * <p>
     * 匹配规则 (按优先级):
     * 1. 精确匹配
     * 2. 不区分大小写的子串匹配 (如 "blender" -> "gt.recipe.metablender")
     * 3. 如果多个匹配，返回所有匹配的配方表
     *
     * @param keyword 用户输入的关键字
     * @return 匹配到的配方表 ID 列表
     */
    public static List<String> findMatchingRecipeMaps(String keyword) {
        List<String> matches = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        // 1. 精确匹配
        if (RecipeMap.ALL_RECIPE_MAPS.containsKey(keyword)) {
            matches.add(keyword);
            return matches;
        }

        // 2. 不区分大小写的子串匹配
        for (String mapId : RecipeMap.ALL_RECIPE_MAPS.keySet()) {
            if (mapId.toLowerCase()
                .contains(lowerKeyword)) {
                matches.add(mapId);
            }
        }

        return matches;
    }

    /**
     * 从匹配的配方表中收集所有配方 (支持模糊匹配)
     *
     * @param keyword 配方表关键字 (支持子串匹配)
     * @return 所有匹配配方表的 RecipeEntry 列表
     */
    public static List<RecipeEntry> collectRecipes(String keyword) {
        List<RecipeEntry> entries = new ArrayList<>();

        List<String> matchedMaps = findMatchingRecipeMaps(keyword);
        if (matchedMaps.isEmpty()) {
            return entries;
        }

        for (String mapId : matchedMaps) {
            RecipeMap<?> targetMap = RecipeMap.ALL_RECIPE_MAPS.get(mapId);
            if (targetMap == null) continue;

            Collection<GTRecipe> recipes = targetMap.getAllRecipes();
            if (recipes == null) continue;

            for (GTRecipe recipe : recipes) {
                if (recipe == null || !recipe.mEnabled) continue;

                ItemStack[] normalInputs = recipe.mInputs;
                ItemStack[] specialItems = new ItemStack[0];

                RecipeEntry entry = new RecipeEntry(
                    "gt",
                    mapId,
                    mapId,
                    normalInputs,
                    recipe.mOutputs,
                    recipe.mFluidInputs,
                    recipe.mFluidOutputs,
                    specialItems,
                    recipe.mDuration,
                    recipe.mEUt);

                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * 收集所有配方表的所有配方
     */
    public static List<RecipeEntry> collectAllRecipes() {
        List<RecipeEntry> all = new ArrayList<>();
        for (String mapId : RecipeMap.ALL_RECIPE_MAPS.keySet()) {
            all.addAll(collectRecipes(mapId));
        }
        return all;
    }
}
