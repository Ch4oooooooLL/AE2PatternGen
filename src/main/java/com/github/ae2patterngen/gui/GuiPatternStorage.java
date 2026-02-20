package com.github.ae2patterngen.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketStorageAction;

/**
 * 样板仓储 GUI — 纯文字渲染，展示存储统计和分页预览
 * <p>
 * 支持: 翻页浏览 / 逐条删除 / 取出到背包 / 一键清空
 */
public class GuiPatternStorage extends GuiContainer {

    private static final int GUI_W = 260;
    private static final int GUI_H = 260;
    private static final int PAGE_SIZE = 8;

    // 调色板 (与 GuiPatternGen 一致)
    private static final int COL_PANEL_BG = 0xF0181825;
    private static final int COL_BORDER_OUTER = 0xFF2A2A44;
    private static final int COL_BORDER_GLOW = 0xFF5B89FF;
    private static final int COL_CARD_BG = 0xFF1E1E30;
    private static final int COL_CARD_BORDER = 0xFF33335A;
    private static final int COL_TITLE_TEXT = 0xFFFFFFFF;
    private static final int COL_SECTION_TEXT = 0xFF8899CC;
    private static final int COL_LABEL_TEXT = 0xFFAABBDD;
    private static final int COL_STATUS_BG = 0xFF14141F;
    private static final int COL_DIVIDER = 0xFF33335A;
    private static final int COL_BTN_NORMAL = 0xFF2E3B5A;
    private static final int COL_BTN_HOVER = 0xFF3D4F7A;
    private static final int COL_BTN_ACCENT = 0xFF2A5DB0;
    private static final int COL_BTN_ACCENT_HOVER = 0xFF3A70CC;
    private static final int COL_BTN_DANGER = 0xFF8B2020;
    private static final int COL_BTN_DANGER_HOVER = 0xFFAA3333;
    private static final int COL_PREVIEW_TEXT = 0xFF99AACC;
    private static final int COL_EMPTY_TEXT = 0xFF666688;
    private static final int COL_DELETE_NORMAL = 0xFF5A2E2E;
    private static final int COL_DELETE_HOVER = 0xFF7A3D3D;

    private static final int PAD = 8;
    private static final int CARD_PAD = 6;

    private final ContainerPatternStorage container;
    private GuiButton btnExtract;
    private GuiButton btnClear;
    private GuiButton btnPrev;
    private GuiButton btnNext;

    private int currentPage = 0;

    // 存储每条预览行的 Y 位置 (用于删除按钮点击检测)
    private int[] previewLineY = new int[PAGE_SIZE];
    private int previewStartX;
    private int deleteButtonX;
    private int actualPreviewCount = 0;

    public GuiPatternStorage(ContainerPatternStorage container) {
        super(container);
        this.container = container;
        this.xSize = GUI_W;
        this.ySize = GUI_H;
    }

