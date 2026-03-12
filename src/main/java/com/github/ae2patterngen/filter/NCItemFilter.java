package com.github.ae2patterngen.filter;

import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按 NC（不消耗）物品的统一显式筛选语法进行匹配。
 */
public class NCItemFilter implements IRecipeFilter {

    private final String matchSource;
    private final ExplicitStackMatcher matcher;

    public NCItemFilter(String matchSource) {
        this(matchSource, new ExplicitStackMatcher.StackMatchCache());
    }

    NCItemFilter(String matchSource, ExplicitStackMatcher.StackMatchCache stackMatchCache) {
        this.matchSource = matchSource;
        this.matcher = new ExplicitStackMatcher(matchSource, stackMatchCache);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        for (ItemStack item : recipe.specialItems) {
            if (item != null && matcher.matches(item)) {
                return true;
            }
        }

        for (ItemStack item : recipe.inputs) {
            if (item != null && item.stackSize == 0 && matcher.matches(item)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "NC 筛选: " + matchSource;
    }

    public String getRegexPattern() {
        return matchSource;
    }
}
