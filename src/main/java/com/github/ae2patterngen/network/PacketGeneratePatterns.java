package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.OreDictReplacer;
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.RecipeFilterFactory;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.storage.CacheQueryResult;
import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.storage.RecipeCacheService;
import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端: 请求生成样板
 * <p>
 * v1.3: 支持矿辞替换规则
 */
public class PacketGeneratePatterns implements IMessage {

    private String recipeMapId;
    private String outputOreDict;
    private String inputOreDict;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;
    private String replacements;
    private int targetTier;

    public PacketGeneratePatterns() {}

    public PacketGeneratePatterns(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput, String replacements, int targetTier) {
        this.recipeMapId = recipeMapId;
        this.outputOreDict = outputOreDict;
        this.inputOreDict = inputOreDict;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
        this.replacements = replacements;
        this.targetTier = targetTier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        outputOreDict = ByteBufUtils.readUTF8String(buf);
        inputOreDict = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
        replacements = ByteBufUtils.readUTF8String(buf);
        targetTier = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMapId != null ? recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, outputOreDict != null ? outputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, inputOreDict != null ? inputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
        ByteBufUtils.writeUTF8String(buf, replacements != null ? replacements : "");
        buf.writeInt(targetTier);
    }

    public static class Handler implements IMessageHandler<PacketGeneratePatterns, IMessage> {

        @Override
        public IMessage onMessage(PacketGeneratePatterns message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            try {
                String requestFingerprint = PatternGenerationRequestGate.fingerprint(
                    message.recipeMapId,
                    message.outputOreDict,
                    message.inputOreDict,
                    message.ncItem,
                    message.blacklistInput,
                    message.blacklistOutput,
                    message.replacements,
                    message.targetTier);
                if (!PatternGenerationRequestGate.shouldProcess(uuid, requestFingerprint, System.currentTimeMillis())) {
                    return null;
                }

                // 检查仓储是否有残留样板
                if (!PatternStorage.isEmpty(uuid)) {
                    PatternStorage.StorageSummary existing = PatternStorage.getSummary(uuid);
                    send(
                        player,
                        EnumChatFormatting.RED,
                        "ae2patterngen.msg.generate.storage_not_empty",
                        existing.count,
                        existing.source);
                    return null;
                }

                CompositeFilter filter = RecipeFilterFactory.build(
                    message.outputOreDict,
                    message.inputOreDict,
                    message.ncItem,
                    message.blacklistInput,
                    message.blacklistOutput,
                    message.targetTier);
                CacheQueryResult queryResult = RecipeCacheService.loadAndFilterRecipes(message.recipeMapId, filter);
                if (!queryResult.cacheValid) {
                    send(player, EnumChatFormatting.RED, "ae2patterngen.msg.cache.missing_or_invalid");
                    return null;
                }

                // 1. 查找匹配的配方表
                List<String> matchedMaps = queryResult.matchedMapIds;
                if (matchedMaps.isEmpty()) {
                    send(
                        player,
                        EnumChatFormatting.RED,
                        "ae2patterngen.msg.generate.no_matching_map",
                        message.recipeMapId);
                    return null;
                }

                send(
                    player,
                    EnumChatFormatting.GRAY,
                    "ae2patterngen.msg.generate.matched_maps",
                    String.join(", ", matchedMaps));

                // 2. 使用缓存查询结果
                List<RecipeEntry> filtered = new java.util.ArrayList<>(queryResult.recipes);

                send(
                    player,
                    EnumChatFormatting.GRAY,
                    "ae2patterngen.msg.generate.filter_result",
                    queryResult.totalLoadedCount,
                    queryResult.totalFilteredCount);

                if (filtered.isEmpty()) {
                    send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.generate.no_match_after_filter");
                    return null;
                }

                // 5. 应用矿辞替换 (从服务端配置读取)
                OreDictReplacer replacer = new OreDictReplacer(
                    com.github.ae2patterngen.config.ReplacementConfig.getRulesString());
                if (replacer.hasRules()) {
                    filtered = RecipeSubmissionSupport.applyReplacements(filtered);
                    send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.generate.replacement_applied");
                }

                // 6. 配方冲突检测与分组 (按产物显示名称)
                RecipeSubmissionSupport.Conflicts conflicts = RecipeSubmissionSupport.classifyConflicts(filtered);
                if (conflicts.hasConflicts()) {
                    int conflictGroupCount = conflicts.conflictGroups.size();
                    if (ConflictSelectionPolicy.shouldAbortInteractiveSelection(filtered.size(), conflictGroupCount)) {
                        send(
                            player,
                            EnumChatFormatting.RED,
                            "ae2patterngen.msg.generate.conflicts_too_large",
                            filtered.size(),
                            conflictGroupCount,
                            ConflictSelectionPolicy.getMaxInteractiveFilteredRecipes(),
                            ConflictSelectionPolicy.getMaxInteractiveConflictGroups());
                        return null;
                    }

                    // 开启冲突处理会话
                    ConflictSession
                        .start(uuid, message.recipeMapId, conflicts.nonConflicting, conflicts.conflictGroups);
                    send(
                        player,
                        EnumChatFormatting.YELLOW,
                        "ae2patterngen.msg.generate.conflicts_detected",
                        conflictGroupCount);
                    ConflictResolutionService.sendCurrentBatch(player, ConflictSession.get(uuid));
                    return null;
                }

                PatternGenerationService.generateAndStore(player, message.recipeMapId, filtered);
            } catch (RuntimeException e) {
                cpw.mods.fml.common.FMLLog.severe(
                    "[AE2PatternGen] Generation request failed for player %s: %s",
                    player != null ? player.getCommandSenderName() : "unknown",
                    e.getMessage());
                send(player, EnumChatFormatting.RED, "ae2patterngen.msg.generate.internal_error");
            }
            return null;
        }

        private void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
            player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
        }
    }
}
