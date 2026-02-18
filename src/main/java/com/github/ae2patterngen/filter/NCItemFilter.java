package com.github.ae2patterngen.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 按 NC（不消耗）物品过滤
 * <p>
 * 规则更新: 使用 "ID:Meta" 格式 (如 "7511:1" 或 "7511")
 * 匹配 specialItems 或 inputs 中 stackSize == 0 的物品
 */
public class NCItemFilter implements IRecipeFilter {

    private final String matchSource;

    public NCItemFilter(String matchSource) {
        this.matchSource = matchSource;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matchSource == null || matchSource.isEmpty() || matchSource.equals("*")) {
            return true;
        }

        // 检查 specialItems
        for (ItemStack item : recipe.specialItems) {
            if (item == null) continue;
            if (checkMatch(item)) {
                return true;
            }
        }

        // 检查 inputs 中 stackSize == 0 的物品
        for (ItemStack item : recipe.inputs) {
            if (item == null) continue;
            if (item.stackSize == 0) {
                if (checkMatch(item)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkMatch(ItemStack stack) {
        Item item = stack.getItem();
        if (item == null) return false;

        int id = Item.getIdFromItem(item);
        int meta = stack.getItemDamage();

        // 支持 ":" 或 "\" 或 "/" 分隔
        String[] parts = matchSource.split("[:\\\\/]");
        try {
            int targetId = Integer.parseInt(parts[0]);
            if (id != targetId) return false;

            if (parts.length > 1) {
                int targetMeta = Integer.parseInt(parts[1]);
                return meta == targetMeta;
            }
            return true;
        } catch (NumberFormatException e) {
            // 如果不是数字，回退到字符串 ID 匹配或正则
            String registryName = Item.itemRegistry.getNameForObject(item);
            if (registryName == null) return false;

            try {
                return Pattern.compile(matchSource, Pattern.CASE_INSENSITIVE)
                    .matcher(registryName + ":" + meta)
                    .find();
            } catch (PatternSyntaxException e2) {
                return (registryName + ":" + meta).contains(matchSource);
            }
        }
    }

    @Override
    public String getDescription() {
        return "NC 物品匹配: " + matchSource;
    }

    public String getRegexPattern() {
        return matchSource;
    }
}
