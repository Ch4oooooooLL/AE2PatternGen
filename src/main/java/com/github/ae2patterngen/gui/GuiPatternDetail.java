package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketStorageAction;
import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiPatternDetail {

    private static final int GUI_W = 260;
    private static final int GUI_H = 220;

    public static ModularWindow createWindow(UIBuildContext buildContext, int patternIndex, List<String> inputNames,
        List<String> outputNames) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(EnumChatFormatting.BOLD + "▸ 样板详情 #" + (patternIndex + 1));
        titleText.setScale(1.2f);
        titleText.setSize(200, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 24);
        scrollable.setSize(GUI_W - 16, GUI_H - 24 - 32);

        int y = 0;
        TextWidget inTitle = new TextWidget(EnumChatFormatting.BOLD + "输入 (" + inputNames.size() + ")");
        inTitle.setPos(4, y);
        scrollable.widget(inTitle);
        y += 12;

        if (inputNames.isEmpty()) {
            TextWidget emptyIn = new TextWidget(EnumChatFormatting.GRAY + "(无)");
            emptyIn.setPos(8, y);
            scrollable.widget(emptyIn);
            y += 12;
        } else {
            for (String name : inputNames) {
                TextWidget row = new TextWidget(EnumChatFormatting.GRAY + "• " + EnumChatFormatting.WHITE + name);
                row.setPos(8, y);
                scrollable.widget(row);
                y += 12;
            }
        }
        y += 8;

        TextWidget outTitle = new TextWidget(EnumChatFormatting.BOLD + "输出 (" + outputNames.size() + ")");
        outTitle.setPos(4, y);
        scrollable.widget(outTitle);
        y += 12;

        if (outputNames.isEmpty()) {
            TextWidget emptyOut = new TextWidget(EnumChatFormatting.GRAY + "(无)");
            emptyOut.setPos(8, y);
            scrollable.widget(emptyOut);
            y += 12;
        } else {
            for (String name : outputNames) {
                TextWidget row = new TextWidget(EnumChatFormatting.GREEN + "▶ " + EnumChatFormatting.WHITE + name);
                row.setPos(8, y);
                scrollable.widget(row);
                y += 12;
            }
        }
        builder.widget(scrollable);

        int btnW = 90;
        int btnH = 20;
        int btnY = GUI_H - 28;

        ButtonWidget btnDelete = new ButtonWidget();
        btnDelete.setPos(GUI_W / 2 - btnW - 4, btnY);
        btnDelete.setSize(btnW, btnH);
        btnDelete.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnDelText = new TextWidget("删除此样板");
        btnDelText.setPos(GUI_W / 2 - btnW - 4 + 16, btnY + 6);
        btnDelete.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE
                .sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_DELETE, patternIndex));
            Minecraft.getMinecraft().thePlayer.closeScreen();
        });
        builder.widget(btnDelete);
        builder.widget(btnDelText);

        ButtonWidget btnBack = new ButtonWidget();
        btnBack.setPos(GUI_W / 2 + 4, btnY);
        btnBack.setSize(btnW, btnH);
        btnBack.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnBackText = new TextWidget("返回");
        btnBackText.setPos(GUI_W / 2 + 4 + 32, btnY + 6);
        btnBack.setOnClick((cd, w) -> {
            Minecraft.getMinecraft().thePlayer.closeScreen();

            // Reopen GUI
            UIBuildContext newContext = new UIBuildContext(Minecraft.getMinecraft().thePlayer);
            ModularUIContext muiContext = new ModularUIContext(newContext, () -> {});
            ModularWindow storageWindow = GuiPatternStorage
                .createWindow(newContext, Minecraft.getMinecraft().thePlayer);
            Minecraft.getMinecraft()
                .displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, storageWindow)));
        });
        builder.widget(btnBack);
        builder.widget(btnBackText);

        return builder.build();
    }
}
