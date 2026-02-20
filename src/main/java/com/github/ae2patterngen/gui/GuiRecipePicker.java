package com.github.ae2patterngen.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketRecipeConflicts;
import com.github.ae2patterngen.network.PacketResolveConflicts;
import com.github.ae2patterngen.recipe.RecipeEntry;

/**
 * 交互式配方选择 GUI
 */
public class GuiRecipePicker extends GuiScreen {

    private final PacketRecipeConflicts data;
    private static final int GUI_W = 320;
    private static final int RECIPE_H = 50;
    private int guiLeft, guiTop;

    // 颜色常量 (参考 GuiPatternGen)
    private static final int COL_BG = 0xF0181825;
    private static final int COL_BORDER = 0xFF5B89FF;
    private static final int COL_CARD = 0xFF1E1E30;

    public GuiRecipePicker(PacketRecipeConflicts data) {
        this.data = data;
    }

    public static void open(PacketRecipeConflicts message) {
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiRecipePicker(message));
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - GUI_W) / 2;
        int totalH = 40 + data.recipes.size() * (RECIPE_H + 5) + 30;
        this.guiTop = (this.height - totalH) / 2;

        buttonList.clear();
        for (int i = 0; i < data.recipes.size(); i++) {
            buttonList.add(
                new GuiButton(
                    i,
                    guiLeft + GUI_W - 60,
                    guiTop + 45 + i * (RECIPE_H + 5) + (RECIPE_H - 20) / 2,
                    50,
                    20,
                    "选择"));
        }
        buttonList.add(new GuiButton(100, guiLeft + (GUI_W - 80) / 2, guiTop + totalH - 25, 80, 20, "放弃 (Cancel)"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. 背景与边框
        int totalH = 40 + data.recipes.size() * (RECIPE_H + 5) + 30;
        drawRect(guiLeft, guiTop, guiLeft + GUI_W, guiTop + totalH, COL_BG);
        drawHollowRect(guiLeft, guiTop, GUI_W, totalH, COL_BORDER);

        // 2. 标题
        String title = "配方冲突: " + data.productName;
        drawCenteredString(fontRendererObj, title, guiLeft + GUI_W / 2, guiTop + 10, 0xFFFFFF);
        String progress = "(" + data.currentIndex + " / " + data.totalConflicts + ")";
        drawCenteredString(fontRendererObj, progress, guiLeft + GUI_W / 2, guiTop + 22, 0xAAAAAA);

        // 3. 绘制配方卡片
        RenderItem itemRender = RenderItem.getInstance();
        for (int i = 0; i < data.recipes.size(); i++) {
            int ry = guiTop + 45 + i * (RECIPE_H + 5);
            drawRect(guiLeft + 5, ry, guiLeft + GUI_W - 5, ry + RECIPE_H, COL_CARD);

            RecipeEntry re = data.recipes.get(i);

            // 绘制物品 (原料)
            int ix = guiLeft + 10;
            int iy = ry + 15;

            fontRendererObj.drawString("原料:", guiLeft + 10, ry + 5, 0x8899CC);
            drawRecipeItems(re.inputs, re.fluidInputs, ix, iy);

            // 绘制产物 (右侧)
            int ox = guiLeft + 150;
            fontRendererObj.drawString("产物:", ox, ry + 5, 0x8899CC);
            drawRecipeItems(re.outputs, re.fluidOutputs, ox, iy);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        // 4. Tooltips
        for (int i = 0; i < data.recipes.size(); i++) {
            int ry = guiTop + 45 + i * (RECIPE_H + 5);
            handleTooltips(data.recipes.get(i), mouseX, mouseY, ry + 15);
        }
    }

    private void drawRecipeItems(ItemStack[] items, FluidStack[] fluids, int startX, int startY) {
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);

        int count = 0;
        if (items != null) {
            for (ItemStack stack : items) {
                if (stack == null || count >= 7) continue;
                this.itemRender.renderItemAndEffectIntoGUI(
                    this.fontRendererObj,
                    this.mc.getTextureManager(),
                    stack,
                    startX + count * 18,
                    startY);
                this.itemRender.renderItemOverlayIntoGUI(
                    this.fontRendererObj,
                    this.mc.getTextureManager(),
                    stack,
                    startX + count * 18,
                    startY);
                count++;
            }
        }
        // 流体绘制简单化，由于没有流体渲染器，这里只画个颜色块或跳过
        // 用户只要求展示原料产物，物品通常更重要

        RenderHelper.disableStandardItemLighting();
    }

    private void handleTooltips(RecipeEntry re, int mx, int my, int startY) {
        // 原料 Tooltip
        int ix = guiLeft + 10;
        checkAndDrawTooltip(re.inputs, mx, my, ix, startY);

        // 产物 Tooltip
        int ox = guiLeft + 150;
        checkAndDrawTooltip(re.outputs, mx, my, ox, startY);
    }

    private void checkAndDrawTooltip(ItemStack[] items, int mx, int my, int startX, int startY) {
        if (items == null) return;
        for (int i = 0; i < items.length && i < 7; i++) {
            if (items[i] == null) continue;
            int x = startX + i * 18;
            if (mx >= x && mx < x + 16 && my >= startY && my < startY + 16) {
                renderToolTip(items[i], mx, my);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 100) {
            NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflicts(0, true));
            this.mc.displayGuiScreen(null);
        } else {
            NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflicts(button.id, false));
            // 不需要手动关闭，服务端会发送下一个 PacketRecipeConflicts 或 finalize
        }
    }

    private void drawHollowRect(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
