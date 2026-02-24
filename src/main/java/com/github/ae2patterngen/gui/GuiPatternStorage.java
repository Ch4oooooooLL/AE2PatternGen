package com.github.ae2patterngen.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.AE2PatternGen;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketStorageAction;
import com.github.ae2patterngen.storage.PatternStorage;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

public class GuiPatternStorage {

    private static final int GUI_W = 260;
    private static final int GUI_H = 260;
    private static final Map<UUID, String> SEARCH_QUERY_BY_PLAYER = new HashMap<UUID, String>();

    public static ModularWindow createWindow(UIBuildContext buildContext, EntityPlayer player) {
        PatternStorage.StorageSummary summary = PatternStorage.getSummary(player.getUniqueID());
        String searchQuery = getSearchQuery(player.getUniqueID());

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

            int searchInputX = 46;
            int searchBtnW = 34;
            int resetBtnW = 34;
            int searchGap = 2;
            int searchFieldW = GUI_W - 16 - searchInputX - searchBtnW - resetBtnW - searchGap * 2;

            TextWidget searchLabel = new TextWidget("搜索:");
            searchLabel.setPos(14, y + 3);
            builder.widget(searchLabel);

            TextFieldWidget tfSearch = new TextFieldWidget();
            tfSearch.setText(searchQuery);
            tfSearch.setPos(searchInputX, y);
            tfSearch.setSize(searchFieldW, 14);
            tfSearch.setTextColor(0xFFFFFF);
            tfSearch.setBackground(new Rectangle().setColor(0xFF1E1E30));
            tfSearch.setTextAlignment(Alignment.CenterLeft);
            builder.widget(tfSearch);

            int btnSearchX = searchInputX + searchFieldW + searchGap;
            ButtonWidget btnSearch = new ButtonWidget();
            btnSearch.setPos(btnSearchX, y);
            btnSearch.setSize(searchBtnW, 14);
            btnSearch.setBackground(new Rectangle().setColor(0xFF1E1E30));
            TextWidget btnSearchText = new TextWidget("搜索");
            btnSearchText.setPos(btnSearchX + 5, y + 3);
            btnSearchText.setStringSupplier(() -> EnumChatFormatting.WHITE + "搜索");
            btnSearch.setOnClick((cd, w) -> {
                setSearchQuery(player.getUniqueID(), tfSearch.getText());
                AE2PatternGen.proxy.openPatternStorageScreen(player);
            });
            builder.widget(btnSearch);
            builder.widget(btnSearchText);

            int btnResetX = btnSearchX + searchBtnW + searchGap;
            ButtonWidget btnReset = new ButtonWidget();
            btnReset.setPos(btnResetX, y);
            btnReset.setSize(resetBtnW, 14);
            btnReset.setBackground(new Rectangle().setColor(0xFF1E1E30));
            TextWidget btnResetText = new TextWidget("全部");
            btnResetText.setPos(btnResetX + 5, y + 3);
            btnResetText.setStringSupplier(() -> EnumChatFormatting.WHITE + "全部");
            btnReset.setOnClick((cd, w) -> {
                setSearchQuery(player.getUniqueID(), "");
                AE2PatternGen.proxy.openPatternStorageScreen(player);
            });
            builder.widget(btnReset);
            builder.widget(btnResetText);

            y += 20;

            List<Integer> filteredIndices = filterIndices(summary.previews, searchQuery);
            TextWidget previewTitle = new TextWidget(
                EnumChatFormatting.BOLD + "样板预览 (" + filteredIndices.size() + "/" + summary.count + ")");
            previewTitle.setPos(8, y);
            builder.widget(previewTitle);
            y += 14;

            Scrollable scrollable = new Scrollable().setVerticalScroll();
            scrollable.setPos(8, y);
            scrollable.setSize(GUI_W - 16, GUI_H - y - 40);

            int listY = 0;
            if (filteredIndices.isEmpty()) {
                TextWidget emptyResult = new TextWidget(EnumChatFormatting.GRAY + "未找到匹配样板");
                emptyResult.setPos(4, listY + 3);
                scrollable.widget(emptyResult);
                listY += 16;
            } else {
                for (int i = 0; i < filteredIndices.size(); i++) {
                    int actualIndex = filteredIndices.get(i);
                    String name = summary.previews.get(actualIndex);

                    ButtonWidget rowBtn = new ButtonWidget();
                    rowBtn.setPos(0, listY);
                    rowBtn.setSize(GUI_W - 28, 14);
                    rowBtn.setBackground(new Rectangle().setColor(0xFF1E1E30));

                    TextWidget rowText = new TextWidget(
                        EnumChatFormatting.GRAY + "#" + (actualIndex + 1) + "  " + EnumChatFormatting.WHITE + name);
                    rowText.setPos(4, listY + 3);

                    rowBtn.setOnClick((cd, w) -> {
                        PatternStorage.PatternDetail detail = PatternStorage
                            .getPatternDetail(player.getUniqueID(), actualIndex);
                        if (detail != null) {
                            AE2PatternGen.proxy
                                .openPatternDetailScreen(player, actualIndex, detail.inputs, detail.outputs);
                        }
                    });

                    scrollable.widget(rowBtn);
                    scrollable.widget(rowText);
                    listY += 16;
                }
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

    private static String getSearchQuery(UUID playerUUID) {
        String query = SEARCH_QUERY_BY_PLAYER.get(playerUUID);
        return query == null ? "" : query;
    }

    private static void setSearchQuery(UUID playerUUID, String query) {
        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            SEARCH_QUERY_BY_PLAYER.remove(playerUUID);
            return;
        }
        SEARCH_QUERY_BY_PLAYER.put(playerUUID, normalized);
    }

    private static List<Integer> filterIndices(List<String> previews, String query) {
        List<Integer> result = new ArrayList<Integer>();
        if (previews == null || previews.isEmpty()) {
            return result;
        }

        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            for (int i = 0; i < previews.size(); i++) {
                result.add(i);
            }
            return result;
        }

        String loweredQuery = normalized.toLowerCase(Locale.ROOT);
        for (int i = 0; i < previews.size(); i++) {
            String preview = previews.get(i);
            if (preview != null && preview.toLowerCase(Locale.ROOT)
                .contains(loweredQuery)) {
                result.add(i);
            }
        }

        return result;
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private static int approximateTextWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() * 6;
    }
}
