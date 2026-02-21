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
            com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                player);
            com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                buildContext,
                () -> {});
            com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternGen
                .createWindow(buildContext, player.getCurrentEquippedItem());
            return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window);
        }
        if (id == ItemPatternGenerator.GUI_ID_STORAGE) {
            com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                player);
            com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                buildContext,
                () -> {});
            com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternStorage
                .createWindow(buildContext, player);
            return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        cpw.mods.fml.common.FMLLog.info("[AE2PatternGen] getClientGuiElement ID=" + id + " Side=CLIENT");
        try {
            if (id == ItemPatternGenerator.GUI_ID) {
                com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                    player);
                com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                    buildContext,
                    () -> {});
                com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternGen
                    .createWindow(buildContext, player.getCurrentEquippedItem());
                return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui(
                    new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window));
            }
            if (id == ItemPatternGenerator.GUI_ID_STORAGE) {
                com.gtnewhorizons.modularui.api.screen.UIBuildContext buildContext = new com.gtnewhorizons.modularui.api.screen.UIBuildContext(
                    player);
                com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
                    buildContext,
                    () -> {});
                com.gtnewhorizons.modularui.api.screen.ModularWindow window = GuiPatternStorage
                    .createWindow(buildContext, player);
                return new com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui(
                    new com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer(muiContext, window));
            }
        } catch (Throwable t) {
            cpw.mods.fml.common.FMLLog.severe("[AE2PatternGen] Error creating client GUI element: " + t);
            t.printStackTrace();
        }
        return null;
    }
}
