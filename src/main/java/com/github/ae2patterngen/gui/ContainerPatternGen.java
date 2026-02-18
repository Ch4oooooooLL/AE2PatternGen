package com.github.ae2patterngen.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.github.ae2patterngen.item.ItemPatternGenerator;

/**
 * 样板生成器 Container — 无物品槽位，持有当前手持的 ItemStack 引用
 */
public class ContainerPatternGen extends Container {

    public final EntityPlayer player;
    public final ItemStack heldItem;

    public ContainerPatternGen(EntityPlayer player) {
        this.player = player;
        this.heldItem = player.getCurrentEquippedItem();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    /**
     * 保存 GUI 数据到手持物品的 NBT
     */
    public void saveData(String recipeMap, String outputOre, String inputOre, String ncItem) {
        if (heldItem != null && heldItem.getItem() instanceof ItemPatternGenerator) {
            ItemPatternGenerator.saveAllFields(heldItem, recipeMap, outputOre, inputOre, ncItem);
        }
    }
}
