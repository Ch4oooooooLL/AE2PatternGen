package com.github.ae2patterngen.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.github.ae2patterngen.item.ItemPatternGenerator;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ItemPatternGenerator.GUI_ID) {
            return new ContainerPatternGen(player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == ItemPatternGenerator.GUI_ID) {
            return new GuiPatternGen(new ContainerPatternGen(player));
        }
        return null;
    }
}
