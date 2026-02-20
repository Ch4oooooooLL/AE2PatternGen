package com.github.ae2patterngen.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.github.ae2patterngen.item.ItemPatternGenerator;

import cpw.mods.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        cpw.mods.fml.common.FMLLog.info("[AE2PatternGen] getServerGuiElement ID=" + id + " Side=SERVER");
        if (id == ItemPatternGenerator.GUI_ID) {
            return new ContainerPatternGen(player, player.getCurrentEquippedItem());
        }
        if (id == ItemPatternGenerator.GUI_ID_STORAGE) {
            return new ContainerPatternStorage(player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        cpw.mods.fml.common.FMLLog.info("[AE2PatternGen] getClientGuiElement ID=" + id + " Side=CLIENT");
        try {
            if (id == ItemPatternGenerator.GUI_ID) {
                return new GuiPatternGen(new ContainerPatternGen(player, player.getCurrentEquippedItem()));
            }
            if (id == ItemPatternGenerator.GUI_ID_STORAGE) {
                return new GuiPatternStorage(new ContainerPatternStorage(player));
            }
        } catch (Throwable t) {
            cpw.mods.fml.common.FMLLog.severe("[AE2PatternGen] Error creating client GUI element: " + t);
            t.printStackTrace();
        }
        return null;
    }
}
