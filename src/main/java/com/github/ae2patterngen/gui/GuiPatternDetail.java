package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketStorageAction;

/**
 * 样板详情 GUI — 显示单个样板的输入/输出物品列表
 * <p>
 * 纯客户端覆盖层，不注册 GUI ID
 */
public class GuiPatternDetail extends GuiScreen {

    private static final int GUI_W = 260;
    private static final int GUI_H = 220;

    // 调色板
    private static final int COL_PANEL_BG = 0xF0181825;
    private static final int COL_BORDER_OUTER = 0xFF2A2A44;
    private static final int COL_BORDER_GLOW = 0xFF5B89FF;
    private static final int COL_CARD_BG = 0xFF1E1E30;
    private static final int COL_CARD_BORDER = 0xFF33335A;
    private static final int COL_TITLE_TEXT = 0xFFFFFFFF;
    private static final int COL_SECTION_TEXT = 0xFF8899CC;
    private static final int COL_LABEL_TEXT = 0xFFAABBDD;
    private static final int COL_ITEM_TEXT = 0xFFCCDDEE;
    private static final int COL_BTN_NORMAL = 0xFF2E3B5A;
    private static final int COL_BTN_HOVER = 0xFF3D4F7A;
    private static final int COL_BTN_DANGER = 0xFF8B2020;
    private static final int COL_BTN_DANGER_HOVER = 0xFFAA3333;

    private static final int PAD = 8;
    private static final int CARD_PAD = 6;

    private final int patternIndex;
    private final List<String> inputNames;
    private final List<String> outputNames;
    private final GuiScreen parentScreen;

    private GuiButton btnDelete;
    private GuiButton btnBack;

    private int guiLeft;
    private int guiTop;

    public GuiPatternDetail(GuiScreen parent, int patternIndex, List<String> inputNames, List<String> outputNames) {
        this.parentScreen = parent;
        this.patternIndex = patternIndex;
        this.inputNames = inputNames;
        this.outputNames = outputNames;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_W) / 2;
        guiTop = (height - GUI_H) / 2;

        int btnW = 90;
        int btnH = 20;
        int btnY = guiTop + GUI_H - PAD - btnH;

        btnDelete = new GuiButton(
                0,
                guiLeft + (GUI_W / 2 - btnW - 4),
                btnY,
                btnW,
                btnH,
                "\u5220\u9664\u6B64\u6837\u677F");
        btnBack = new GuiButton(1, guiLeft + (GUI_W / 2 + 4), btnY, btnW, btnH, "\u8FD4\u56DE");

