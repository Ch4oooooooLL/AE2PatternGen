package com.github.ae2patterngen.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

public class InventoryUtil {

    /**
     * 计算玩家背包中特定物品的数量
     */
    public static int countItem(EntityPlayer player, ItemStack target) {
        if (player == null || target == null || target.getItem() == null)
            return 0;
        int count = 0;
        IInventory inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == target.getItem()
                    && stack.getItemDamage() == target.getItemDamage()) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    /**
     * 消耗玩家背包中指定数量的特定物品
     * 
     * @return 如果扣除成功（数量足够）返回 true；如果失败（数量不足）不会扣除任何物品，返回 false。
     */
    public static boolean consumeItem(EntityPlayer player, ItemStack target, int amount) {
        if (countItem(player, target) < amount)
            return false;

        int remainToConsume = amount;
        IInventory inv = player.inventory;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (remainToConsume <= 0)
                break;

            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == target.getItem()
                    && stack.getItemDamage() == target.getItemDamage()) {
                if (stack.stackSize <= remainToConsume) {
                    remainToConsume -= stack.stackSize;
                    inv.setInventorySlotContents(i, null);
                } else {
                    stack.stackSize -= remainToConsume;
                    remainToConsume = 0;
                }
            }
        }

        // 通知背包更新
        player.inventoryContainer.detectAndSendChanges();
        return true;
    }

    /**
     * 获得 AE2 的空白样板 ItemStack 实例
     */
    public static ItemStack getBlankPattern() {
        return new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 52);
    }
}
