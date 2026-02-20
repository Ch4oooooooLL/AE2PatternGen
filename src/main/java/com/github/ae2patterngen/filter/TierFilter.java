package com.github.ae2patterngen.filter;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 电压等级过滤器
 */
public class TierFilter implements IRecipeFilter {

    private final int targetTier;

    /**
     * @param targetTier 目标电压等级 (-1=Any, 0=ULV, 1=LV...)
     */
    public TierFilter(int targetTier) {
        this.targetTier = targetTier;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (targetTier < 0) return true; // Any

        // 仅保留完全匹配所选电压等级的配方，避免重复生成低等级或无法处理高等级
        return getTier(recipe.euPerTick) == targetTier;
    }

    @Override
    public String getDescription() {
        return "Tier=" + targetTier;
    }

    private byte getTier(long euPerTick) {
        long eu = euPerTick;
        byte tier = 0;
        while (eu > 8) {
            eu >>= 2;
            tier++;
        }
        return tier;
    }
}
