package com.github.ae2patterngen.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        return buildChoices(itemId, meta, oreNames, displayName).getDefaultToken();
    }

    static DropChoices buildChoices(ItemStack stack) {
        if (stack == null) {
            return DropChoices.empty();
        }

        Item item = stack.getItem();
        int itemId = item != null ? Item.getIdFromItem(item) : -1;
        Integer meta = shouldIncludeMeta(stack) ? Integer.valueOf(stack.getItemDamage()) : null;
        return buildChoices(itemId, meta, OreDictUtil.getOreNamesSafe(stack), ItemStackUtil.getSafeDisplayName(stack));
    }

    static DropChoices buildChoices(int itemId, Integer meta, String[] oreNames, String displayName) {
        List<DropChoice> options = new ArrayList<DropChoice>();

        if (itemId >= 0) {
            if (meta != null) {
                options.add(new DropChoice(DropChoiceSource.ITEM_ID, "[" + itemId + ":" + meta.intValue() + "]"));
            } else {
                options.add(new DropChoice(DropChoiceSource.ITEM_ID, "[" + itemId + "]"));
            }
        }

        String oreName = firstNonBlank(oreNames);
        if (!oreName.isEmpty()) {
            options.add(new DropChoice(DropChoiceSource.ORE_DICT, "(" + escapeRegexToken(oreName) + ")"));
        }

        String safeDisplayName = sanitize(displayName);
        if (!safeDisplayName.isEmpty()) {
            options.add(new DropChoice(DropChoiceSource.DISPLAY_NAME, "{" + escapeRegexToken(safeDisplayName) + "}"));
        }

        return options.isEmpty() ? DropChoices.empty() : new DropChoices(options, 0);
    }

    static DropChoices singleChoice(String token) {
        String safeToken = sanitize(token);
        if (safeToken.isEmpty()) {
            return DropChoices.empty();
        }
        return new DropChoices(Collections.singletonList(new DropChoice(DropChoiceSource.CUSTOM, safeToken)), 0);
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

    enum DropChoiceSource {
        ITEM_ID,
        ORE_DICT,
        DISPLAY_NAME,
        CUSTOM
    }

    static final class DropChoice {

        private final DropChoiceSource source;
        private final String token;

        private DropChoice(DropChoiceSource source, String token) {
            this.source = source;
            this.token = token;
        }

        DropChoiceSource getSource() {
            return source;
        }

        String getToken() {
            return token;
        }
    }

    static final class DropChoices {

        private static final DropChoices EMPTY = new DropChoices(Collections.<DropChoice>emptyList(), -1);

        private final List<DropChoice> options;
        private final int defaultIndex;

        private DropChoices(List<DropChoice> options, int defaultIndex) {
            this.options = Collections.unmodifiableList(new ArrayList<DropChoice>(options));
            this.defaultIndex = defaultIndex;
        }

        static DropChoices empty() {
            return EMPTY;
        }

        List<DropChoice> getOptions() {
            return options;
        }

        int getDefaultIndex() {
            return defaultIndex;
        }

        boolean isEmpty() {
            return options.isEmpty();
        }

        int size() {
            return options.size();
        }

        String getDefaultToken() {
            DropChoice choice = getDefaultChoice();
            return choice != null ? choice.getToken() : "";
        }

        DropChoice getDefaultChoice() {
            if (defaultIndex < 0 || defaultIndex >= options.size()) {
                return null;
            }
            return options.get(defaultIndex);
        }
    }
}
