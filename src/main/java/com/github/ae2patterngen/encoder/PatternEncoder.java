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
 * 编码样板的 NBT 结构 (来自 AE2 PatternHelper.java):
 * 
 * <pre>
 * TagCompound:
 *   "in"           : NBTTagList  -> 输入物品
 *   "out"          : NBTTagList  -> 输出物品
 *   "crafting"     : boolean     -> false (加工配方)
 *   "substitute"   : boolean     -> false
 *   "beSubstitute" : boolean     -> false
 *   "author"       : String      -> "AE2PatternGen"
 * </pre>
 */
public class PatternEncoder {

    private static final String AE2_ENCODED_PATTERN_ID = "appliedenergistics2:item.ItemMultiMaterial";
    private static final int AE2_ENCODED_PATTERN_META = 52; // Encoded Pattern damage value

    /**
     * 将配方编码为 AE2 Processing Pattern
     * NC 物品会从输入列表中排除
     *
     * @param recipe 配方
     * @return 编码后的样板 ItemStack, 如果编码失败则返回 null
     */
    public static ItemStack encode(RecipeEntry recipe) {
        // 获取编码样板的 Item
        Item patternItem = findEncodedPatternItem();
        if (patternItem == null) {
            return null;
        }

        ItemStack patternStack = new ItemStack(patternItem, 1, AE2_ENCODED_PATTERN_META);

        NBTTagCompound tag = new NBTTagCompound();

        // 编码输入 (排除 NC 物品，即 stackSize == 0 的物品)
        NBTTagList inList = new NBTTagList();
        for (ItemStack input : recipe.inputs) {
            if (input == null) continue;
            if (input.stackSize <= 0) continue; // 排除 NC 物品

            NBTTagCompound itemTag = new NBTTagCompound();
            input.writeToNBT(itemTag);
            // 大数量支持
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
        tag.setBoolean("crafting", false); // Processing Pattern
        tag.setBoolean("substitute", false);
        tag.setBoolean("beSubstitute", false);
        tag.setString("author", "AE2PatternGen");

        patternStack.setTagCompound(tag);
        return patternStack;
    }

    /**
     * 批量编码配方为样板
     *
     * @param recipes 配方列表
     * @return 编码后的样板列表
     */
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
        // 尝试通过注册名找到 AE2 的编码样板物品
        Item item = (Item) Item.itemRegistry.getObject(AE2_ENCODED_PATTERN_ID);
        if (item != null) {
            return item;
        }

        // 备选方案: 通过 AE2 API
        try {
            for (ItemStack stack : appeng.api.AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()
                .maybeStack(1)
                .asSet()) {
                return stack.getItem();
            }
        } catch (Exception e) {
            // AE2 API not available
        }

        return null;
    }
}
