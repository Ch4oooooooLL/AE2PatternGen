package com.github.ae2patterngen.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按输出物品的矿辞名过滤，支持正则表达式
 */
public class OutputOreDictFilter implements IRecipeFilter {

    private final String regexPattern;
    private final Pattern compiledPattern;

    public OutputOreDictFilter(String regexPattern) {
        this.regexPattern = regexPattern;
        Pattern p = null;
        try {
            p = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            // invalid regex, fallback to literal match
            p = Pattern.compile(Pattern.quote(regexPattern), Pattern.CASE_INSENSITIVE);
        }
        this.compiledPattern = p;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (regexPattern == null || regexPattern.isEmpty() || regexPattern.equals("*")) {
            return true;
        }

        for (ItemStack output : recipe.outputs) {
            if (output == null) continue;

            int[] oreIds = OreDictionary.getOreIDs(output);
            for (int oreId : oreIds) {
                String oreName = OreDictionary.getOreName(oreId);
                if (compiledPattern.matcher(oreName)
                    .find()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "输出矿辞匹配: " + regexPattern;
    }

    public String getRegexPattern() {
        return regexPattern;
    }
}
