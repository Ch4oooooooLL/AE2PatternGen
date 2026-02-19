package com.github.ae2patterngen.encoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 矿辞替换器 — 在编码前替换 ItemStack 中的物品
 * <p>
 * 规则格式: {@code 源矿辞=目标矿辞} (多条用 {@code ;} 分隔)
 * <p>
 * 例: {@code ingotCopper=dustCopper;ingotTin=dustTin}
 */
public class OreDictReplacer {

    private final Map<String, String> rules;

    public OreDictReplacer(String rulesStr) {
        this.rules = new LinkedHashMap<>();
        if (rulesStr == null || rulesStr.trim()
            .isEmpty()) return;

        for (String rule : rulesStr.split(";")) {
            rule = rule.trim();
            if (rule.isEmpty()) continue;
            int eq = rule.indexOf('=');
            if (eq <= 0 || eq >= rule.length() - 1) continue;
            String src = rule.substring(0, eq)
                .trim();
            String dst = rule.substring(eq + 1)
                .trim();
            if (!src.isEmpty() && !dst.isEmpty()) {
                rules.put(src, dst);
            }
        }
    }

    /**
     * @return 是否有任何替换规则
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * 对物品数组执行矿辞替换
     *
     * @param items 原始物品数组
     * @return 替换后的新数组 (不修改原数组)
     */
    public ItemStack[] apply(ItemStack[] items) {
        if (!hasRules() || items == null) return items;

        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = tryReplace(items[i]);
        }
        return result;
    }

    private ItemStack tryReplace(ItemStack original) {
        if (original == null) return null;

        // 获取此物品注册的所有矿辞名
        int[] oreIds = OreDictionary.getOreIDs(original);
        if (oreIds.length == 0) return original;

        for (int oreId : oreIds) {
            String oreName = OreDictionary.getOreName(oreId);
            if (rules.containsKey(oreName)) {
                String targetOre = rules.get(oreName);
                List<ItemStack> candidates = OreDictionary.getOres(targetOre);
                if (!candidates.isEmpty()) {
                    // 取第一个注册的物品作为替代
                    ItemStack replacement = candidates.get(0)
                        .copy();
                    replacement.stackSize = original.stackSize;
                    return replacement;
                }
            }
        }

        return original;
    }
}
