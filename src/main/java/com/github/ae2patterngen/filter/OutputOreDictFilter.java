package com.github.ae2patterngen.filter;

import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按输出物品的统一显式筛选语法进行匹配。
 */
public class OutputOreDictFilter implements IRecipeFilter {

    private final String matchSource;
    private final ExplicitStackMatcher matcher;

    public OutputOreDictFilter(String matchSource) {
        this.matchSource = matchSource;
        this.matcher = new ExplicitStackMatcher(matchSource);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        for (ItemStack output : recipe.outputs) {
            if (output != null && matcher.matches(output)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "输出筛选: " + matchSource;
    }

    public String getRegexPattern() {
        return matchSource;
    }
}
