package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.github.ae2patterngen.item.ItemPatternGenerator;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketGeneratePatterns;
import com.github.ae2patterngen.network.PacketSaveFields;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * AE2 Pattern Generator GUI — 现代化重构版
 * <p>
 * 分组卡片式布局 + AE2 1.21+ 深色极简风格
 */
public class GuiPatternGen extends GuiContainer {

    // ---- 尺寸常量 ----
    private static final int GUI_W = 260;
    private static final int GUI_H = 315;

    // ---- 调色板 ----
    private static final int COL_PANEL_BG = 0xF0181825; // 主面板深蓝灰背景
    private static final int COL_BORDER_OUTER = 0xFF2A2A44; // 外框暗色
    private static final int COL_BORDER_GLOW = 0xFF5B89FF; // 发光蓝
    private static final int COL_CARD_BG = 0xFF1E1E30; // 卡片背景
    private static final int COL_CARD_BORDER = 0xFF33335A; // 卡片边框
    private static final int COL_INPUT_BG = 0xFF0D0D1A; // 输入框底色
    private static final int COL_INPUT_BORDER = 0xFF3A3A5C; // 输入框边框
    private static final int COL_TITLE_TEXT = 0xFFFFFFFF; // 标题白色
    private static final int COL_SECTION_TEXT = 0xFF8899CC; // 分组标题蓝灰
    private static final int COL_LABEL_TEXT = 0xFFAABBDD; // 标签浅蓝灰
    private static final int COL_STATUS_BG = 0xFF14141F; // 状态栏背景
    private static final int COL_DIVIDER = 0xFF33335A; // 分隔线
    private static final int COL_BTN_NORMAL = 0xFF2E3B5A; // 按钮默认
    private static final int COL_BTN_HOVER = 0xFF3D4F7A; // 按钮悬停
    private static final int COL_BTN_ACCENT = 0xFF2A5DB0; // 生成按钮强调色
    private static final int COL_BTN_ACCENT_HOVER = 0xFF3A70CC; // 生成按钮悬停

    // ---- 内边距 ----
    private static final int PAD = 8; // 面板内边距
    private static final int CARD_PAD = 6; // 卡片内边距
    private static final int FIELD_H = 14; // 输入框高度
    private static final int LABEL_W = 80; // 标签宽度

    // ---- UI 元素 ----
    private GuiTextField fieldRecipeMap;
    private GuiTextField fieldOutputOreDict;
    private GuiTextField fieldInputOreDict;
    private GuiTextField fieldNCItem;
    private GuiTextField fieldBlacklistInput;
    private GuiTextField fieldBlacklistOutput;

    private GuiComboBox comboTier;
    private static final java.util.List<String> TIER_OPTIONS = java.util.Arrays.asList(
        "Any",
        "ULV",
        "LV",
        "MV",
        "HV",
        "EV",
        "IV",
        "LuV",
        "ZPM",
        "UV",
        "UHV",
        "UEV",
        "UIV",
        "UMV",
        "UXV",
        "MAX");

    private GuiButton btnList;
    private GuiButton btnPreview;
    private GuiButton btnGenerate;
    private GuiButton btnOpenConfig;

    private String statusMessage = "\u00A77\u25CF \u5c31\u7eea"; // ● 就绪
    private final ContainerPatternGen container;
    private int loadedRuleCount = 0;

    public GuiPatternGen(ContainerPatternGen container) {
        super(container);
        this.container = container;
        this.xSize = GUI_W;
        this.ySize = GUI_H;
    }

    // ==================================================================
    // initGui — 创建所有控件
    // ==================================================================
    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        // 加载矿辞替换规则配置
        this.loadedRuleCount = com.github.ae2patterngen.config.ReplacementConfig.load();
        this.statusMessage = "\u00A77\u25CF \u5DF2\u52A0\u8F7D " + loadedRuleCount + " \u6761\u66FF\u6362\u89C4\u5219";

