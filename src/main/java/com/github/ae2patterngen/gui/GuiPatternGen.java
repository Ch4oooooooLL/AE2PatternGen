package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.github.ae2patterngen.item.ItemPatternGenerator;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketGeneratePatterns;
import com.github.ae2patterngen.network.PacketSaveFields;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * AE2 Pattern Generator GUI - 手动美化版
 * <p>
 * 模拟 AE2 1.21+ 视觉风格:
 * - 深色背景
 * - 浅蓝色发光边框
 * - 扁平化按钮
 */
public class GuiPatternGen extends GuiContainer {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 240;

    // 视觉常量
    private static final int COLOR_BG = 0xEE1E1E1E;
    private static final int COLOR_BORDER = 0xFF5B89FF;
    private static final int COLOR_INNER_BG = 0xFF111111;

    // 输入框
    private GuiTextField fieldRecipeMap;
    private GuiTextField fieldOutputOreDict;
    private GuiTextField fieldInputOreDict;
    private GuiTextField fieldNCItem;
    private GuiTextField fieldBlacklistInput;
    private GuiTextField fieldBlacklistOutput;

    // 按钮
    private GuiButton btnList;
    private GuiButton btnPreview;
    private GuiButton btnGenerate;

    private String statusMessage = "等待输入...";
    private final ContainerPatternGen container;

    public GuiPatternGen(ContainerPatternGen container) {
        super(container);
        this.container = container;
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int left = guiLeft + 10;
        int top = guiTop + 15;
        int fieldW = WIDTH - 20;
        int fieldH = 14;
        int spacing = 32;

        ItemStack held = container.heldItem;

        // 初始化输入框
        fieldRecipeMap = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_RECIPE_MAP));
        top += spacing;
        fieldOutputOreDict = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_OUTPUT_ORE));
        top += spacing;
        fieldInputOreDict = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_INPUT_ORE));
        top += spacing;
        fieldNCItem = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_NC_ITEM));
        top += spacing;
        fieldBlacklistInput = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_INPUT));
        top += spacing;
        fieldBlacklistOutput = createField(
            left,
            top + 10,
            fieldW,
            fieldH,
            ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_OUTPUT));
        top += spacing + 5;

        // 按钮
        int btnW = 75;
        int btnH = 20;
        btnList = new GuiButton(0, left, top, btnW, btnH, "列出地图");
        btnPreview = new GuiButton(1, left + 80, top, btnW, btnH, "预览数量");
        btnGenerate = new GuiButton(2, left + 160, top, btnW, btnH, "【开始生成】");

        buttonList.add(btnList);
        buttonList.add(btnPreview);
        buttonList.add(btnGenerate);
    }

    private GuiTextField createField(int x, int y, int w, int h, String initText) {
        GuiTextField field = new GuiTextField(fontRendererObj, x, y, w, h);
        field.setMaxStringLength(256);
        field.setText(initText);
        field.setCanLoseFocus(true);
        field.setTextColor(0xFFFFFF);
        return field;
    }

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
                fieldBlacklistOutput.getText()));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnList) {
            handleList();
        } else if (button == btnPreview) {
            handlePreview();
        } else if (button == btnGenerate) {
            handleGenerate();
        }
    }

    private void handleList() {
        List<String> matched = GTRecipeSource.findMatchingRecipeMaps(fieldRecipeMap.getText());
        statusMessage = EnumChatFormatting.GREEN + "匹配地图: " + matched.size();
    }

    private void handlePreview() {
        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(fieldRecipeMap.getText());
        statusMessage = EnumChatFormatting.AQUA + "过滤前配方: " + recipes.size();
    }

    private void handleGenerate() {
        if (fieldRecipeMap.getText()
            .isEmpty()) {
            statusMessage = EnumChatFormatting.RED + "错误: 配方表不可为空";
            return;
        }
        NetworkHandler.sendToServer(
            new PacketGeneratePatterns(
                fieldRecipeMap.getText(),
                fieldOutputOreDict.getText(),
                fieldInputOreDict.getText(),
                fieldNCItem.getText(),
                fieldBlacklistInput.getText(),
                fieldBlacklistOutput.getText()));
        statusMessage = EnumChatFormatting.YELLOW + "已向服务端请求生成样板";
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        // 1. 绘制 AE2 风格深色背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, COLOR_BG);

        // 2. 绘制发光蓝色边框 (模拟 1.21 风格)
        drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + ySize, COLOR_BORDER); // 左
        drawRect(guiLeft + xSize - 1, guiTop, guiLeft + xSize, guiTop + ySize, COLOR_BORDER); // 右
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 1, COLOR_BORDER); // 上
        drawRect(guiLeft, guiTop + ySize - 1, guiLeft + xSize, guiTop + ySize, COLOR_BORDER); // 下

        // 3. 绘制标签和输入框背景
        int left = guiLeft + 10;
        int top = guiTop + 15;
        int spacing = 32;

        drawFieldLabel("配方表 (模糊匹配):", left, top);
        drawFieldBg(fieldRecipeMap);
        top += spacing;

        drawFieldLabel("输出矿辞过滤器:", left, top);
        drawFieldBg(fieldOutputOreDict);
        top += spacing;

        drawFieldLabel("输入矿辞过滤器:", left, top);
        drawFieldBg(fieldInputOreDict);
        top += spacing;

        drawFieldLabel("NC 物品 (ID:Meta / ID\\Meta):", left, top);
        drawFieldBg(fieldNCItem);
        top += spacing;

        drawFieldLabel("输入黑名单 (排除):", left, top);
        drawFieldBg(fieldBlacklistInput);
        top += spacing;

        drawFieldLabel("输出黑名单 (排除):", left, top);
        drawFieldBg(fieldBlacklistOutput);

        // 绘制状态指示器
        int statusY = guiTop + ySize - 18;
        fontRendererObj.drawStringWithShadow(statusMessage, left, statusY, 0xCCCCCC);
    }

    private void drawFieldLabel(String text, int x, int y) {
        fontRendererObj.drawString(EnumChatFormatting.GRAY + text, x, y, 0xBBBBBB);
    }

    private void drawFieldBg(GuiTextField field) {
        drawRect(
            field.xPosition - 1,
            field.yPosition - 1,
            field.xPosition + field.width + 1,
            field.yPosition + field.height + 1,
            0xFF444444);
        drawRect(
            field.xPosition,
            field.yPosition,
            field.xPosition + field.width,
            field.yPosition + field.height,
            COLOR_INNER_BG);
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldRecipeMap.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOutputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldInputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldNCItem.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBlacklistInput.mouseClicked(mouseX, mouseY, mouseButton);
        fieldBlacklistOutput.mouseClicked(mouseX, mouseY, mouseButton);
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
