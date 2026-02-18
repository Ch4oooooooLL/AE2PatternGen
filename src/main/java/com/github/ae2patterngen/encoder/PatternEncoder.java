package com.github.ae2patterngen.encoder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * AE2 样板编码器 — 将 RecipeEntry 转为 AE2 编码样板 ItemStack
 * <p>
 * 修正: 使用正确的已编码样板物品 ID
 */
public class PatternEncoder {

    // GTNH 2.8.4 AE2 Unofficial 的已编码样板物品 ID
    private static final String AE2_ENCODED_PATTERN_ID = "appliedenergistics2:item.ItemEncodedPattern";

    /**
     * 将配方编码为 AE2 Processing Pattern
     *
     * @param recipe 配方
     * @return 编码后的样板 ItemStack
     */
    public static ItemStack encode(RecipeEntry recipe) {
        // 获取编码样板的 Item
        Item patternItem = findEncodedPatternItem();
        if (patternItem == null) {
            return null;
        }

        // 样板物品本身
        ItemStack patternStack = new ItemStack(patternItem, 1, 0);

        NBTTagCompound tag = new NBTTagCompound();

        // 编码输入
        NBTTagList inList = new NBTTagList();
        for (ItemStack input : recipe.inputs) {
            if (input == null) continue;
            // 排除 NC 物品 (stackSize <= 0)
            if (input.stackSize <= 0) continue;

            NBTTagCompound itemTag = new NBTTagCompound();
            input.writeToNBT(itemTag);
            // 处理 GTNH 的 Cnt 扩展 (支持超过 127 的堆叠)
            if (input.stackSize > 127) {
                itemTag.setLong("Cnt", input.stackSize);
            }
            inList.appendTag(itemTag);
        }

        // 编码输出
        NBTTagList outList = new NBTTagList();
        for (ItemStack output : recipe.outputs) {
            if (output == null) continue;

            NBTTagCompound itemTag = new NBTTagCompound();
            output.writeToNBT(itemTag);
            if (output.stackSize > 127) {
                itemTag.setLong("Cnt", output.stackSize);
            }
            outList.appendTag(itemTag);
        }

        // 验证：至少要有一个输入和一个输出
        if (inList.tagCount() == 0 || outList.tagCount() == 0) {
            return null;
        }

        tag.setTag("in", inList);
        tag.setTag("out", outList);
        tag.setBoolean("crafting", false); // 加工模式
        tag.setBoolean("substitute", false);
        tag.setBoolean("beSubstitute", false);
        // AE2 rv3 通常还需要这个标记
        tag.setBoolean("isStandard", true);

        patternStack.setTagCompound(tag);
        return patternStack;
    }

    public static List<ItemStack> encodeBatch(List<RecipeEntry> recipes) {
        List<ItemStack> patterns = new ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            ItemStack pattern = encode(recipe);
            if (pattern != null) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    private static Item findEncodedPatternItem() {
        // 1. 优先通过 AE2 API 获取正确的定义
        try {
            for (ItemStack stack : appeng.api.AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()
                .maybeStack(1)
                .asSet()) {
                if (stack != null && stack.getItem() != null) {
                    return stack.getItem();
                }
            }
        } catch (Throwable e) {
            // Fallback
        }

        // 2. 备选方案: 通过注册名
        return (Item) Item.itemRegistry.getObject(AE2_ENCODED_PATTERN_ID);
    }
}
