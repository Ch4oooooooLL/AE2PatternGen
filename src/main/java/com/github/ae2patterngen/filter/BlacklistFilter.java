package com.github.ae2patterngen.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.OreDictUtil;

/**
 * 黑名单过滤器 — 如果配方包含匹配项，则拒绝
 */
public class BlacklistFilter implements IRecipeFilter {

    private static final Pattern ID_TOKEN_PATTERN = Pattern.compile("\\[(\\d+)(?::(\\d+))?\\]");

    private final String keyword;
    private final Pattern compiledPattern;
    private final boolean checkInputs;
    private final boolean checkOutputs;

    public BlacklistFilter(String keyword, boolean checkInputs, boolean checkOutputs) {
        this.keyword = keyword;
        this.checkInputs = checkInputs;
        this.checkOutputs = checkOutputs;
        this.compiledPattern = compileKeyword(keyword);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (keyword == null || keyword.isEmpty() || keyword.equals("*")) {
            return true;
        }

        // 检查输入
        if (checkInputs) {
            for (ItemStack input : recipe.inputs) {
                if (isMatch(input)) return false; // 命中黑名单，排除
            }
        }

        // 检查输出
        if (checkOutputs) {
            for (ItemStack output : recipe.outputs) {
                if (isMatch(output)) return false; // 命中黑名单，排除
            }
        }

        return true;
    }

    private boolean isMatch(ItemStack stack) {
        if (stack == null) return false;

        String displayName = stack.getDisplayName();
        String[] oreNames = getOreNames(stack);

        int itemId = -1;
        int meta = -1;
        Item item = stack.getItem();
        if (item != null) {
            itemId = Item.getIdFromItem(item);
            meta = stack.getItemDamage();
        }

        return matchesCompiledPattern(compiledPattern, displayName, itemId, meta, oreNames);
    }

    static Pattern compileKeyword(String keyword) {
        String source = keyword != null ? keyword : "";
        String regex = escapeIdTokens(source);
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return Pattern.compile(Pattern.quote(source), Pattern.CASE_INSENSITIVE);
        }
    }

    static boolean matchesCompiledPattern(Pattern pattern, String displayName, int itemId, int meta,
        String[] oreNames) {
        if (pattern == null) return false;

        StringBuilder searchTarget = new StringBuilder();
        appendPart(searchTarget, displayName);

        if (oreNames != null) {
            for (String oreName : oreNames) {
                appendPart(searchTarget, oreName);
            }
        }

        if (itemId >= 0) {
            appendPart(searchTarget, "[" + itemId + "]");
            if (meta >= 0) {
                appendPart(searchTarget, "[" + itemId + ":" + meta + "]");
            }
        }

        if (searchTarget.length() == 0) return false;
        return pattern.matcher(searchTarget.toString())
            .find();
    }

    private static String escapeIdTokens(String source) {
        Matcher matcher = ID_TOKEN_PATTERN.matcher(source);
        StringBuffer escaped = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group();
            String quoted = Pattern.quote(token);
            matcher.appendReplacement(escaped, Matcher.quoteReplacement(quoted));
        }
        matcher.appendTail(escaped);
        return escaped.toString();
    }

    private static String[] getOreNames(ItemStack stack) {
        return OreDictUtil.getOreNamesSafe(stack);
    }

    private static void appendPart(StringBuilder builder, String value) {
        if (value == null || value.isEmpty()) return;
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(value);
    }

    @Override
    public String getDescription() {
        return "黑名单(" + (checkInputs ? "入" : "") + (checkOutputs ? "出" : "") + "): " + keyword;
    }
}
