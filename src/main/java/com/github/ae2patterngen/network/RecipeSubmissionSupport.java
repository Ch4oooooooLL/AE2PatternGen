package com.github.ae2patterngen.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.ae2patterngen.config.ReplacementConfig;
import com.github.ae2patterngen.encoder.OreDictReplacer;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;
import com.github.ae2patterngen.util.ItemStackUtil;

/**
 * 生成前的统一准备逻辑：矿辞替换与产物冲突归类。
 * <p>
 * 供"按过滤条件生成"与"按规则组生成"两条入口复用，保证两条路径行为一致。
 */
public final class RecipeSubmissionSupport {

    private RecipeSubmissionSupport() {}

    /**
     * 按服务端配置里的替换规则统一材料。
     *
     * @return 应用替换后的配方列表；无规则时返回原列表。
     */
    public static List<RecipeEntry> applyReplacements(List<RecipeEntry> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        OreDictReplacer replacer = new OreDictReplacer(ReplacementConfig.getRulesString());
        if (!replacer.hasRules()) {
            return recipes;
        }

        List<RecipeEntry> replaced = new ArrayList<RecipeEntry>(recipes.size());
        for (RecipeEntry recipe : recipes) {
            replaced.add(
                new RecipeEntry(
                    recipe.sourceType,
                    recipe.recipeMapId,
                    recipe.machineDisplayName,
                    replacer.apply(recipe.inputs),
                    replacer.apply(recipe.outputs),
                    recipe.fluidInputs,
                    recipe.fluidOutputs,
                    recipe.specialItems,
                    recipe.duration,
                    recipe.euPerTick));
        }
        return replaced;
    }

    /**
     * 按产物显示名称归类，区分"无冲突配方"与"有冲突配方组"。
     */
    public static Conflicts classifyConflicts(List<RecipeEntry> recipes) {
        Map<String, List<RecipeEntry>> groups = new LinkedHashMap<String, List<RecipeEntry>>();
        if (recipes != null) {
            for (RecipeEntry recipe : recipes) {
                groups.computeIfAbsent(productKeyOf(recipe), key -> new ArrayList<RecipeEntry>())
                    .add(recipe);
            }
        }

        List<RecipeEntry> nonConflicting = new ArrayList<RecipeEntry>();
        Map<String, List<RecipeEntry>> conflictGroups = new LinkedHashMap<String, List<RecipeEntry>>();
        for (Map.Entry<String, List<RecipeEntry>> entry : groups.entrySet()) {
            if (entry.getValue()
                .size() > 1) {
                conflictGroups.put(entry.getKey(), entry.getValue());
            } else {
                nonConflicting.addAll(entry.getValue());
            }
        }
        return new Conflicts(nonConflicting, conflictGroups);
    }

    private static String productKeyOf(RecipeEntry recipe) {
        if (recipe.outputs != null && recipe.outputs.length > 0 && recipe.outputs[0] != null) {
            return ItemStackUtil.getSafeDisplayName(recipe.outputs[0]);
        }
        if (recipe.fluidOutputs != null && recipe.fluidOutputs.length > 0 && recipe.fluidOutputs[0] != null) {
            return recipe.fluidOutputs[0].getLocalizedName();
        }
        return I18nUtil.tr("ae2patterngen.msg.common.unknown_item");
    }

    /** 冲突归类结果。 */
    public static final class Conflicts {

        public final List<RecipeEntry> nonConflicting;
        public final Map<String, List<RecipeEntry>> conflictGroups;

        Conflicts(List<RecipeEntry> nonConflicting, Map<String, List<RecipeEntry>> conflictGroups) {
            this.nonConflicting = nonConflicting;
            this.conflictGroups = conflictGroups;
        }

        public boolean hasConflicts() {
            return !conflictGroups.isEmpty();
        }
    }
}
