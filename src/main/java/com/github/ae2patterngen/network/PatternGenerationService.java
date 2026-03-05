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
import com.github.ae2patterngen.util.I18nUtil;
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
            send(player, EnumChatFormatting.YELLOW, "ae2patterngen.msg.pattern.no_valid_after_encode");
            return false;
        }

        int requiredCount = patterns.size();
        ItemStack blankPattern = InventoryUtil.getBlankPattern();
        if (!consumeBlankPatterns(player, requiredCount, blankPattern)) {
            return false;
        }

        UUID uuid = player.getUniqueID();
        if (!PatternStorage.save(uuid, patterns, source)) {
            send(player, EnumChatFormatting.RED, "ae2patterngen.msg.pattern.storage_write_failed");
            return false;
        }
        send(player, EnumChatFormatting.GREEN, "ae2patterngen.msg.pattern.generated_and_consumed", requiredCount);
        send(player, EnumChatFormatting.GRAY, "ae2patterngen.msg.pattern.stored_hint");
        return true;
    }

    private static boolean consumeBlankPatterns(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
        boolean consumed = AE2Util.tryWirelessConsume(player, requiredCount, blankPattern);
        if (consumed) {
            return true;
        }

        if (!InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = InventoryUtil.countItem(player, blankPattern);
            send(
                player,
                EnumChatFormatting.RED,
                "ae2patterngen.msg.pattern.insufficient_blank_pattern",
                requiredCount,
                currentHas);
            return false;
        }

        return true;
    }

    private static void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
