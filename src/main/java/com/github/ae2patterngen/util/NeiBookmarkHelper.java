package com.github.ae2patterngen.util;

import java.util.List;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 把规则组里的 NC（不消耗）物品放进 NEI 书签栏。
 * <p>
 * 样板输入总成的手动槽位要摆的就是这些物品，而 GT 侧的 NC 不会写进样板里（本项目口径：样板内容不变），
 * 所以投递后把清单放进 NEI 书签，玩家可以直接从书签拖进总成。
 * <p>
 * <b>只在客户端加载</b>：本类引用 NEI 的类，只能从客户端包处理逻辑里调用（服务端没有 NEI）。
 * 所有 NEI 调用都包在 try/catch 里——NEI 版本差异不该连带影响投递本身的反馈。
 */
@SideOnly(Side.CLIENT)
public final class NeiBookmarkHelper {

    private NeiBookmarkHelper() {}

    /**
     * 把这些物品加入 NEI 书签。
     *
     * @return 实际新增的书签数量（已存在的物品 NEI 会跳过）
     */
    public static int addAll(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return 0;
        }

        int added = 0;
        try {
            codechicken.nei.BookmarkPanel panel = codechicken.nei.ItemPanels.bookmarkPanel;
            // NEI 每帧都会把界面上的 grid 换成书签存储的活动命名空间；
            // 刚读档/切命名空间时不先同步一次，会加进一个马上被丢弃的 grid（表现为"加了但看不到"）。
            panel.update();
            for (ItemStack stack : stacks) {
                if (stack == null || stack.getItem() == null) {
                    continue;
                }
                ItemStack copy = stack.copy();
                copy.stackSize = Math.max(1, copy.stackSize);
                if (panel.addItem(copy)) {
                    added++;
                }
            }
        } catch (Throwable ignored) {
            // NEI 不可用或 API 漂移时静默降级：投递结果仍由聊天栏提示
        }
        return added;
    }
}
