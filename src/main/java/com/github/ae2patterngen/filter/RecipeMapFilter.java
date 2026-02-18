package com.github.ae2patterngen.filter;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按配方表 ID (机器类型) 过滤
 */
public class RecipeMapFilter implements IRecipeFilter {

    private final String targetRecipeMapId;

    public RecipeMapFilter(String targetRecipeMapId) {
        this.targetRecipeMapId = targetRecipeMapId;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (targetRecipeMapId == null || targetRecipeMapId.isEmpty()) {
            return true; // 空过滤 = 不过滤
        }
        return targetRecipeMapId.equals(recipe.recipeMapId);
    }

    @Override
    public String getDescription() {
        return "配方表: " + (targetRecipeMapId != null ? targetRecipeMapId : "全部");
    }

    public String getTargetRecipeMapId() {
        return targetRecipeMapId;
    }
}
