package com.github.ae2patterngen.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/**
 * 样本生成器的影子容器 (无槽位)
 */
public class ContainerPatternGen extends Container {

    public final ItemStack heldItem;

    public ContainerPatternGen(EntityPlayer player, ItemStack stack) {
        this.heldItem = stack;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