    @Override
    public void initGui() {
        super.initGui();

        int btnW = 90;
        int btnH = 20;
        int btnY = guiTop + GUI_H - 48;

        btnExtract = new GuiButton(
            0,
            guiLeft + (GUI_W / 2 - btnW - 4),
            btnY,
            btnW,
            btnH,
            "\u53D6\u51FA\u5230\u80CC\u5305");
        btnClear = new GuiButton(1, guiLeft + (GUI_W / 2 + 4), btnY, btnW, btnH, "\u4E00\u952E\u6E05\u7A7A");

        int navBtnW = 40;
        int navY = btnY;
        btnPrev = new GuiButton(2, guiLeft + PAD, navY, navBtnW, btnH, "\u25C0");
        btnNext = new GuiButton(3, guiLeft + GUI_W - PAD - navBtnW, navY, navBtnW, btnH, "\u25B6");

        buttonList.add(btnExtract);
        buttonList.add(btnClear);
        buttonList.add(btnPrev);
        buttonList.add(btnNext);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 主面板
        drawRect(guiLeft, guiTop, guiLeft + GUI_W, guiTop + GUI_H, COL_PANEL_BG);
        drawHollowRect(guiLeft, guiTop, GUI_W, GUI_H, COL_BORDER_OUTER);
        drawHollowRect(guiLeft + 1, guiTop + 1, GUI_W - 2, GUI_H - 2, COL_BORDER_GLOW);

        // 标题栏
        int titleY = guiTop + 6;
        fontRendererObj
            .drawStringWithShadow("\u25B8 \u6837\u677F\u4ED3\u50A8", guiLeft + PAD + 2, titleY, COL_TITLE_TEXT);
        drawRect(guiLeft + PAD, guiTop + 18, guiLeft + GUI_W - PAD, guiTop + 19, COL_BORDER_GLOW);

        int contentLeft = guiLeft + PAD;
        int y = guiTop + 24;

        if (container.patternCount == 0) {
            // 空状态
            int emptyCardH = 40;
            drawCard(contentLeft, y, GUI_W - PAD * 2, emptyCardH);
            String emptyMsg = "\u4ED3\u50A8\u4E3A\u7A7A\uFF0C\u8BF7\u5148\u5728\u914D\u7F6E GUI \u4E2D\u751F\u6210\u6837\u677F";
            int textW = fontRendererObj.getStringWidth(emptyMsg);
            fontRendererObj
                .drawStringWithShadow(emptyMsg, contentLeft + (GUI_W - PAD * 2 - textW) / 2, y + 16, COL_EMPTY_TEXT);
        } else {
            // 统计卡片
            int statsH = 14 + 12 * 3 + CARD_PAD;
            drawCard(contentLeft, y, GUI_W - PAD * 2, statsH);
            fontRendererObj
                .drawStringWithShadow("\u00A7r\u7EDF\u8BA1", contentLeft + CARD_PAD, y + 3, COL_SECTION_TEXT);
            y += 14;

            fontRendererObj.drawStringWithShadow(
                "\u00A77\u603B\u6570: \u00A7f" + container.patternCount + " \u00A77\u4E2A\u6837\u677F",
                contentLeft + CARD_PAD,
                y,
                COL_LABEL_TEXT);
            y += 12;

            fontRendererObj.drawStringWithShadow(
                "\u00A77\u6765\u6E90: \u00A7f" + container.source,
                contentLeft + CARD_PAD,
                y,
                COL_LABEL_TEXT);
            y += 12;

            String timeStr = formatTimestamp(container.timestamp);
            fontRendererObj.drawStringWithShadow(
                "\u00A77\u751F\u6210\u65F6\u95F4: \u00A7f" + timeStr,
                contentLeft + CARD_PAD,
                y,
                COL_LABEL_TEXT);
            y += 12 + CARD_PAD + 4;

            // 预览卡片（分页）
            List<String> previews = container.previews;
            int totalPages = Math.max(1, (container.patternCount + PAGE_SIZE - 1) / PAGE_SIZE);
            if (currentPage >= totalPages) currentPage = totalPages - 1;
            if (currentPage < 0) currentPage = 0;

            int startIdx = currentPage * PAGE_SIZE;
            int endIdx = Math.min(startIdx + PAGE_SIZE, startIdx + previews.size());
            actualPreviewCount = endIdx - startIdx;

            int previewH = 14 + actualPreviewCount * 12 + 12 + CARD_PAD;
            drawCard(contentLeft, y, GUI_W - PAD * 2, previewH);
            fontRendererObj.drawStringWithShadow(
                "\u00A7r\u6837\u677F\u9884\u89C8",
                contentLeft + CARD_PAD,
                y + 3,
                COL_SECTION_TEXT);

            // 页码在标题右侧
            String pageInfo = EnumChatFormatting.DARK_GRAY + "(" + (currentPage + 1) + "/" + totalPages + ")";
            int pageInfoW = fontRendererObj.getStringWidth(pageInfo);
            fontRendererObj.drawStringWithShadow(
                pageInfo,
                contentLeft + GUI_W - PAD * 2 - CARD_PAD - pageInfoW,
                y + 3,
                COL_EMPTY_TEXT);
            y += 14;

            previewStartX = contentLeft + CARD_PAD;
            deleteButtonX = contentLeft + GUI_W - PAD * 2 - CARD_PAD - 10;

            for (int i = 0; i < actualPreviewCount; i++) {
                previewLineY[i] = y;

                int globalIdx = startIdx + i;
                String line = EnumChatFormatting.GRAY + "#" + (globalIdx + 1) + "  " + EnumChatFormatting.WHITE;
                String name = globalIdx < previews.size() ? previews.get(globalIdx) : "?";
                // 截断过长的名称 (留出删除按钮空间)
                int maxNameW = GUI_W - PAD * 2 - CARD_PAD * 2 - 20;
                if (fontRendererObj.getStringWidth(name) > maxNameW) {
                    while (fontRendererObj.getStringWidth(name + "...") > maxNameW && name.length() > 5) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name += "...";
                }
                fontRendererObj.drawStringWithShadow(line + name, previewStartX, y, COL_PREVIEW_TEXT);

                // 点击行区域 -> 打开详情
                // 这里只绘制文本，点击检测在 mouseClicked
                y += 12;
            }

            // 翻页提示
            fontRendererObj.drawStringWithShadow(
                EnumChatFormatting.DARK_GRAY + "\u5171 " + container.patternCount + " \u6761",
                previewStartX,
                y,
                COL_EMPTY_TEXT);
        }

        // 自定义按钮
        drawModernButton(btnExtract, mouseX, mouseY, true, false);
        drawModernButton(btnClear, mouseX, mouseY, false, true);

        // 翻页按钮
        if (container.patternCount > PAGE_SIZE) {
            drawModernButton(btnPrev, mouseX, mouseY, false, false);
            drawModernButton(btnNext, mouseX, mouseY, false, false);
        }

        // 状态栏
        int statusY = guiTop + GUI_H - 18;
        drawRect(guiLeft + PAD, statusY - 3, guiLeft + GUI_W - PAD, statusY - 2, COL_DIVIDER);
        drawRect(guiLeft + PAD, statusY - 2, guiLeft + GUI_W - PAD, guiTop + GUI_H - 4, COL_STATUS_BG);
        fontRendererObj.drawStringWithShadow(
            EnumChatFormatting.GRAY
                + "\u25CF \u8E72\u4E0B\u53F3\u952E\u65B9\u5757\u53EF\u76F4\u63A5\u5BFC\u51FA\u5230\u5BB9\u5668",
            guiLeft + PAD + 4,
            statusY + 1,
            0xCCCCCC);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 不绘制默认标题
    }

