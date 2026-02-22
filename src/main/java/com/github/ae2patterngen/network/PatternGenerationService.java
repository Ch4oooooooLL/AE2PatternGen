package com.github.ae2patterngen.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.encoder.PatternEncoder;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.storage.PatternStorage;
import com.github.ae2patterngen.util.AE2Util;
import com.github.ae2patterngen.util.InventoryUtil;

/**
 * 统一封装最终生成流程: 编码 -> 扣除空白样板 -> 写入虚拟仓储。
 */
public final class PatternGenerationService {

    private PatternGenerationService() {}

    public static boolean generateAndStore(EntityPlayerMP player, String source, List<RecipeEntry> recipes) {
        if (player == null || recipes == null || recipes.isEmpty()) {
            return false;
        }

        List<ItemStack> patterns = PatternEncoder.encodeBatch(recipes);
        if (patterns.isEmpty()) {
            player.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + "[AE2PatternGen] 编码后无有效样板"));
            return false;
        }

        int requiredCount = patterns.size();
        ItemStack blankPattern = InventoryUtil.getBlankPattern();
        if (!consumeBlankPatterns(player, requiredCount, blankPattern)) {
            return false;
        }

        UUID uuid = player.getUniqueID();
        PatternStorage.save(uuid, patterns, source);
        player.addChatMessage(
            new ChatComponentText(
                EnumChatFormatting.GREEN + "[AE2PatternGen] 已扣除 " + requiredCount + " 个空白样板并生成了等量成品。"));
        player.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "成品已存入仓储! 蹲下右键空气查看，蹲下右键容器导出。"));
        return true;
    }

    private static boolean consumeBlankPatterns(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
        boolean consumed = AE2Util.tryWirelessConsume(player, requiredCount, blankPattern);
        if (consumed) {
            return true;
        }

        if (!InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = InventoryUtil.countItem(player, blankPattern);
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "[AE2PatternGen] 生成失败: 空白样板不足。需要 "
                        + requiredCount
                        + " 但只有 "
                        + currentHas));
            return false;
        }

        return true;
    }
}