        ItemStack held = container.heldItem;

        // ---- 标题栏: 20px ----
        int contentLeft = guiLeft + PAD;
        int contentRight = guiLeft + GUI_W - PAD;
        int fieldW = contentRight - contentLeft - LABEL_W - CARD_PAD * 2;
        int fullFieldW = contentRight - contentLeft - CARD_PAD * 2;

        int y = guiTop + 24; // 标题栏下方开始

        // ---- 卡片 1: 配方设置 (单行，全宽输入框) ----
        y += 14; // 分组标题行
        fieldRecipeMap = createField(
            contentLeft + CARD_PAD,
            y,
            fullFieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_RECIPE_MAP));
        y += FIELD_H + CARD_PAD;
        y += 4; // 卡片间距

        // ---- 卡片 2: 过滤器 (3 行, 标签|输入框) ----
        y += 14; // 分组标题行
        int inputX = contentLeft + CARD_PAD + LABEL_W;

        fieldOutputOreDict = createField(
            inputX,
            y,
            fieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_OUTPUT_ORE));
        y += FIELD_H + 4;

        fieldInputOreDict = createField(
            inputX,
            y,
            fieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_INPUT_ORE));
        y += FIELD_H + 4;

        fieldNCItem = createField(
            inputX,
            y,
            fieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_NC_ITEM));
        y += FIELD_H + 4;

        // Tier Dropdown
        int savedTier = ItemPatternGenerator.getSavedInt(held, ItemPatternGenerator.NBT_TARGET_TIER, -1);
        comboTier = new GuiComboBox(inputX, y, fieldW, FIELD_H, TIER_OPTIONS);
        comboTier.setSelectedIndex(savedTier + 1); // -1 -> 0 (Any), 0 -> 1 (ULV)

        y += FIELD_H + CARD_PAD;
        y += 4;

        // ---- 卡片 3: 黑名单 (2 行) ----
        y += 14;

        fieldBlacklistInput = createField(
            inputX,
            y,
            fieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_INPUT));
        y += FIELD_H + 4;

        fieldBlacklistOutput = createField(
            inputX,
            y,
            fieldW,
            FIELD_H,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_OUTPUT));
        y += FIELD_H + CARD_PAD;
        y += 4;

        // ---- 卡片 4: 替换规则 (1 行, 全宽) ----
        y += 14;

        // ---- 卡片 4: 替换规则 (信息显示 + 按钮) ----
        y += 14;

        int configBtnW = 80;
        int configBtnH = 16;
        btnOpenConfig = new GuiButton(
            3,
            contentLeft + GUI_W - PAD * 2 - CARD_PAD - configBtnW,
            y,
            configBtnW,
            configBtnH,
            "\u6253\u5F00\u914D\u7F6E");

        y += 20 + CARD_PAD;
        y += 8;

        // ---- 按钮行 ----
        int btnW = 76;
        int btnH = 20;
        int totalBtnW = btnW * 3 + 6;
        int btnStartX = guiLeft + (GUI_W - totalBtnW) / 2;

        btnList = new GuiButton(0, btnStartX, y, btnW, btnH, "\u5217\u51FA\u5730\u56FE");
        btnPreview = new GuiButton(1, btnStartX + btnW + 3, y, btnW, btnH, "\u9884\u89C8\u6570\u91CF");
        btnGenerate = new GuiButton(2, btnStartX + (btnW + 3) * 2, y, btnW, btnH, "\u25B6 \u751F\u6210\u6837\u677F");

        buttonList.add(btnList);
        buttonList.add(btnPreview);
        buttonList.add(btnGenerate);
        buttonList.add(btnOpenConfig);
    }

    private GuiTextField createField(int x, int y, int w, int h, String initText) {
        GuiTextField field = new GuiTextField(fontRendererObj, x, y, w, h);
        field.setMaxStringLength(512);
        field.setText(initText);
        field.setCanLoseFocus(true);
        field.setTextColor(0xFFFFFF);
        field.setEnableBackgroundDrawing(false); // 我们自己绘制背景
        return field;
    }

    // ==================================================================
    // 绘制
    // ==================================================================
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // ---- 1. 主面板 ----
        drawRect(guiLeft, guiTop, guiLeft + GUI_W, guiTop + GUI_H, COL_PANEL_BG);

        // ---- 2. 发光双层边框 ----
        drawHollowRect(guiLeft, guiTop, GUI_W, GUI_H, COL_BORDER_OUTER);
        drawHollowRect(guiLeft + 1, guiTop + 1, GUI_W - 2, GUI_H - 2, COL_BORDER_GLOW);

        // ---- 3. 标题栏 ----
        int titleY = guiTop + 6;
        fontRendererObj.drawStringWithShadow("\u25B8 AE2 Pattern Generator", guiLeft + PAD + 2, titleY, COL_TITLE_TEXT);

        // 标题下分隔线
        drawRect(guiLeft + PAD, guiTop + 18, guiLeft + GUI_W - PAD, guiTop + 19, COL_BORDER_GLOW);

        // ---- 4. 内容区域 ----
        int contentLeft = guiLeft + PAD;

        int y = guiTop + 24;

        // -- 卡片 1: 配方设置 --
        int card1H = 14 + FIELD_H + CARD_PAD;
        drawCard(contentLeft, y, GUI_W - PAD * 2, card1H);
        fontRendererObj
            .drawStringWithShadow("\u00A7r\u914D\u65B9\u8BBE\u7F6E", contentLeft + CARD_PAD, y + 3, COL_SECTION_TEXT);
        y += 14;
        drawInputBg(fieldRecipeMap);
        y += FIELD_H + CARD_PAD + 4;

        // -- 卡片 2: 过滤器 --
        // 原有 3 行 + 新增 1 行
        int card2H = 14 + (FIELD_H + 4) * 4 - 4 + CARD_PAD;
        drawCard(contentLeft, y, GUI_W - PAD * 2, card2H);
        fontRendererObj
            .drawStringWithShadow("\u00A7r\u8FC7\u6EE4\u5668", contentLeft + CARD_PAD, y + 3, COL_SECTION_TEXT);
        y += 14;

        drawLabelAndInputBg("\u8F93\u51FA\u77FF\u8F9E:", contentLeft + CARD_PAD, y, fieldOutputOreDict);
        y += FIELD_H + 4;
        drawLabelAndInputBg("\u8F93\u5165\u77FF\u8F9E:", contentLeft + CARD_PAD, y, fieldInputOreDict);
        y += FIELD_H + 4;
        drawLabelAndInputBg("NC \u7269\u54C1:", contentLeft + CARD_PAD, y, fieldNCItem);
        y += FIELD_H + 4;

        // Draw Label for Tier
        fontRendererObj
            .drawStringWithShadow("\u7535\u538B\u7B49\u7EA7:", contentLeft + CARD_PAD, y + 3, COL_LABEL_TEXT);
        // ComboBox drawn later to overlay

        y += FIELD_H + CARD_PAD + 4;

        // -- 卡片 3: 黑名单 --
        int card3H = 14 + (FIELD_H + 4) * 2 - 4 + CARD_PAD;
        drawCard(contentLeft, y, GUI_W - PAD * 2, card3H);
        fontRendererObj
            .drawStringWithShadow("\u00A7r\u9ED1\u540D\u5355", contentLeft + CARD_PAD, y + 3, COL_SECTION_TEXT);
        y += 14;

        drawLabelAndInputBg("\u8F93\u5165\u6392\u9664:", contentLeft + CARD_PAD, y, fieldBlacklistInput);
        y += FIELD_H + 4;
        drawLabelAndInputBg("\u8F93\u51FA\u6392\u9664:", contentLeft + CARD_PAD, y, fieldBlacklistOutput);
        y += FIELD_H + CARD_PAD + 4;

        // -- 卡片 4: 替换规则 --
        // -- 卡片 4: 替换规则 --
        int card4H = 14 + 20 + CARD_PAD;
        drawCard(contentLeft, y, GUI_W - PAD * 2, card4H);
        fontRendererObj
            .drawStringWithShadow("\u00A7r\u66FF\u6362\u89C4\u5219", contentLeft + CARD_PAD, y + 3, COL_SECTION_TEXT);

        // 显示规则统计
        String ruleInfo = "\u00A77\u5DF2\u52A0\u8F7D: \u00A7f" + loadedRuleCount + " \u00A77\u6761";
        fontRendererObj.drawStringWithShadow(ruleInfo, contentLeft + CARD_PAD, y + 14 + 5, COL_LABEL_TEXT);

        // 按钮在 initGui 已定位，这里只需要绘制
        drawModernButton(btnOpenConfig, mouseX, mouseY, false);

        // ---- 5. 自定义按钮绘制 ----
        drawModernButton(btnList, mouseX, mouseY, false);
        drawModernButton(btnPreview, mouseX, mouseY, false);
        drawModernButton(btnGenerate, mouseX, mouseY, true);

        // ---- 6. 状态栏 ----
        int statusY = guiTop + GUI_H - 18;
        drawRect(guiLeft + PAD, statusY - 3, guiLeft + GUI_W - PAD, statusY - 2, COL_DIVIDER);
        drawRect(guiLeft + PAD, statusY - 2, guiLeft + GUI_W - PAD, guiTop + GUI_H - 4, COL_STATUS_BG);
        fontRendererObj.drawStringWithShadow(statusMessage, guiLeft + PAD + 4, statusY + 1, 0xCCCCCC);
    }

    // ==================================================================
    // 辅助绘制方法
    // ==================================================================
    private void drawHollowRect(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color); // 上
        drawRect(x, y + h - 1, x + w, y + h, color); // 下
        drawRect(x, y, x + 1, y + h, color); // 左
        drawRect(x + w - 1, y, x + w, y + h, color); // 右
    }

    private void drawCard(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, COL_CARD_BG);
        drawHollowRect(x, y, w, h, COL_CARD_BORDER);
    }

    private void drawInputBg(GuiTextField field) {
        int x = field.xPosition - 2;
        int y = field.yPosition - 2;
        int w = field.width + 4;
        int h = field.height + 4;
        drawRect(x, y, x + w, y + h, COL_INPUT_BORDER);
        drawRect(x + 1, y + 1, x + w - 1, y + h - 1, COL_INPUT_BG);
    }

    private void drawLabelAndInputBg(String label, int labelX, int y, GuiTextField field) {
        fontRendererObj.drawStringWithShadow(label, labelX, y + 3, COL_LABEL_TEXT);
        drawInputBg(field);
    }

    private void drawModernButton(GuiButton btn, int mouseX, int mouseY, boolean accent) {
        boolean hovered = mouseX >= btn.xPosition && mouseX < btn.xPosition + btn.width
            && mouseY >= btn.yPosition
            && mouseY < btn.yPosition + btn.height;

        int bgColor;
        if (accent) {
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

        // 隐藏原生按钮渲染
        btn.visible = false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 不绘制默认标题
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        GL11.glDisable(GL11.GL_LIGHTING);
        fieldRecipeMap.drawTextBox();
        fieldOutputOreDict.drawTextBox();
        fieldInputOreDict.drawTextBox();
        fieldNCItem.drawTextBox();
        fieldBlacklistInput.drawTextBox();
        fieldBlacklistOutput.drawTextBox();

        // Draw ComboBox last
        comboTier.drawComboBox(mc, mouseX, mouseY);
    }

    // ==================================================================
    // 事件处理
    // ==================================================================
    @Override
    public void onGuiClosed() {
        saveFields();
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void saveFields() {
        NetworkHandler.sendSaveFieldsToServer(
            new PacketSaveFields(
                fieldRecipeMap.getText(),
                fieldOutputOreDict.getText(),
                fieldInputOreDict.getText(),
                fieldNCItem.getText(),
                fieldBlacklistInput.getText(),
                fieldBlacklistOutput.getText(),
                "", // replacements (managed by server config)
                comboTier.getSelectedIndex() - 1 // Index 0 (Any) -> -1
            ));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        // 由于 btn.visible=false，原生点击检测失效，需手动处理
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (comboTier.mouseClicked(mouseX, mouseY, mouseButton)) return; // Handle combo click first
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // 手动按钮点击检测
        if (isMouseOver(btnList, mouseX, mouseY)) {
            handleList();
        } else if (isMouseOver(btnPreview, mouseX, mouseY)) {
            handlePreview();
        } else if (isMouseOver(btnGenerate, mouseX, mouseY)) {
            handleGenerate();
        } else if (isMouseOver(btnOpenConfig, mouseX, mouseY)) {
            try {
                java.awt.Desktop.getDesktop()
                    .open(com.github.ae2patterngen.config.ReplacementConfig.getConfigFile());
            } catch (Exception e) {
                statusMessage = EnumChatFormatting.RED + "\u6253\u5F00\u5931\u8D25: " + e.getMessage();
            }
        }

        fieldRecipeMap.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOutputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldInputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldNCItem.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBlacklistInput.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBlacklistOutput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isMouseOver(GuiButton btn, int mx, int my) {
        return mx >= btn.xPosition && mx < btn.xPosition + btn.width
            && my >= btn.yPosition
            && my < btn.yPosition + btn.height;
    }

    private void handleList() {
        List<String> matched = GTRecipeSource.findMatchingRecipeMaps(fieldRecipeMap.getText());
        statusMessage = EnumChatFormatting.GREEN + "\u25CF \u5339\u914D\u5730\u56FE: " + matched.size();
    }

    private void handlePreview() {
        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(fieldRecipeMap.getText());
        statusMessage = EnumChatFormatting.AQUA + "\u25CF \u8FC7\u6EE4\u524D\u914D\u65B9: " + recipes.size();
    }

    private void handleGenerate() {
        if (fieldRecipeMap.getText()
            .isEmpty()) {
            statusMessage = EnumChatFormatting.RED + "\u25CF \u9519\u8BEF: \u914D\u65B9\u8868\u4E0D\u53EF\u4E3A\u7A7A";
            return;
        }
        NetworkHandler.sendToServer(
            new PacketGeneratePatterns(
                fieldRecipeMap.getText(),
                fieldOutputOreDict.getText(),
                fieldInputOreDict.getText(),
                fieldNCItem.getText(),
                fieldBlacklistInput.getText(),
                fieldBlacklistOutput.getText(),
                "", // replacements (server config)
                comboTier.getSelectedIndex() - 1));
        statusMessage = EnumChatFormatting.YELLOW
            + "\u25CF \u5DF2\u5411\u670D\u52A1\u7AEF\u8BF7\u6C42\u751F\u6210\u6837\u677F";
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (fieldRecipeMap.textboxKeyTyped(c, keyCode) || fieldOutputOreDict.textboxKeyTyped(c, keyCode)
            || fieldInputOreDict.textboxKeyTyped(c, keyCode)
            || fieldNCItem.textboxKeyTyped(c, keyCode)
            || fieldBlacklistInput.textboxKeyTyped(c, keyCode)
            || fieldBlacklistOutput.textboxKeyTyped(c, keyCode)) {
            return;
        }
        super.keyTyped(c, keyCode);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            comboTier.handleMouseWheel(dWheel);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        fieldRecipeMap.updateCursorCounter();
        fieldOutputOreDict.updateCursorCounter();
        fieldInputOreDict.updateCursorCounter();
        fieldNCItem.updateCursorCounter();
        fieldBlacklistInput.updateCursorCounter();
        fieldBlacklistOutput.updateCursorCounter();

    }
}
