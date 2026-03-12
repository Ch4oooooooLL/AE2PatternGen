package com.github.ae2patterngen.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * GT 配方收集器 — 从 RecipeMap 读取所有配方并转为 RecipeEntry
 * <p>
 * 支持模糊匹配: 输入 "blender" 可匹配 "gt.recipe.metablender"
 */
public class GTRecipeSource {

    private static final RecipeCollectionCache<String, List<RecipeEntry>> COLLECTED_RECIPE_CACHE = new RecipeCollectionCache<String, List<RecipeEntry>>();

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
        if (keyword == null) {
            return new ArrayList<>();
        }

        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> matches = new LinkedHashSet<>();
        String lowerKeyword = normalized.toLowerCase();

        // 1. 精确匹配
        if (RecipeMap.ALL_RECIPE_MAPS.containsKey(normalized)) {
            matches.add(normalized);
        }

        // 2. 兼容旧格式 ID (xxx_1_2_3_4_5)
        RecipeMap<?> fromLegacy = RecipeMap.getFromOldIdentifier(normalized);
        if (fromLegacy != null) {
            matches.add(fromLegacy.unlocalizedName);
        }

        // 3. 不区分大小写的子串匹配 (map id + NEI transfer id)
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            String mapId = entry.getKey();
            RecipeMap<?> map = entry.getValue();

            if (mapId.toLowerCase()
                .contains(lowerKeyword)) {
                matches.add(mapId);
                continue;
            }

