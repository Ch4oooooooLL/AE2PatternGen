package com.github.ae2patterngen.filter;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 配方过滤器接口
 */
public interface IRecipeFilter {

    /**
     * 测试配方是否满足过滤条件
     *
     * @param recipe 配方
     * @return true 表示保留该配方
     */
    boolean matches(RecipeEntry recipe);

    /**
     * 过滤器描述（用于 GUI 显示）
     */
    String getDescription();
}
