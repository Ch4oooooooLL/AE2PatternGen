package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.delivery.HatchBindingRegistry;
import com.github.ae2patterngen.delivery.PatternDeliveryService;
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.RecipeFilterFactory;
import com.github.ae2patterngen.recipe.PoolRuleIndex;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.recipe.RecipePreviewSupport;
import com.github.ae2patterngen.recipe.RuleGroup;
import com.github.ae2patterngen.recipe.RuleIndexService;
import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.storage.RecipeCacheMetadata;
import com.github.ae2patterngen.storage.RecipeCacheService;
import com.github.ae2patterngen.util.I18nUtil;

/**
 * 配方浏览器服务端处理：所有数据都在这里基于持久化配方缓存重新计算。
 */
final class PacketBrowseResponder {

    /** 单次下发的配方明细上限，避免超大规则组撑爆网络包。 */
    static final int MAX_RECIPES_PER_GROUP = 200;
    /** 浏览器仓储每页条数：与界面行数对齐，避免服务端给了界面放不下。 */
    static final int STORAGE_PAGE_SIZE = 12;
    private static final int MAX_OUTPUT_SAMPLES = 3;

    private PacketBrowseResponder() {}

    static PacketBrowseResponse handle(EntityPlayerMP player, PacketBrowseRequest request) {
        switch (request.action) {
            case PacketBrowseRequest.ACTION_GROUPS:
                return buildGroups(request);
            case PacketBrowseRequest.ACTION_RECIPES:
                return buildRecipes(request);
            case PacketBrowseRequest.ACTION_PREVIEW:
                return buildPreview(request);
            case PacketBrowseRequest.ACTION_GENERATE:
                return generate(player, request);
            case PacketBrowseRequest.ACTION_GENERATE_FILTERED:
                return generateFiltered(player, request);
            case PacketBrowseRequest.ACTION_DELIVER:
                return deliver(player, request);
            case PacketBrowseRequest.ACTION_STORAGE_LIST:
                return storageList(player, request);
            case PacketBrowseRequest.ACTION_STORAGE_DELETE:
                return storageDelete(player, request);
            case PacketBrowseRequest.ACTION_STORAGE_CLEAR:
                return storageClear(player);
            case PacketBrowseRequest.ACTION_OPEN:
            default:
                return buildPools();
        }
    }

    private static PacketBrowseResponse buildPools() {
        if (!RecipeCacheService.validateCache()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }

        List<RecipeCacheMetadata.RecipeMapInfo> pools = RuleIndexService.listPools();
        if (pools.isEmpty()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }

        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = PacketBrowseResponse.TYPE_POOLS;
        for (RecipeCacheMetadata.RecipeMapInfo info : pools) {
            if (info == null || info.mapId == null || info.mapId.isEmpty() || info.recipeCount <= 0) {
                continue;
            }
            response.poolMapIds.add(info.mapId);
            response.poolModIds.add(info.modId != null ? info.modId : "");
            response.poolRecipeCounts.add(info.recipeCount);
        }
        if (response.poolMapIds.isEmpty()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }
        return response;
    }

    private static PacketBrowseResponse buildGroups(PacketBrowseRequest request) {
        if (!RecipeCacheService.validateCache()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }

        PoolRuleIndex index = RuleIndexService.getIndex(request.mapId);
        if (index == null) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.pool_unavailable"));
        }

        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = PacketBrowseResponse.TYPE_GROUPS;
        response.mapId = index.mapId;
        response.totalRecipeCount = index.totalRecipeCount;
        response.hiddenSingletonCount = index.getSingletonRuleCount();
        response.hiddenUnruledRecipeCount = index.getUnruledRecipeCount();
        response.showSingletonRules = request.showSingletonRules;
        response.showUnruledGroup = request.showUnruledGroup;

