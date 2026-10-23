package com.github.ae2patterngen.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 规则组预览数据：把一批（已过滤的）配方摊成"去重后的输入物品 + 输出物品 + 计数"。
 * <p>
 * 只收集**消耗型**输入（stackSize &gt; 0）：NC 物品属于规则本身，已在规则行上单独展示，
 * 混进输入清单只会让玩家误以为要往机器里放。
 */
public final class RecipePreviewSupport {

    /** 单侧（输入/输出）最多回给客户端的图标数量。 */
    public static final int MAX_SAMPLE_STACKS = 16;

    private RecipePreviewSupport() {}

    /** 预览结果：数量统计 + 去重后的展示用物品。 */
    public static final class Preview {

        /** 规则组内的配方总数。 */
        public final int totalCount;
        /** 当前过滤条件下命中的配方数。 */
        public final int matchedCount;
        /** 命中的配方里出现的不同输入物品数（消耗型）。 */
        public final int distinctInputCount;
        /** 命中的配方里出现的不同输出物品数。 */
        public final int distinctOutputCount;
        /** 输入物品示例（按 recipe 顺序，最多 {@link #MAX_SAMPLE_STACKS} 个）。 */
        public final ItemStack[] inputSamples;
        /** 输出物品示例（按 recipe 顺序，最多 {@link #MAX_SAMPLE_STACKS} 个）。 */
        public final ItemStack[] outputSamples;

        Preview(int totalCount, int matchedCount, int distinctInputCount, int distinctOutputCount,
            ItemStack[] inputSamples, ItemStack[] outputSamples) {
            this.totalCount = totalCount;
            this.matchedCount = matchedCount;
            this.distinctInputCount = distinctInputCount;
            this.distinctOutputCount = distinctOutputCount;
            this.inputSamples = inputSamples;
            this.outputSamples = outputSamples;
        }
    }

    public static Preview build(int totalCount, List<RecipeEntry> matched) {
        Map<String, ItemStack> inputs = new LinkedHashMap<String, ItemStack>();
        Map<String, ItemStack> outputs = new LinkedHashMap<String, ItemStack>();

        if (matched != null) {
            for (RecipeEntry recipe : matched) {
                if (recipe == null) {
                    continue;
                }
                collect(recipe.inputs, true, inputs);
                collect(recipe.outputs, false, outputs);
            }
        }

        return new Preview(
            Math.max(0, totalCount),
            matched != null ? matched.size() : 0,
            inputs.size(),
            outputs.size(),
            sampleOf(inputs),
            sampleOf(outputs));
    }

    private static void collect(ItemStack[] stacks, boolean consumableOnly, Map<String, ItemStack> target) {
        if (stacks == null) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getItem() == null) {
                continue;
            }
            if (consumableOnly && stack.stackSize <= 0) {
                continue;
            }
            String key = identityKey(stack);
            if (!target.containsKey(key)) {
                target.put(key, stack);
            }
        }
    }

    private static ItemStack[] sampleOf(Map<String, ItemStack> source) {
        List<ItemStack> samples = new ArrayList<ItemStack>(Math.min(source.size(), MAX_SAMPLE_STACKS));
        for (ItemStack stack : source.values()) {
            if (samples.size() >= MAX_SAMPLE_STACKS) {
                break;
            }
            ItemStack copy = stack.copy();
            if (copy.stackSize <= 0) {
                copy.stackSize = 1;
            }
            samples.add(copy);
        }
        return samples.toArray(new ItemStack[samples.size()]);
    }

    /** 物品身份：(注册名, meta, NBT)，不比较数量。 */
    static String identityKey(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        Object registryName = null;
        try {
            registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        } catch (Throwable ignored) {}
        builder.append(registryName != null ? registryName.toString() : "id:" + stack.getItem())
            .append('@')
            .append(stack.getItemDamage());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            builder.append('#')
                .append(tag.toString());
        }
        return builder.toString();
    }
}
