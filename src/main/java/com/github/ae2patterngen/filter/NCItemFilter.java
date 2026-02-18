package com.github.ae2patterngen.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按 NC（不消耗）物品 ID 过滤
 * 匹配 specialItems 或 mInputs 中 stackSize == 0 的物品
 */
public class NCItemFilter implements IRecipeFilter {

    private final String regexPattern;
    private final Pattern compiledPattern;

    public NCItemFilter(String regexPattern) {
        this.regexPattern = regexPattern;
        Pattern p = null;
        try {
            p = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            p = Pattern.compile(Pattern.quote(regexPattern), Pattern.CASE_INSENSITIVE);
        }
        this.compiledPattern = p;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (regexPattern == null || regexPattern.isEmpty()) {
            return true;
        }

        // 检查 specialItems
        for (ItemStack item : recipe.specialItems) {
            if (item == null) continue;
            String itemId = getItemIdentifier(item);
            if (compiledPattern.matcher(itemId)
                .find()) {
                return true;
            }
        }

        // 检查 inputs 中 stackSize == 0 的物品 (GT 标记 NC 的方式之一)
        for (ItemStack item : recipe.inputs) {
            if (item == null) continue;
            if (item.stackSize == 0) {
                String itemId = getItemIdentifier(item);
                if (compiledPattern.matcher(itemId)
                    .find()) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getItemIdentifier(ItemStack stack) {
        Item item = stack.getItem();
        if (item == null) return "";
        String registryName = Item.itemRegistry.getNameForObject(item);
        if (registryName == null) return "";
        if (stack.getItemDamage() != 0) {
            return registryName + ":" + stack.getItemDamage();
        }
        return registryName;
    }

    @Override
    public String getDescription() {
        return "NC 物品匹配: " + regexPattern;
    }

    public String getRegexPattern() {
        return regexPattern;
    }
}
