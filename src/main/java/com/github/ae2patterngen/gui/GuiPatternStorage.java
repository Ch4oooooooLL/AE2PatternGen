package com.github.ae2patterngen.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.AE2PatternGen;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketStorageAction;
import com.github.ae2patterngen.storage.PatternStorage;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiPatternStorage {

    private static final int GUI_W = 260;
    private static final int GUI_H = 260;

    public static ModularWindow createWindow(UIBuildContext buildContext, EntityPlayer player) {
        PatternStorage.StorageSummary summary = PatternStorage.getSummary(player.getUniqueID());

        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(EnumChatFormatting.BOLD + "▸ 样板仓储");
        titleText.setScale(1.2f);
        titleText.setSize(GUI_W - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        if (summary.count == 0) {
            String msg = "仓储为空，请先在配置 GUI 中生成样板";
            TextWidget emptyText = new TextWidget(msg);
            int strW = approximateTextWidth(msg);
            emptyText.setPos(GUI_W / 2 - strW / 2, 40);
            builder.widget(emptyText);
        } else {
            int y = 24;
            TextWidget statsTitle = new TextWidget(EnumChatFormatting.BOLD + "统计");
            statsTitle.setPos(8, y);
            builder.widget(statsTitle);
            y += 14;

            TextWidget countText = new TextWidget("总数: " + summary.count + " 个样板");
            countText.setPos(14, y);
            builder.widget(countText);
            y += 12;

            TextWidget sourceText = new TextWidget("来源: " + summary.source);
            sourceText.setPos(14, y);
            builder.widget(sourceText);
            y += 12;

            String timeStr = summary.timestamp <= 0 ? "N/A"
                : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(summary.timestamp));
            TextWidget timeText = new TextWidget("生成时间: " + timeStr);
            timeText.setPos(14, y);
            builder.widget(timeText);
            y += 16;

            TextWidget previewTitle = new TextWidget(EnumChatFormatting.BOLD + "样板预览 (共 " + summary.count + " 条)");
            previewTitle.setPos(8, y);
            builder.widget(previewTitle);
            y += 14;

            Scrollable scrollable = new Scrollable().setVerticalScroll();
            scrollable.setPos(8, y);
            scrollable.setSize(GUI_W - 16, GUI_H - y - 40);

            int listY = 0;
            List<String> previews = summary.previews;
            for (int i = 0; i < previews.size(); i++) {
                final int index = i;
                String name = previews.get(i);

                ButtonWidget rowBtn = new ButtonWidget();
                rowBtn.setPos(0, listY);
                rowBtn.setSize(GUI_W - 28, 14);
                rowBtn.setBackground(new Rectangle().setColor(0xFF1E1E30));

                TextWidget rowText = new TextWidget(
                    EnumChatFormatting.GRAY + "#" + (index + 1) + "  " + EnumChatFormatting.WHITE + name);
                rowText.setPos(4, listY + 3);

                rowBtn.setOnClick((cd, w) -> {
                    PatternStorage.PatternDetail detail = PatternStorage.getPatternDetail(player.getUniqueID(), index);
                    if (detail != null) {
                        AE2PatternGen.proxy.openPatternDetailScreen(player, index, detail.inputs, detail.outputs);
                    }
                });

                scrollable.widget(rowBtn);
                scrollable.widget(rowText);
                listY += 16;
            }
            builder.widget(scrollable);
        }

        int btnW = 90;
        int btnH = 20;
        int btnY = GUI_H - 32;

        ButtonWidget btnExtract = new ButtonWidget();
        btnExtract.setPos(GUI_W / 2 - btnW - 4, btnY);
        btnExtract.setSize(btnW, btnH);
        btnExtract.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnExtText = new TextWidget("取出到背包");
        btnExtText.setPos(GUI_W / 2 - btnW - 4 + 16, btnY + 6);
        btnExtract.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_EXTRACT));
            AE2PatternGen.proxy.closeCurrentScreen();
        });
        builder.widget(btnExtract);
        builder.widget(btnExtText);

        ButtonWidget btnClear = new ButtonWidget();
        btnClear.setPos(GUI_W / 2 + 4, btnY);
        btnClear.setSize(btnW, btnH);
        btnClear.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        TextWidget btnClrText = new TextWidget("一键清空");
        btnClrText.setPos(GUI_W / 2 + 4 + 20, btnY + 6);
        btnClear.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketStorageAction(PacketStorageAction.ACTION_CLEAR));
            AE2PatternGen.proxy.closeCurrentScreen();
        });
        builder.widget(btnClear);
        builder.widget(btnClrText);

        TextWidget footerText = new TextWidget(EnumChatFormatting.GRAY + "● 蹲下右键方块可直接导出到容器");
        footerText.setPos(8, GUI_H - 12);
        builder.widget(footerText);

        return builder.build();
    }

    private static int approximateTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() * 6;
    }
}
