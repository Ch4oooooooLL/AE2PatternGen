package com.github.ae2patterngen.proxy;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.github.ae2patterngen.gui.GuiPatternDetail;
import com.github.ae2patterngen.gui.GuiPatternStorage;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event, Object modInstance) {
        super.init(event, modInstance);
    }

    @Override
    public void closeCurrentScreen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void openPatternDetailScreen(EntityPlayer player, int index, List<String> inputs, List<String> outputs) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer uiPlayer = player != null ? player : mc.thePlayer;
        if (uiPlayer == null) {
            return;
        }

        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }

        UIBuildContext buildContext = new UIBuildContext(uiPlayer);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow detailWindow = GuiPatternDetail.createWindow(buildContext, index, inputs, outputs);
        mc.displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, detailWindow)));
    }

    @Override
    public void openPatternStorageScreen(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer uiPlayer = player != null ? player : mc.thePlayer;
        if (uiPlayer == null) {
            return;
        }

        if (mc.thePlayer != null) {
            mc.thePlayer.closeScreen();
        }

        UIBuildContext buildContext = new UIBuildContext(uiPlayer);
        ModularUIContext muiContext = new ModularUIContext(buildContext, () -> {});
        ModularWindow storageWindow = GuiPatternStorage.createWindow(buildContext, uiPlayer);
        mc.displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, storageWindow)));
    }
}