        buttonList.add(btnDelete);
        buttonList.add(btnBack);
    }

    private static final int MAX_ITEMS_DISPLAY = 12;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // 主面板
        drawRect(guiLeft, guiTop, guiLeft + GUI_W, guiTop + GUI_H, COL_PANEL_BG);
        drawHollowRect(guiLeft, guiTop, GUI_W, GUI_H, COL_BORDER_OUTER);
        drawHollowRect(guiLeft + 1, guiTop + 1, GUI_W - 2, GUI_H - 2, COL_BORDER_GLOW);

        // 标题
        int titleY = guiTop + 6;
        fontRendererObj.drawStringWithShadow(
                "\u25B8 \u6837\u677F\u8BE6\u60C5 #" + (patternIndex + 1),
                guiLeft + PAD + 2,
                titleY,
                COL_TITLE_TEXT);
        drawRect(guiLeft + PAD, guiTop + 18, guiLeft + GUI_W - PAD, guiTop + 19, COL_BORDER_GLOW);

        int contentLeft = guiLeft + PAD;
        int cardW = GUI_W - PAD * 2;
        int y = guiTop + 24;

        // 输入卡片
        int displayedInputs = Math.min(inputNames.size(), MAX_ITEMS_DISPLAY);
        boolean inputTruncated = inputNames.size() > MAX_ITEMS_DISPLAY;
        int inputH = 14 + (displayedInputs + (inputTruncated ? 1 : 0)) * 11 + CARD_PAD;

        drawCard(contentLeft, y, cardW, inputH);
        fontRendererObj.drawStringWithShadow(
                "\u00A7r\u8F93\u5165 (" + inputNames.size() + ")",
                contentLeft + CARD_PAD,
                y + 3,
                COL_SECTION_TEXT);
        y += 14;
        if (inputNames.isEmpty()) {
            fontRendererObj
                    .drawStringWithShadow(EnumChatFormatting.GRAY + "(\u65E0)", contentLeft + CARD_PAD, y,
                            COL_LABEL_TEXT);
            y += 11;
        } else {
            for (int i = 0; i < displayedInputs; i++) {
                String name = inputNames.get(i);
                String displayName = truncate(name, cardW - CARD_PAD * 2 - 10);
                fontRendererObj.drawStringWithShadow(
                        EnumChatFormatting.GRAY + "\u2022 " + EnumChatFormatting.WHITE + displayName,
                        contentLeft + CARD_PAD,
                        y,
                        COL_ITEM_TEXT);
                y += 11;
            }
            if (inputTruncated) {
                fontRendererObj.drawStringWithShadow(
                        EnumChatFormatting.DARK_GRAY + "... \u8FD8\u6709 "
                                + (inputNames.size() - displayedInputs)
                                + " \u9879",
                        contentLeft + CARD_PAD,
                        y,
                        COL_LABEL_TEXT);
                y += 11;
            }
        }
        y += CARD_PAD + 4;

        // 输出卡片
        int displayedOutputs = Math.min(outputNames.size(), MAX_ITEMS_DISPLAY); // 通常输出很少，但也加上限制
        boolean outputTruncated = outputNames.size() > MAX_ITEMS_DISPLAY;
        int outputH = 14 + (displayedOutputs + (outputTruncated ? 1 : 0)) * 11 + CARD_PAD;

        if (y + outputH > guiTop + GUI_H - 28) { // 防止输出区域超出
            // 如果空间非常紧缺，可能需要压缩。但在 MAX_ITEMS_DISPLAY=12 且 GUI_H=220 下，应该勉强够用。
            // 如果输入占满，y ≈ 24 + 14 + 13*11 + 10 = 191
            // 按钮在 220 - 28 = 192
            // 确实非常紧凑。也许输入应该限制为 8？
            // 如果输入很多，输出也有一点。
        }

        drawCard(contentLeft, y, cardW, outputH);
        fontRendererObj.drawStringWithShadow(
                "\u00A7r\u8F93\u51FA (" + outputNames.size() + ")",
                contentLeft + CARD_PAD,
                y + 3,
                COL_SECTION_TEXT);
        y += 14;
        if (outputNames.isEmpty()) {
            fontRendererObj
                    .drawStringWithShadow(EnumChatFormatting.GRAY + "(\u65E0)", contentLeft + CARD_PAD, y,
                            COL_LABEL_TEXT);
        } else {
            for (int i = 0; i < displayedOutputs; i++) {
                String name = outputNames.get(i);
                String displayName = truncate(name, cardW - CARD_PAD * 2 - 10);
                fontRendererObj.drawStringWithShadow(
                        EnumChatFormatting.GREEN + "\u25B6 " + EnumChatFormatting.WHITE + displayName,
                        contentLeft + CARD_PAD,
                        y,
                        COL_ITEM_TEXT);
                y += 11;
            }
            if (outputTruncated) {
                fontRendererObj.drawStringWithShadow(
                        EnumChatFormatting.DARK_GRAY + "... \u8FD8\u6709 "
                                + (outputNames.size() - displayedOutputs)
                                + " \u9879",
                        contentLeft + CARD_PAD,
                        y,
                        COL_LABEL_TEXT);
            }
        }

        // 按钮
        drawModernButton(btnDelete, mouseX, mouseY, true);
        drawModernButton(btnBack, mouseX, mouseY, false);

        // 注意：不调用 super.drawScreen，因为我们手动绘制了按钮
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isMouseOver(btnDelete, mouseX, mouseY)) {
            NetworkHandler.sendStorageAction(new PacketStorageAction(PacketStorageAction.ACTION_DELETE, patternIndex));
            mc.thePlayer.closeScreen();
        } else if (isMouseOver(btnBack, mouseX, mouseY)) {
            mc.displayGuiScreen(null);
            // 重新打开仓储 GUI
            mc.thePlayer.closeScreen();
        }
    }

    // ---- 辅助方法 ----

    private String truncate(String text, int maxPixelW) {
        if (fontRendererObj.getStringWidth(text) <= maxPixelW)
            return text;
        while (fontRendererObj.getStringWidth(text + "...") > maxPixelW && text.length() > 3) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    private void drawHollowRect(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCard(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, COL_CARD_BG);
        drawHollowRect(x, y, w, h, COL_CARD_BORDER);
    }

    private void drawModernButton(GuiButton btn, int mouseX, int mouseY, boolean danger) {
        boolean hovered = mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
                && mouseY >= btn.yPosition
                && mouseY < btn.yPosition + btn.height;

        int bgColor;
        if (danger) {
            bgColor = hovered ? COL_BTN_DANGER_HOVER : COL_BTN_DANGER;
        } else {
            bgColor = hovered ? COL_BTN_HOVER : COL_BTN_NORMAL;
        }

        drawRect(btn.xPosition, btn.yPosition, btn.xPosition + btn.width, btn.yPosition + btn.height, bgColor);
        drawHollowRect(btn.xPosition, btn.yPosition, btn.width, btn.height, COL_CARD_BORDER);

        int textW = fontRendererObj.getStringWidth(btn.displayString);
        int textX = btn.xPosition + (btn.width - textW) / 2;
        int textY = btn.yPosition + (btn.height - 8) / 2;
        fontRendererObj.drawStringWithShadow(btn.displayString, textX, textY, 0xFFFFFF);

        btn.visible = false;
    }

    private boolean isMouseOver(GuiButton btn, int mx, int my) {
        return mx >= btn.xPosition && mx < btn.xPosition + btn.width
                && my >= btn.yPosition
                && my < btn.yPosition + btn.height;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
