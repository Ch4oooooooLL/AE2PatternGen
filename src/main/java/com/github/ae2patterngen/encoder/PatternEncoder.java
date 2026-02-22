package com.github.ae2patterngen.encoder;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * AE2 样板编码器 — 将 RecipeEntry 转为 AE2 编码样板 ItemStack
 * <p>
 * 支持物品和流体编码。流体通过 AE2FC 的 ItemFluidDrop 转为物品表示。
 */
public class PatternEncoder {

    // GTNH 2.8.4 AE2 Unofficial 的已编码样板物品 ID
    private static final String AE2_ENCODED_PATTERN_ID = "appliedenergistics2:item.ItemEncodedPattern";

    /** 缓存 AE2FC 是否可用 */
    private static Boolean ae2fcAvailable = null;

    /**
     * 将配方编码为 AE2 Processing Pattern (支持流体)
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

        // ---- 编码输入 ----
        NBTTagList inList = new NBTTagList();

        // 物品输入
        for (ItemStack input : recipe.inputs) {
            appendStackTag(inList, input);
        }

        // 流体输入 -> ItemFluidDrop
        for (FluidStack fluid : recipe.fluidInputs) {
            if (fluid == null || fluid.amount <= 0) continue;
            ItemStack fluidItem = convertFluidToItem(fluid);
            appendStackTag(inList, fluidItem);
        }

        // ---- 编码输出 ----
        NBTTagList outList = new NBTTagList();

        // 物品输出
        for (ItemStack output : recipe.outputs) {
            appendStackTag(outList, output);
        }

        // 流体输出 -> ItemFluidDrop
        for (FluidStack fluid : recipe.fluidOutputs) {
            if (fluid == null || fluid.amount <= 0) continue;
            ItemStack fluidItem = convertFluidToItem(fluid);
            appendStackTag(outList, fluidItem);
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

    /**
     * 将 ItemStack 以 AE2 可正确解析的大数量格式写入样板 NBT。
     * <p>
     * AE2 的 Pattern 解析逻辑读取的是 int 类型的 "Count"，不是 "Cnt"。
     */
    private static void appendStackTag(NBTTagList targetList, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) {
            return;
        }

        NBTTagCompound itemTag = new NBTTagCompound();
        stack.writeToNBT(itemTag);

        // 保持 Count 为 int，避免 >127 时因 byte 溢出被误读为 1 或负数
        itemTag.setInteger("Count", stack.stackSize);

        // 同步写入 Cnt 兼容字段，避免不同读取路径出现计数分歧
        itemTag.setLong("Cnt", stack.stackSize);

        targetList.appendTag(itemTag);
    }

    /**
     * 将 FluidStack 转为 AE2FC 的 ItemFluidDrop ItemStack
     * <p>
     * 如果 AE2FC 不可用，返回 null（优雅降级）
     */
    private static ItemStack convertFluidToItem(FluidStack fluid) {
        if (!isAe2fcAvailable()) {
            return null;
        }
        try {
            return com.glodblock.github.common.item.ItemFluidDrop.newStack(fluid);
        } catch (Throwable e) {
            // AE2FC 运行时不可用，禁用后续尝试
            ae2fcAvailable = false;
            return null;
        }
    }

    /**
     * 检测 AE2FC 是否可用
     */
    private static boolean isAe2fcAvailable() {
        if (ae2fcAvailable != null) {
            return ae2fcAvailable;
        }
        try {
            Class.forName("com.glodblock.github.common.item.ItemFluidDrop");
            ae2fcAvailable = true;
        } catch (ClassNotFoundException e) {
            ae2fcAvailable = false;
        }
        return ae2fcAvailable;
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
