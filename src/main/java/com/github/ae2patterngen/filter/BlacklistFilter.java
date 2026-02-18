package com.github.ae2patterngen.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 黑名单过滤器 — 如果配方包含匹配项，则拒绝
 */
public class BlacklistFilter implements IRecipeFilter {

    private final String keyword;
    private final Pattern compiledPattern;
    private final boolean checkInputs;
    private final boolean checkOutputs;

    public BlacklistFilter(String keyword, boolean checkInputs, boolean checkOutputs) {
        this.keyword = keyword;
        this.checkInputs = checkInputs;
        this.checkOutputs = checkOutputs;
        Pattern p = null;
        try {
            p = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            p = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        }
        this.compiledPattern = p;
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

        // 1. 匹配名称
        String displayName = stack.getDisplayName();
        if (compiledPattern.matcher(displayName)
            .find()) return true;

        // 2. 匹配矿辞
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int oreId : oreIds) {
            String oreName = OreDictionary.getOreName(oreId);
            if (compiledPattern.matcher(oreName)
                .find()) return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "黑名单(" + (checkInputs ? "入" : "") + (checkOutputs ? "出" : "") + "): " + keyword;
    }
}
