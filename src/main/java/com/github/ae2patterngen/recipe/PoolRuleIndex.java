package com.github.ae2patterngen.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单个配方池（一个 GT 配方表）的规则分组索引。
 * <p>
 * 分组结果按配方数量降序排列；默认视图会隐藏"只有 1 条配方的规则组"以及
 * "既不要求编程电路、也没有 NC 物品"的默认组，两者都可通过开关展开。
 */
public final class PoolRuleIndex {

    public final String mapId;

    /** 池内参与分组的配方总数。 */
    public final int totalRecipeCount;

    /** 全部规则组，已按 (配方数降序, 电路编号升序, groupId 升序) 排序。 */
    public final List<RuleGroup> groups;

    private final int singletonRuleCount;
    private final int unruledRecipeCount;

    PoolRuleIndex(String mapId, int totalRecipeCount, List<RuleGroup> groups) {
        this.mapId = mapId != null ? mapId : "";
        this.totalRecipeCount = Math.max(0, totalRecipeCount);
        this.groups = Collections.unmodifiableList(new ArrayList<RuleGroup>(groups));

        int singletons = 0;
        int unruledRecipes = 0;
        for (RuleGroup group : this.groups) {
            if (group.unruled) {
                unruledRecipes += group.size();
                continue;
            }
            if (group.isSingleton()) {
                singletons++;
            }
        }
        this.singletonRuleCount = singletons;
        this.unruledRecipeCount = unruledRecipes;
    }

    /** 只有 1 条配方的规则组数量（默认折叠不展示）。 */
    public int getSingletonRuleCount() {
        return singletonRuleCount;
    }

    /** "无电路 + 无 NC" 组覆盖的配方数量（默认隐藏）。 */
    public int getUnruledRecipeCount() {
        return unruledRecipeCount;
    }

    /** 按当前开关过滤后、真正需要展示给用户的规则组。 */
    public List<RuleGroup> visibleGroups(boolean showSingletonRules, boolean showUnruledGroup) {
        List<RuleGroup> visible = new ArrayList<RuleGroup>();
        for (RuleGroup group : groups) {
            if (group.unruled) {
                if (showUnruledGroup) {
                    visible.add(group);
                }
                continue;
            }
            if (group.isSingleton() && !showSingletonRules) {
                continue;
            }
            visible.add(group);
        }
        return visible;
    }

    public RuleGroup findById(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return null;
        }
        for (RuleGroup group : groups) {
            if (group.groupId.equals(groupId)) {
                return group;
            }
        }
        return null;
    }
}
