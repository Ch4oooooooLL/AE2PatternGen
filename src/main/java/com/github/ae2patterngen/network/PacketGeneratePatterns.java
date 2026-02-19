package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.PatternEncoder;
import com.github.ae2patterngen.filter.BlacklistFilter;
import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.InputOreDictFilter;
import com.github.ae2patterngen.filter.NCItemFilter;
import com.github.ae2patterngen.filter.OutputOreDictFilter;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.storage.PatternStorage;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端 -> 服务端: 请求生成样板
 * <p>
 * v1.2: 生成结果写入虚拟仓储文件，不再直接放入背包
 */
public class PacketGeneratePatterns implements IMessage {

    private String recipeMapId;
    private String outputOreDict;
    private String inputOreDict;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;

    public PacketGeneratePatterns() {}

    public PacketGeneratePatterns(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput) {
        this.recipeMapId = recipeMapId;
        this.outputOreDict = outputOreDict;
        this.inputOreDict = inputOreDict;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        outputOreDict = ByteBufUtils.readUTF8String(buf);
        inputOreDict = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMapId != null ? recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, outputOreDict != null ? outputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, inputOreDict != null ? inputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
    }

    public static class Handler implements IMessageHandler<PacketGeneratePatterns, IMessage> {

        @Override
        public IMessage onMessage(PacketGeneratePatterns message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();

            // 检查仓储是否有残留样板
            if (!PatternStorage.isEmpty(uuid)) {
                PatternStorage.StorageSummary existing = PatternStorage.getSummary(uuid);
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "[AE2PatternGen] 仓储中仍有 "
                            + existing.count
                            + " 个未清空的样板 (来源: "
                            + existing.source
                            + ")。请先蹲下右键查看并取出/清空后再生成新样板。"));
                return null;
            }

            // 1. 查找匹配的配方表
            List<String> matchedMaps = GTRecipeSource.findMatchingRecipeMaps(message.recipeMapId);
            if (matchedMaps.isEmpty()) {
                player.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "[AE2PatternGen] 未找到匹配的配方表: " + message.recipeMapId));
                return null;
            }

            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "[AE2PatternGen] 匹配到配方表: " + String.join(", ", matchedMaps)));

            // 2. 收集原始配方
            List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(message.recipeMapId);
            int totalBeforeFilter = recipes.size();

            // 3. 构建过滤器
            CompositeFilter filter = new CompositeFilter();
            if (message.outputOreDict != null && !message.outputOreDict.isEmpty()
                && !message.outputOreDict.equals("*")) {
                filter.addFilter(new OutputOreDictFilter(message.outputOreDict));
            }
            if (message.inputOreDict != null && !message.inputOreDict.isEmpty() && !message.inputOreDict.equals("*")) {
                filter.addFilter(new InputOreDictFilter(message.inputOreDict));
            }
            if (message.ncItem != null && !message.ncItem.isEmpty() && !message.ncItem.equals("*")) {
                filter.addFilter(new NCItemFilter(message.ncItem));
            }
            if (message.blacklistInput != null && !message.blacklistInput.isEmpty()
                && !message.blacklistInput.equals("*")) {
                filter.addFilter(new BlacklistFilter(message.blacklistInput, true, false));
            }
            if (message.blacklistOutput != null && !message.blacklistOutput.isEmpty()
                && !message.blacklistOutput.equals("*")) {
                filter.addFilter(new BlacklistFilter(message.blacklistOutput, false, true));
            }

            // 4. 应用过滤
            List<RecipeEntry> filtered = new java.util.ArrayList<>();
            for (RecipeEntry recipe : recipes) {
                if (filter.matches(recipe)) {
                    filtered.add(recipe);
                }
            }

            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GRAY + "[AE2PatternGen] 原始配方: "
                        + totalBeforeFilter
                        + ", 过滤后: "
                        + filtered.size()));

            if (filtered.isEmpty()) {
                player.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 没有找到匹配的配方 (请检查正则/矿辞)"));
                return null;
            }

            // 5. 编码样板
            List<ItemStack> patterns = PatternEncoder.encodeBatch(filtered);

            if (patterns.isEmpty()) {
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 编码后无有效样板"));
                return null;
            }

            // 6. 写入虚拟仓储
            PatternStorage.save(uuid, patterns, message.recipeMapId);

            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "[AE2PatternGen] 已生成 "
                        + patterns.size()
                        + " 个样板并存入仓储! 蹲下右键空气查看，蹲下右键容器导出。"));

            return null;
        }
    }
}
