package com.github.ae2patterngen.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.github.ae2patterngen.network.PacketBrowseRequest;
import com.github.ae2patterngen.network.PacketBrowseResponse;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;

/**
 * 配方浏览器客户端状态：服务端返回什么就显示什么，不在客户端自行解析配方。
 */
public final class RecipeBrowserState {

    public static final int VIEW_POOLS = 0;
    public static final int VIEW_RULES = 1;
    public static final int VIEW_RECIPES = 2;
    public static final int VIEW_PREVIEW = 3;
    public static final int VIEW_STORAGE = 4;

    public int view = VIEW_POOLS;
    public String status = "";
    public String search = "";
    public int page = 0;

    /** 配方池列表。 */
    public final List<String> poolMapIds = new ArrayList<String>();
    public final List<String> poolModIds = new ArrayList<String>();
    public final List<Integer> poolRecipeCounts = new ArrayList<Integer>();

    /** 当前池与规则组列表。 */
    public String mapId = "";
    public int totalRecipeCount = 0;
    public int hiddenSingletonCount = 0;
    public int hiddenUnruledRecipeCount = 0;
    public boolean showSingletonRules = false;
    public boolean showUnruledGroup = false;
    public final List<PacketBrowseResponse.GroupSummary> rules = new ArrayList<PacketBrowseResponse.GroupSummary>();

    /** 当前规则组与配方明细。 */
    public String groupId = "";
    public PacketBrowseResponse.GroupSummary groupRule;
    public int recipeTotalCount = 0;
    public final List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();

    /** 三级页面：与样板生成器一致的过滤条件。 */
    public String filterOutput = "";
    public String filterInput = "";
    public String filterNc = "";
    public String excludeInput = "";
    public String excludeOutput = "";
    public int tierIndex = 0;

    /** 三级页面：最近一次预览结果。 */
    public boolean previewLoaded = false;
    public int groupTotalCount = 0;
    public int matchedCount = 0;
    public int distinctInputCount = 0;
    public int distinctOutputCount = 0;
    public ItemStack[] inputSamples = new ItemStack[0];
    public ItemStack[] outputSamples = new ItemStack[0];

    /** 浏览器仓储视图。 */
    public int storageCount = 0;
    public String storageSource = "";
    public int storagePage = 0;
    public int storagePageSize = 12;
    public final List<String> storagePreviews = new ArrayList<String>();

    void applyResponse(PacketBrowseResponse response) {
        if (response == null) {
            return;
        }

        switch (response.type) {
            case PacketBrowseResponse.TYPE_POOLS:
                view = VIEW_POOLS;
                status = "";
                page = 0;
                poolMapIds.clear();
                poolModIds.clear();
                poolRecipeCounts.clear();
                poolMapIds.addAll(response.poolMapIds);
                poolModIds.addAll(response.poolModIds);
                poolRecipeCounts.addAll(response.poolRecipeCounts);
                break;
            case PacketBrowseResponse.TYPE_GROUPS:
                view = VIEW_RULES;
                status = "";
                page = 0;
                mapId = response.mapId;
                totalRecipeCount = response.totalRecipeCount;
                hiddenSingletonCount = response.hiddenSingletonCount;
                hiddenUnruledRecipeCount = response.hiddenUnruledRecipeCount;
                showSingletonRules = response.showSingletonRules;
                showUnruledGroup = response.showUnruledGroup;
                rules.clear();
                rules.addAll(response.groups);
                break;
            case PacketBrowseResponse.TYPE_RECIPES:
                view = VIEW_RECIPES;
                status = "";
                page = 0;
                mapId = response.mapId;
                groupId = response.groupId;
                recipeTotalCount = response.recipeTotalCount;
                recipes.clear();
                recipes.addAll(response.recipes);
                break;
            case PacketBrowseResponse.TYPE_PREVIEW:
                view = VIEW_PREVIEW;
                status = "";
                mapId = response.mapId;
                if (!response.groupId.isEmpty()) {
                    groupId = response.groupId;
                }
                groupTotalCount = response.groupTotalCount;
                matchedCount = response.matchedCount;
                distinctInputCount = response.distinctInputCount;
                distinctOutputCount = response.distinctOutputCount;
                inputSamples = response.inputSamples;
                outputSamples = response.outputSamples;
                previewLoaded = true;
                break;
            case PacketBrowseResponse.TYPE_STORAGE:
                view = VIEW_STORAGE;
                status = "";
                storageCount = response.storageCount;
                storageSource = response.storageSource;
                storagePage = response.storagePageIndex;
                page = response.storagePageIndex;
                storagePageSize = Math.max(1, response.storagePageSize);
                storagePreviews.clear();
                storagePreviews.addAll(response.storagePreviews);
                break;
            case PacketBrowseResponse.TYPE_STATUS:
            default:
                status = response.message != null ? response.message : "";
                break;
        }
    }

    /** 当前编辑中的过滤条件（与样板生成器同语法）。 */
    public PacketBrowseRequest.Filters filters() {
        return new PacketBrowseRequest.Filters(
            filterOutput,
            filterInput,
            filterNc,
            excludeInput,
            excludeOutput,
            tierIndex - 1);
    }

    public void clearFilters() {
        filterOutput = "";
        filterInput = "";
        filterNc = "";
        excludeInput = "";
        excludeOutput = "";
        tierIndex = 0;
        clearPreviewData();
    }

    public void clearPreviewData() {
        previewLoaded = false;
        groupTotalCount = 0;
        matchedCount = 0;
        distinctInputCount = 0;
        distinctOutputCount = 0;
        inputSamples = new ItemStack[0];
        outputSamples = new ItemStack[0];
    }

    /** 当前池的本地化名称；GT 的 NEI 标签名就是 mapId 这个语言键。 */
    public static String displayNameOfPool(String poolMapId) {
        if (poolMapId == null || poolMapId.isEmpty()) {
            return "";
        }
        String translated = StatCollector.translateToLocal(poolMapId);
        return translated != null && !translated.isEmpty() && !translated.equals(poolMapId) ? translated : poolMapId;
    }

    public static String displayNameOf(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return I18nUtil.tr("ae2patterngen.gui.browser.unknown_item");
        }
        return com.github.ae2patterngen.util.ItemStackUtil.getSafeDisplayName(stack);
    }

    /** 按搜索词过滤后的配方池下标。 */
    public List<Integer> filteredPoolIndexes() {
        List<Integer> indexes = new ArrayList<Integer>();
        String needle = search != null ? search.trim()
            .toLowerCase() : "";
        for (int i = 0; i < poolMapIds.size(); i++) {
            if (needle.isEmpty()) {
                indexes.add(i);
                continue;
            }
            String mapIdValue = String.valueOf(poolMapIds.get(i))
                .toLowerCase();
            String localized = displayNameOfPool(poolMapIds.get(i)).toLowerCase();
            String modId = i < poolModIds.size() ? String.valueOf(poolModIds.get(i))
                .toLowerCase() : "";
            if (mapIdValue.contains(needle) || localized.contains(needle) || modId.contains(needle)) {
                indexes.add(i);
            }
        }
        return indexes;
    }
}
