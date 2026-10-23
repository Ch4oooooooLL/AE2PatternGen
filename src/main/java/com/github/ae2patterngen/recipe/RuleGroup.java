package com.github.ae2patterngen.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * 一个"规则组"：池内经 (编程电路, NC 物品) 归并后的一组配方。
 * <p>
 * 同一组内所有配方的编程电路与 NC 物品完全一致，因此可以共用同一套机器配置
 * （幽灵电路 / 输入总线里的模具等），支持整组批量生成样板。
 */
public final class RuleGroup {

    /** 组标识，池内唯一，用于网络回传与服务端定位。 */
    public final String groupId;

    /** 本组要求的编程电路编号（升序去重）；空数组表示不要求编程电路。 */
    public final int[] circuitDamages;

    /** 展示用的编程电路物品（stackSize = 1），无电路要求时为 null。 */
    public final ItemStack circuitDisplay;

    /** 规范化 NC 物品（去重、按签名排序、stackSize = 1）。 */
    public final ItemStack[] ncItems;

    /** 组内配方，保持配方缓存中的原始顺序。 */
    public final List<RecipeEntry> recipes;

    /** 是否为"既不要求编程电路、也没有 NC 物品"的默认组。 */
    public final boolean unruled;

    RuleGroup(String groupId, int[] circuitDamages, ItemStack circuitDisplay, ItemStack[] ncItems,
        List<RecipeEntry> recipes) {
        this.groupId = groupId != null ? groupId : "";
        this.circuitDamages = circuitDamages != null ? circuitDamages : new int[0];
        this.circuitDisplay = circuitDisplay;
        this.ncItems = ncItems != null ? ncItems : new ItemStack[0];
        this.recipes = Collections
            .unmodifiableList(new ArrayList<RecipeEntry>(recipes != null ? recipes : new ArrayList<RecipeEntry>()));
        this.unruled = this.circuitDamages.length == 0 && this.ncItems.length == 0;
    }

    public int size() {
        return recipes.size();
    }

    public boolean isSingleton() {
        return recipes.size() == 1;
    }
}