            if (map != null && map.getFrontend() != null
                && map.getFrontend()
                    .getUIProperties() != null) {
                String transferId = map.getFrontend()
                    .getUIProperties().neiTransferRectId;
                if (transferId != null && transferId.toLowerCase()
                    .contains(lowerKeyword)) {
                    matches.add(mapId);
                }
            }
        }

        return new ArrayList<>(matches);
    }

    /**
     * 从匹配的配方表中收集所有配方 (支持模糊匹配)
     *
     * @param keyword 配方表关键字 (支持子串匹配)
     * @return 所有匹配配方表的 RecipeEntry 列表
     */
    public static List<RecipeEntry> collectRecipes(String keyword) {
        List<String> matchedMaps = findMatchingRecipeMaps(keyword);
        if (matchedMaps.isEmpty()) {
            return new ArrayList<>();
        }

        String cacheKey = buildCollectionCacheKey(matchedMaps);
        return COLLECTED_RECIPE_CACHE
            .getOrCompute(cacheKey, () -> Collections.unmodifiableList(collectRecipesForMatchedMaps(matchedMaps)));
    }

    private static List<RecipeEntry> collectRecipesForMatchedMaps(List<String> matchedMaps) {
        List<RecipeEntry> entries = new ArrayList<>();
        Set<String> processedKeys = new java.util.HashSet<>();

        for (String mapId : matchedMaps) {
            RecipeMap<?> targetMap = RecipeMap.ALL_RECIPE_MAPS.get(mapId);
            if (targetMap == null) continue;

            Collection<GTRecipe> recipes = targetMap.getAllRecipes();
            if (recipes != null) {
                for (GTRecipe recipe : recipes) {
                    if (recipe == null || !recipe.mEnabled) continue;

                    // 生成配方唯一性 Key (基于输入、输出、时长、EU)
                    String recipeKey = generateRecipeKey(recipe);
                    if (!processedKeys.add(recipeKey)) continue;

                    ItemStack[] normalInputs = recipe.mInputs;
                    ItemStack[] specialItems = new ItemStack[0];
                    if (recipe.mSpecialItems instanceof ItemStack[]) {
                        specialItems = (ItemStack[]) recipe.mSpecialItems;
                    } else if (recipe.mSpecialItems instanceof ItemStack) {
                        specialItems = new ItemStack[] { (ItemStack) recipe.mSpecialItems };
                    }

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

            collectDynamicSmeltingRecipesIfNeeded(mapId, targetMap, entries, processedKeys);
        }

        return entries;
    }

    private static String buildCollectionCacheKey(List<String> matchedMaps) {
        if (matchedMaps == null || matchedMaps.isEmpty()) {
            return "";
        }
        return String.join("\u001F", matchedMaps);
    }

    /**
     * GT 的 furnace / microwave 使用 NonGTBackend 动态查配方，不会预先填充到 getAllRecipes()。
     * 因此这里回退到原版 FurnaceRecipes 枚举并构造 RecipeEntry。
     */
    private static void collectDynamicSmeltingRecipesIfNeeded(String mapId, RecipeMap<?> targetMap,
        List<RecipeEntry> entries, Set<String> processedKeys) {
        if (targetMap == null || (!"gt.recipe.furnace".equals(mapId) && !"gt.recipe.microwave".equals(mapId))) {
            return;
        }

        Map<ItemStack, ItemStack> smelting = FurnaceRecipes.smelting()
            .getSmeltingList();
        if (smelting == null || smelting.isEmpty()) {
            return;
        }

        // NonGTBackend 配方在查询时动态生成。这里通过候选输入触发 findRecipe。
        // 微波炉有一本书特判，不在原版熔炉表里，手动补一个候选。
        Set<String> inputDedup = new LinkedHashSet<>();
        for (Map.Entry<ItemStack, ItemStack> e : smelting.entrySet()) {
            ItemStack rawInput = e.getKey();
            if (rawInput == null || rawInput.getItem() == null) {
                continue;
            }
            ItemStack input = rawInput.copy();
            if (input.stackSize <= 0) {
                input.stackSize = 1;
            }
            String inputKey = stackKey(input);
            if (!inputDedup.add(inputKey)) {
                continue;
            }

            GTRecipe dynamic = targetMap.findRecipeQuery()
                .items(input)
                .dontCheckStackSizes(true)
                .find();
            if (dynamic == null || !dynamic.mEnabled) {
                continue;
            }
            if ((dynamic.mOutputs == null || dynamic.mOutputs.length == 0)
                && (dynamic.mFluidOutputs == null || dynamic.mFluidOutputs.length == 0)) {
                continue;
            }

            ItemStack[] normalInputs = dynamic.mInputs != null ? dynamic.mInputs : new ItemStack[] { input };
            ItemStack[] specialItems = new ItemStack[0];
            if (dynamic.mSpecialItems instanceof ItemStack[]) {
                specialItems = (ItemStack[]) dynamic.mSpecialItems;
            } else if (dynamic.mSpecialItems instanceof ItemStack) {
                specialItems = new ItemStack[] { (ItemStack) dynamic.mSpecialItems };
            }

            String dynamicKey = generateRecipeKey(dynamic) + "|MAP:" + mapId;
            if (!processedKeys.add(dynamicKey)) {
                continue;
            }

            entries.add(
                new RecipeEntry(
                    "gt",
                    mapId,
                    mapId,
                    normalInputs,
                    dynamic.mOutputs,
                    dynamic.mFluidInputs,
                    dynamic.mFluidOutputs,
                    specialItems,
                    dynamic.mDuration,
                    dynamic.mEUt));
        }

        if ("gt.recipe.microwave".equals(mapId)) {
            ItemStack microwaveBook = new ItemStack(Items.book, 1, 0);
            GTRecipe bookRecipe = targetMap.findRecipeQuery()
                .items(microwaveBook)
                .dontCheckStackSizes(true)
                .find();
            if (bookRecipe != null && bookRecipe.mEnabled
                && bookRecipe.mOutputs != null
                && bookRecipe.mOutputs.length > 0) {
                String bookKey = generateRecipeKey(bookRecipe) + "|MAP:" + mapId;
                if (processedKeys.add(bookKey)) {
                    entries.add(
                        new RecipeEntry(
                            "gt",
                            mapId,
                            mapId,
                            bookRecipe.mInputs,
                            bookRecipe.mOutputs,
                            bookRecipe.mFluidInputs,
                            bookRecipe.mFluidOutputs,
                            new ItemStack[0],
                            bookRecipe.mDuration,
                            bookRecipe.mEUt));
                }
            }
        }
    }

    private static String stackKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "NULL";
        }
        Item item = stack.getItem();
        Object name = Item.itemRegistry.getNameForObject(item);
        return String.valueOf(name) + "@" + stack.getItemDamage() + "@" + stack.stackSize;
    }

    private static String generateRecipeKey(GTRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append(recipe.mDuration)
            .append(":")
            .append(recipe.mEUt)
            .append("|");
        if (recipe.mInputs != null) {
            for (ItemStack is : recipe.mInputs) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append("@")
                        .append(is.stackSize)
                        .append(",");
                else sb.append("NULL,");
            }
        }
        sb.append("|");
        if (recipe.mOutputs != null) {
            for (ItemStack is : recipe.mOutputs) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append("@")
                        .append(is.stackSize)
                        .append(",");
                else sb.append("NULL,");
            }
        }
        sb.append("|SP:");
        if (recipe.mSpecialItems instanceof ItemStack) {
            ItemStack is = (ItemStack) recipe.mSpecialItems;
            if (is.getItem() != null) {
                sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                    .append("@")
                    .append(is.getItemDamage());
            }
        } else if (recipe.mSpecialItems instanceof ItemStack[]) {
            for (ItemStack is : (ItemStack[]) recipe.mSpecialItems) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append(",");
            }
        }
        sb.append("|FI:");
        if (recipe.mFluidInputs != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.mFluidInputs) {
                if (fs != null && fs.getFluid() != null) {
                    sb.append(
                        fs.getFluid()
                            .getName())
                        .append("@")
                        .append(fs.amount)
                        .append(",");
                }
            }
        }
        sb.append("|FO:");
        if (recipe.mFluidOutputs != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.mFluidOutputs) {
                if (fs != null && fs.getFluid() != null) {
                    sb.append(
                        fs.getFluid()
                            .getName())
                        .append("@")
                        .append(fs.amount)
                        .append(",");
                }
            }
        }
        return sb.toString();
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