        for (RuleGroup group : index.visibleGroups(request.showSingletonRules, request.showUnruledGroup)) {
            PacketBrowseResponse.GroupSummary summary = new PacketBrowseResponse.GroupSummary();
            summary.groupId = group.groupId;
            summary.circuitDamages = group.circuitDamages;
            summary.circuitDisplay = group.circuitDisplay;
            summary.ncItems = group.ncItems;
            summary.outputSamples = collectOutputSamples(group);
            summary.recipeCount = group.size();
            summary.unruled = group.unruled;
            response.groups.add(summary);
        }
        return response;
    }

    private static PacketBrowseResponse buildRecipes(PacketBrowseRequest request) {
        if (!RecipeCacheService.validateCache()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }

        PoolRuleIndex index = RuleIndexService.getIndex(request.mapId);
        if (index == null) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.pool_unavailable"));
        }

        RuleGroup group = index.findById(request.groupId);
        if (group == null) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.rule_unavailable"));
        }

        PacketBrowseResponse response = new PacketBrowseResponse();
        response.type = PacketBrowseResponse.TYPE_RECIPES;
        response.mapId = index.mapId;
        response.groupId = group.groupId;
        response.recipeTotalCount = group.size();
        int limit = Math.min(MAX_RECIPES_PER_GROUP, group.size());
        response.recipes = new ArrayList<RecipeEntry>(group.recipes.subList(0, limit));
        return response;
    }

    private static PacketBrowseResponse buildPreview(PacketBrowseRequest request) {
        if (!RecipeCacheService.validateCache()) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid"));
        }

        PoolRuleIndex index = RuleIndexService.getIndex(request.mapId);
        if (index == null) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.pool_unavailable"));
        }

        RuleGroup group = index.findById(request.groupId);
        if (group == null) {
            return PacketBrowseResponse.statusText(I18nUtil.tr("ae2patterngen.msg.browser.rule_unavailable"));
        }

        List<RecipeEntry> matched = applyFilters(group.recipes, request.filters);
        return PacketBrowseResponse
            .preview(index.mapId, group.groupId, RecipePreviewSupport.build(group.size(), matched));
    }

    /** 右键样板接收方：投递浏览器仓储里的样板，并写入名称/电路、把 NC 物品回给客户端加书签。 */
    private static PacketBrowseResponse deliver(EntityPlayerMP player, PacketBrowseRequest request) {
        Object target = PatternDeliveryService
            .resolveTarget(player.worldObj, request.targetX, request.targetY, request.targetZ);
        if (target == null) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.delivery.not_supported");
            return null;
        }

        PatternDeliveryService.Result result = PatternDeliveryService.deliver(player.getUniqueID(), target);
        reportDelivery(player, result);

        if (result.ncItems.length > 0) {
            NetworkHandler.INSTANCE.sendTo(new PacketNeiBookmarks(Arrays.asList(result.ncItems)), player);
        }
        return null;
    }

    private static void reportDelivery(EntityPlayerMP player, PatternDeliveryService.Result result) {
        switch (result.status) {
            case DELIVERED:
                send(
                    player,
                    EnumChatFormatting.GREEN,
                    "ae2patterngen.msg.delivery.delivered",
                    result.inserted,
                    result.capacity);
                if (result.renamed) {
                    send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.delivery.renamed", result.appliedName);
                }
                if (result.circuitWritten) {
                    send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.delivery.circuit_written", result.circuit);
                } else if (result.circuitCleared) {
                    send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.delivery.circuit_cleared");
                }
                if (result.ncItems.length > 0) {
                    send(
                        player,
                        EnumChatFormatting.GRAY,
                        "ae2patterngen.msg.delivery.nc_bookmarked",
                        result.ncItems.length);
                }
                if (!result.bindingKnown) {
                    send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.delivery.unknown_binding");
                }
                break;
            case NO_SPACE:
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.delivery.no_space", result.capacity);
                break;
            case NO_PATTERNS:
                send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.delivery.no_patterns");
                break;
            case NOT_SUPPORTED:
            default:
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.delivery.not_supported");
                break;
        }
    }

    /** 浏览器仓储：列一页（页码越界时自动收口到最后一页）。 */
    private static PacketBrowseResponse storageList(EntityPlayerMP player, PacketBrowseRequest request) {
        UUID uuid = player.getUniqueID();
        int page = Math.max(0, request.pageIndex);

        PatternStorage.StorageSummary summary = PatternStorage
            .getPage(uuid, page, STORAGE_PAGE_SIZE, PatternStorage.CHANNEL_BROWSER);
        int pageCount = Math.max(1, (summary.count + STORAGE_PAGE_SIZE - 1) / STORAGE_PAGE_SIZE);
        if (page >= pageCount) {
            page = pageCount - 1;
            summary = PatternStorage.getPage(uuid, page, STORAGE_PAGE_SIZE, PatternStorage.CHANNEL_BROWSER);
        }

        return PacketBrowseResponse.storage(summary.count, summary.source, page, STORAGE_PAGE_SIZE, summary.previews);
    }

    private static PacketBrowseResponse storageDelete(EntityPlayerMP player, PacketBrowseRequest request) {
        UUID uuid = player.getUniqueID();
        ItemStack removed = PatternStorage.delete(uuid, request.recipeIndex, PatternStorage.CHANNEL_BROWSER);
        if (removed == null) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.browser.storage_missing_index");
        }
        return storageList(player, request);
    }

    private static PacketBrowseResponse storageClear(EntityPlayerMP player) {
        PatternStorage.clear(player.getUniqueID(), PatternStorage.CHANNEL_BROWSER);
        send(player, EnumChatFormatting.GREEN, "ae2patterngen.msg.browser.storage_cleared");
        return PacketBrowseResponse.storage(0, "", 0, STORAGE_PAGE_SIZE, new ArrayList<String>());
    }

    /**
     * 用与样板生成器完全相同的一套过滤条件筛出命中的配方。
     */
    static List<RecipeEntry> applyFilters(List<RecipeEntry> recipes, PacketBrowseRequest.Filters filters) {
        List<RecipeEntry> matched = new ArrayList<RecipeEntry>();
        if (recipes == null) {
            return matched;
        }

        PacketBrowseRequest.Filters safe = filters != null ? filters : PacketBrowseRequest.Filters.none();
        CompositeFilter filter = RecipeFilterFactory.build(
            safe.outputOre,
            safe.inputOre,
            safe.ncItem,
            safe.blacklistInput,
            safe.blacklistOutput,
            safe.targetTier);

        for (RecipeEntry recipe : recipes) {
            if (recipe != null && filter.matches(recipe)) {
                matched.add(recipe);
            }
        }
        return matched;
    }

    private static PacketBrowseResponse generate(EntityPlayerMP player, PacketBrowseRequest request) {
        UUID uuid = player.getUniqueID();
        if (!PatternGenerationRequestGate
            .shouldProcess(uuid, fingerprint("group", request, ""), System.currentTimeMillis())) {
            return null;
        }

        PoolRuleIndex index = requireIndex(player, request);
        if (index == null) {
            return null;
        }
        RuleGroup group = index.findById(request.groupId);
        if (group == null) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.browser.rule_unavailable");
            return null;
        }

        List<RecipeEntry> targets;
        if (request.recipeIndex >= 0 && request.recipeIndex < group.size()) {
            targets = new ArrayList<RecipeEntry>(1);
            targets.add(group.recipes.get(request.recipeIndex));
        } else {
            targets = new ArrayList<RecipeEntry>(group.recipes);
        }
        return submit(player, index.mapId, group, targets);
    }

    private static PacketBrowseResponse generateFiltered(EntityPlayerMP player, PacketBrowseRequest request) {
        UUID uuid = player.getUniqueID();
        if (!PatternGenerationRequestGate.shouldProcess(
            uuid,
            fingerprint("filtered", request, filterKey(request.filters)),
            System.currentTimeMillis())) {
            return null;
        }

        PoolRuleIndex index = requireIndex(player, request);
        if (index == null) {
            return null;
        }
        RuleGroup group = index.findById(request.groupId);
        if (group == null) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.browser.rule_unavailable");
            return null;
        }

        List<RecipeEntry> targets = applyFilters(group.recipes, request.filters);
        if (targets.isEmpty()) {
            send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.generate.no_match_after_filter");
            return null;
        }
        return submit(player, index.mapId, group, targets);
    }

    /** 生成收口：仓储检查 -> 矿辞替换 -> 冲突分流 -> 生成（写入浏览器通道）。 */
    private static PacketBrowseResponse submit(EntityPlayerMP player, String mapId, RuleGroup group,
        List<RecipeEntry> targets) {
        UUID uuid = player.getUniqueID();
        if (!PatternStorage.isEmpty(uuid, PatternStorage.CHANNEL_BROWSER)) {
            PatternStorage.StorageSummary existing = PatternStorage.getSummary(uuid, PatternStorage.CHANNEL_BROWSER);
            send(
                player,
                EnumChatFormatting.RED,
                "ae2patterngen.msg.generate.storage_not_empty",
                existing.count,
                existing.source);
            return null;
        }

        List<RecipeEntry> prepared = RecipeSubmissionSupport.applyReplacements(targets);
        RecipeSubmissionSupport.Conflicts conflicts = RecipeSubmissionSupport.classifyConflicts(prepared);
        if (conflicts.hasConflicts()) {
            int conflictGroupCount = conflicts.conflictGroups.size();
            if (ConflictSelectionPolicy.shouldAbortInteractiveSelection(prepared.size(), conflictGroupCount)) {
                send(
                    player,
                    EnumChatFormatting.RED,
                    "ae2patterngen.msg.generate.conflicts_too_large",
                    prepared.size(),
                    conflictGroupCount,
                    ConflictSelectionPolicy.getMaxInteractiveFilteredRecipes(),
                    ConflictSelectionPolicy.getMaxInteractiveConflictGroups());
                return null;
            }

            ConflictSession.start(uuid, mapId, conflicts.nonConflicting, conflicts.conflictGroups);
            send(
                player,
                EnumChatFormatting.YELLOW,
                "ae2patterngen.msg.generate.conflicts_detected",
                conflictGroupCount);
            ConflictResolutionService.sendCurrentBatch(player, ConflictSession.get(uuid));
            return null;
        }

        if (PatternGenerationService.generateAndStore(player, mapId, prepared, PatternStorage.CHANNEL_BROWSER)) {
            // 记住这批样板属于哪条规则：投递时要靠它给总成命名并写电路槽。
            HatchBindingRegistry.remember(uuid, mapId, group);
            send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.browser.deliver_hint");
        }
        return null;
    }

    private static PoolRuleIndex requireIndex(EntityPlayerMP player, PacketBrowseRequest request) {
        if (!RecipeCacheService.validateCache()) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.cache.missing_or_invalid");
            return null;
        }
        PoolRuleIndex index = RuleIndexService.getIndex(request.mapId);
        if (index == null) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.browser.pool_unavailable");
            return null;
        }
        return index;
    }

    private static String fingerprint(String mode, PacketBrowseRequest request, String filterKey) {
        return "browser\u001F" + mode
            + '\u001F'
            + request.mapId
            + '\u001F'
            + request.groupId
            + '\u001F'
            + request.recipeIndex
            + '\u001F'
            + filterKey;
    }

    private static String filterKey(PacketBrowseRequest.Filters filters) {
        PacketBrowseRequest.Filters safe = filters != null ? filters : PacketBrowseRequest.Filters.none();
        return safe.outputOre + '\u001F'
            + safe.inputOre
            + '\u001F'
            + safe.ncItem
            + '\u001F'
            + safe.blacklistInput
            + '\u001F'
            + safe.blacklistOutput
            + '\u001F'
            + safe.targetTier;
    }

    private static ItemStack[] collectOutputSamples(RuleGroup group) {
        Set<String> seen = new LinkedHashSet<String>();
        List<ItemStack> samples = new ArrayList<ItemStack>(MAX_OUTPUT_SAMPLES);
        for (RecipeEntry recipe : group.recipes) {
            if (recipe.outputs == null) {
                continue;
            }
            for (ItemStack output : recipe.outputs) {
                if (output == null || output.getItem() == null) {
                    continue;
                }
                String key = String.valueOf(output.getItem()) + '@' + output.getItemDamage();
                if (!seen.add(key)) {
                    continue;
                }
                ItemStack copy = output.copy();
                copy.stackSize = 1;
                samples.add(copy);
                if (samples.size() >= MAX_OUTPUT_SAMPLES) {
                    return samples.toArray(new ItemStack[samples.size()]);
                }
            }
        }
        return samples.toArray(new ItemStack[samples.size()]);
    }

    private static void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
