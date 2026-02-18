package com.github.ae2patterngen.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.InputOreDictFilter;
import com.github.ae2patterngen.filter.NCItemFilter;
import com.github.ae2patterngen.filter.OutputOreDictFilter;
import com.github.ae2patterngen.item.ItemPatternGenerator;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketGeneratePatterns;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 样本生成器 GUI
 * <p>
 * 功能:
 * - 配方表 ID 模糊匹配 (输入 blender 即可匹配 gt.recipe.metablender)
 * - 输入信息自动保存到物品 NBT (关闭 GUI 后保留)
 */
public class GuiPatternGen extends GuiContainer {

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;

    // 输入框
    private GuiTextField fieldRecipeMap;
    private GuiTextField fieldOutputOreDict;
    private GuiTextField fieldInputOreDict;
    private GuiTextField fieldNCItem;

    // 按钮
    private GuiButton btnPreview;
    private GuiButton btnGenerate;
    private GuiButton btnListMaps;

    // 状态
    private String statusMessage = "";

    private final ContainerPatternGen container;

    public GuiPatternGen(ContainerPatternGen container) {
        super(container);
        this.container = container;
        this.xSize = GUI_WIDTH;
        this.ySize = GUI_HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        int left = guiLeft + 10;
        int top = guiTop + 10;
        int fieldWidth = GUI_WIDTH - 20;
        int fieldHeight = 16;
        int spacing = 24;

        // 从物品 NBT 读取保存的值
        ItemStack held = container.heldItem;
        String savedRecipeMap = ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_RECIPE_MAP);
        String savedOutputOre = ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_OUTPUT_ORE);
        String savedInputOre = ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_INPUT_ORE);
        String savedNCItem = ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_NC_ITEM);

        // 配方表 ID (支持模糊匹配)
        fieldRecipeMap = new GuiTextField(fontRendererObj, left, top + 12, fieldWidth, fieldHeight);
        fieldRecipeMap.setMaxStringLength(200);
        fieldRecipeMap.setText(savedRecipeMap);
        top += spacing + 8;

        // 输出矿辞
        fieldOutputOreDict = new GuiTextField(fontRendererObj, left, top + 12, fieldWidth, fieldHeight);
        fieldOutputOreDict.setMaxStringLength(200);
        fieldOutputOreDict.setText(savedOutputOre);
        top += spacing + 8;

        // 输入矿辞
        fieldInputOreDict = new GuiTextField(fontRendererObj, left, top + 12, fieldWidth, fieldHeight);
        fieldInputOreDict.setMaxStringLength(200);
        fieldInputOreDict.setText(savedInputOre);
        top += spacing + 8;

        // NC 物品
        fieldNCItem = new GuiTextField(fontRendererObj, left, top + 12, fieldWidth, fieldHeight);
        fieldNCItem.setMaxStringLength(200);
        fieldNCItem.setText(savedNCItem);
        top += spacing + 12;

        // 按钮
        int btnWidth = 72;
        int btnHeight = 20;

        btnListMaps = new GuiButton(0, left, top, btnWidth, btnHeight, "\u5217\u51fa\u914d\u65b9\u8868");
        btnPreview = new GuiButton(1, left + btnWidth + 6, top, btnWidth, btnHeight, "\u9884\u89c8\u6570\u91cf");
        btnGenerate = new GuiButton(2, left + (btnWidth + 6) * 2, top, btnWidth, btnHeight, "\u751f\u6210\u6837\u677f");

        buttonList.add(btnListMaps);
        buttonList.add(btnPreview);
        buttonList.add(btnGenerate);
    }

    @Override
    public void onGuiClosed() {
        // 保存当前输入到物品 NBT
        saveFieldsToNBT();
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void saveFieldsToNBT() {
        if (container.heldItem != null && container.heldItem.getItem() instanceof ItemPatternGenerator) {
            // 发送网络包保存数据到服务端的物品 NBT
            NetworkHandler.sendSaveFieldsToServer(
                new com.github.ae2patterngen.network.PacketSaveFields(
                    fieldRecipeMap.getText(),
                    fieldOutputOreDict.getText(),
                    fieldInputOreDict.getText(),
                    fieldNCItem.getText()));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnListMaps) {
            handleListMaps();
        } else if (button == btnPreview) {
            handlePreview();
        } else if (button == btnGenerate) {
            handleGenerate();
        }
    }

    private void handleListMaps() {
        String keyword = fieldRecipeMap.getText()
            .trim();
        Map<String, String> maps = GTRecipeSource.getAvailableRecipeMaps();

        // 如果有输入关键字，只显示匹配的配方表
        if (!keyword.isEmpty()) {
            List<String> matched = GTRecipeSource.findMatchingRecipeMaps(keyword);
            StringBuilder sb = new StringBuilder(
                "\u00a7a\u5339\u914d\u5230 " + matched.size() + " \u4e2a\u914d\u65b9\u8868:\n");
            int count = 0;
            for (String mapId : matched) {
                sb.append(mapId)
                    .append("\n");
                count++;
                if (count >= 15) {
                    sb.append("... \u8fd8\u6709 ")
                        .append(matched.size() - 15)
                        .append(" \u4e2a");
                    break;
                }
            }
            statusMessage = sb.toString();
        } else {
            StringBuilder sb = new StringBuilder("\u53ef\u7528\u914d\u65b9\u8868 (" + maps.size() + "):\n");
            int count = 0;
            for (Map.Entry<String, String> entry : maps.entrySet()) {
                sb.append(entry.getKey())
                    .append("\n");
                count++;
                if (count >= 15) {
                    sb.append("... \u8fd8\u6709 ")
                        .append(maps.size() - 15)
                        .append(" \u4e2a");
                    break;
                }
            }
            statusMessage = sb.toString();
        }
    }

    private void handlePreview() {
        String keyword = fieldRecipeMap.getText()
            .trim();
        if (keyword.isEmpty()) {
            statusMessage = "\u00a7c\u8bf7\u8f93\u5165\u914d\u65b9\u8868\u5173\u952e\u5b57";
            return;
        }

        // 显示匹配的配方表
        List<String> matched = GTRecipeSource.findMatchingRecipeMaps(keyword);
        List<RecipeEntry> filtered = collectAndFilter();
        statusMessage = "\u00a7a\u5339\u914d " + matched.size()
            + " \u4e2a\u914d\u65b9\u8868, "
            + filtered.size()
            + " \u4e2a\u914d\u65b9";
    }

    private void handleGenerate() {
        String keyword = fieldRecipeMap.getText()
            .trim();
        if (keyword.isEmpty()) {
            statusMessage = "\u00a7c\u8bf7\u8f93\u5165\u914d\u65b9\u8868\u5173\u952e\u5b57";
            return;
        }

        // 发送网络包到服务端执行生成
        NetworkHandler.sendToServer(
            new PacketGeneratePatterns(
                keyword,
                fieldOutputOreDict.getText()
                    .trim(),
                fieldInputOreDict.getText()
                    .trim(),
                fieldNCItem.getText()
                    .trim()));

        statusMessage = "\u00a7e\u6b63\u5728\u751f\u6210\u6837\u677f...";
    }

    private List<RecipeEntry> collectAndFilter() {
        String keyword = fieldRecipeMap.getText()
            .trim();
        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(keyword);

        CompositeFilter filter = new CompositeFilter();

        String outputOre = fieldOutputOreDict.getText()
            .trim();
        if (!outputOre.isEmpty()) {
            filter.addFilter(new OutputOreDictFilter(outputOre));
        }

        String inputOre = fieldInputOreDict.getText()
            .trim();
        if (!inputOre.isEmpty()) {
            filter.addFilter(new InputOreDictFilter(inputOre));
        }

        String nc = fieldNCItem.getText()
            .trim();
        if (!nc.isEmpty()) {
            filter.addFilter(new NCItemFilter(nc));
        }

        List<RecipeEntry> filtered = new ArrayList<>();
        for (RecipeEntry recipe : recipes) {
            if (filter.matches(recipe)) {
                filtered.add(recipe);
            }
        }
        return filtered;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制背景
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xCC000000);
        drawRect(guiLeft + 1, guiTop + 1, guiLeft + xSize - 1, guiTop + ySize - 1, 0xCC333333);

        int left = guiLeft + 10;
        int top = guiTop + 10;
        int spacing = 24;

        // 标签
        fontRendererObj.drawStringWithShadow(
            "\u00a7e\u914d\u65b9\u8868 (\u652f\u6301\u6a21\u7cca\u5339\u914d):",
            left,
            top,
            0xFFFFFF);
        top += spacing + 8;
        fontRendererObj.drawStringWithShadow("\u00a7e\u8f93\u51fa\u77ff\u8f9e (\u6b63\u5219):", left, top, 0xFFFFFF);
        top += spacing + 8;
        fontRendererObj.drawStringWithShadow("\u00a7e\u8f93\u5165\u77ff\u8f9e (\u6b63\u5219):", left, top, 0xFFFFFF);
        top += spacing + 8;
        fontRendererObj.drawStringWithShadow("\u00a7eNC \u7269\u54c1 (\u6b63\u5219):", left, top, 0xFFFFFF);

        // 文本框
        fieldRecipeMap.drawTextBox();
        fieldOutputOreDict.drawTextBox();
        fieldInputOreDict.drawTextBox();
        fieldNCItem.drawTextBox();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // 状态消息
        if (!statusMessage.isEmpty()) {
            String[] lines = statusMessage.split("\n");
            int y = ySize - 10 - lines.length * 10;
            for (String line : lines) {
                fontRendererObj.drawStringWithShadow(line, 10, y, 0xFFFFFF);
                y += 10;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) {
        if (fieldRecipeMap.textboxKeyTyped(c, keyCode)) return;
        if (fieldOutputOreDict.textboxKeyTyped(c, keyCode)) return;
        if (fieldInputOreDict.textboxKeyTyped(c, keyCode)) return;
        if (fieldNCItem.textboxKeyTyped(c, keyCode)) return;
        super.keyTyped(c, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldRecipeMap.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOutputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldInputOreDict.mouseClicked(mouseX, mouseY, mouseButton);
        fieldNCItem.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        fieldRecipeMap.updateCursorCounter();
        fieldOutputOreDict.updateCursorCounter();
        fieldInputOreDict.updateCursorCounter();
        fieldNCItem.updateCursorCounter();
    }
}