    // ---- 辅助绘制 ----

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

    private void drawModernButton(GuiButton btn, int mouseX, int mouseY, boolean accent, boolean danger) {
        boolean hovered = mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
            && mouseY >= btn.yPosition
            && mouseY < btn.yPosition + btn.height;

        int bgColor;
        if (danger) {
            bgColor = hovered ? COL_BTN_DANGER_HOVER : COL_BTN_DANGER;
        } else if (accent) {
            bgColor = hovered ? COL_BTN_ACCENT_HOVER : COL_BTN_ACCENT;
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

    private String formatTimestamp(long ts) {
        if (ts <= 0) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ts));
    }

    // ---- 事件处理 ----

    @Override
    protected void actionPerformed(GuiButton button) {
        // 原生按钮 visible=false, 此方法不会触发
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isMouseOver(btnExtract, mouseX, mouseY)) {
            NetworkHandler.sendStorageAction(new PacketStorageAction(PacketStorageAction.ACTION_EXTRACT));
            mc.thePlayer.closeScreen();
        } else if (isMouseOver(btnClear, mouseX, mouseY)) {
            NetworkHandler.sendStorageAction(new PacketStorageAction(PacketStorageAction.ACTION_CLEAR));
            mc.thePlayer.closeScreen();
        } else if (container.patternCount > PAGE_SIZE && isMouseOver(btnPrev, mouseX, mouseY)) {
            if (currentPage > 0) {
                currentPage--;
            }
        } else if (container.patternCount > PAGE_SIZE && isMouseOver(btnNext, mouseX, mouseY)) {
            int totalPages = (container.patternCount + PAGE_SIZE - 1) / PAGE_SIZE;
            if (currentPage < totalPages - 1) {
                currentPage++;
            }
        } else {
            // 点击行 -> 打开详情
            for (int i = 0; i < actualPreviewCount; i++) {
                // 判断 Y 轴范围 (行高 12px)
                if (mouseY >= previewLineY[i] - 1 && mouseY < previewLineY[i] + 11
                    && mouseX >= previewStartX
                    && mouseX < previewStartX + GUI_W - PAD * 2 - CARD_PAD * 2) {

                    int globalIdx = currentPage * PAGE_SIZE + i;

                    // 获取详情信息
                    com.github.ae2patterngen.storage.PatternStorage.PatternDetail detail = com.github.ae2patterngen.storage.PatternStorage
                        .getPatternDetail(mc.thePlayer.getUniqueID(), globalIdx);

                    if (detail != null) {
                        mc.displayGuiScreen(new GuiPatternDetail(this, globalIdx, detail.inputs, detail.outputs));
                    }
                    return;
                }
            }
        }
    }

    private boolean isMouseOver(GuiButton btn, int mx, int my) {
        return mx >= btn.xPosition && mx < btn.xPosition + btn.width
            && my >= btn.yPosition
            && my < btn.yPosition + btn.height;
    }
}
