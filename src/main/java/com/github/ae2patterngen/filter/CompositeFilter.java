package com.github.ae2patterngen.filter;

import java.util.ArrayList;
import java.util.List;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 组合过滤器 — 所有子过滤器必须同时满足 (AND)
 */
public class CompositeFilter implements IRecipeFilter {

    private final List<IRecipeFilter> filters = new ArrayList<>();

    public CompositeFilter() {}

    public void addFilter(IRecipeFilter filter) {
        if (filter != null) {
            filters.add(filter);
        }
    }

    public void clearFilters() {
        filters.clear();
    }

    public List<IRecipeFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        for (IRecipeFilter filter : filters) {
            if (!filter.matches(recipe)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        if (filters.isEmpty()) return "无过滤条件";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(
                filters.get(i)
                    .getDescription());
        }
        return sb.toString();
    }
}
