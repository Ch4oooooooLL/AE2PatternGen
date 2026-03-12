package com.github.ae2patterngen.gui;

import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.oredict.OreDictionary;

import com.github.ae2patterngen.util.ItemStackUtil;
import com.github.ae2patterngen.util.OreDictUtil;

final class ExplicitFilterDropFormatter {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    private ExplicitFilterDropFormatter() {}

    static String format(ItemStack stack) {
        if (stack == null) {
            return "";
        }

        Item item = stack.getItem();
        int itemId = item != null ? Item.getIdFromItem(item) : -1;
        Integer meta = shouldIncludeMeta(stack) ? Integer.valueOf(stack.getItemDamage()) : null;

        return format(itemId, meta, OreDictUtil.getOreNamesSafe(stack), ItemStackUtil.getSafeDisplayName(stack));
    }

    static String format(int itemId, Integer meta, String[] oreNames, String displayName) {
        if (itemId >= 0) {
            if (meta != null) {
                return "[" + itemId + ":" + meta.intValue() + "]";
            }
            return "[" + itemId + "]";
        }

        String oreName = firstNonBlank(oreNames);
        if (!oreName.isEmpty()) {
            return "(" + escapeRegexToken(oreName) + ")";
        }

        String safeDisplayName = sanitize(displayName);
        if (!safeDisplayName.isEmpty()) {
            return "{" + escapeRegexToken(safeDisplayName) + "}";
        }

        return "";
    }

    private static boolean shouldIncludeMeta(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Item item = stack.getItem();
        if (item == null) {
            return false;
        }

        int meta = stack.getItemDamage();
        if (meta == OreDictionary.WILDCARD_VALUE) {
            return false;
        }

        return item.getHasSubtypes();
    }

    private static String firstNonBlank(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }

        for (String value : values) {
            String sanitized = sanitize(value);
            if (!sanitized.isEmpty()) {
                return sanitized;
            }
        }

        return "";
    }

    private static String escapeRegexToken(String text) {
        String sanitized = sanitize(text);
        if (sanitized.isEmpty()) {
            return "";
        }

        return SPECIAL_REGEX_CHARS.matcher(sanitized)
            .replaceAll("\\\\$0");
    }

    private static String sanitize(String text) {
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text != null ? text : "");
        return stripped != null ? stripped.trim() : "";
    }
}
