package com.github.ae2patterngen.filter;

import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 黑名单过滤器 — 如果配方包含匹配项，则拒绝。
 */
public class BlacklistFilter implements IRecipeFilter {

    private final String keyword;
    private final boolean checkInputs;
    private final boolean checkOutputs;
    private final ExplicitStackMatcher matcher;

    public BlacklistFilter(String keyword, boolean checkInputs, boolean checkOutputs) {
        this.keyword = keyword;
        this.checkInputs = checkInputs;
        this.checkOutputs = checkOutputs;
        this.matcher = new ExplicitStackMatcher(keyword);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        if (checkInputs && containsMatch(recipe.inputs)) {
            return false;
        }

        if (checkOutputs && containsMatch(recipe.outputs)) {
            return false;
        }

        return true;
    }

    private boolean containsMatch(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return false;
        }

        for (ItemStack stack : stacks) {
            if (matcher.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "黑名单(" + (checkInputs ? "入" : "") + (checkOutputs ? "出" : "") + "): " + keyword;
    }
}
