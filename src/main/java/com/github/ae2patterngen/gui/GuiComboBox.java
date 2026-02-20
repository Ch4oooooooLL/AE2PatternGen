package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

/**
 * 简单的自定义下拉框控件 (Modern Style)
 */
public class GuiComboBox extends Gui {

    private final int xPosition;
    private final int yPosition;
    private final int width;
    private final int height;
    private final List<String> options;

    private int selectedIndex = 0;
    private boolean isExpanded = false;
    private boolean isEnabled = true;
    private boolean visible = true;

    // Colors
    private static final int COL_BG = 0xFF1E1E30;
    private static final int COL_BORDER = 0xFF33335A;
    private static final int COL_HOVER = 0xFF2E3B5A;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DISABLED = 0xFFAAAAAA;

    public GuiComboBox(int x, int y, int width, int height, List<String> options) {
        this.xPosition = x;
        this.yPosition = y;
        this.width = width;
        this.height = height;
        this.options = options;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            this.selectedIndex = index;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedValue() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return "";
    }

    public void drawComboBox(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible)
            return;

        // Draw main box
        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, COL_BG);
        drawHollowRect(xPosition, yPosition, width, height, COL_BORDER);

        // Draw selected text
        String text = getSelectedValue();
        int textColor = isEnabled ? COL_TEXT : COL_TEXT_DISABLED;
        mc.fontRenderer.drawStringWithShadow(text, xPosition + 4, yPosition + (height - 8) / 2, textColor);

        // Draw arrow
        String arrow = isExpanded ? "\u25B2" : "\u25BC"; // Up/Down triangle
        mc.fontRenderer.drawStringWithShadow(arrow, xPosition + width - 12, yPosition + (height - 8) / 2, 0xAAAAAA);

        // Draw expanded list if open
        if (isExpanded) {
            // Draw list above other elements (z-level)
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 300); // Elevate z-level

            int listH = options.size() * height;
            int listY = yPosition + height;

            // Background
            drawRect(xPosition, listY, xPosition + width, listY + listH, COL_BG);
            drawHollowRect(xPosition, listY, width, listH, COL_BORDER);

            for (int i = 0; i < options.size(); i++) {
                int optY = listY + i * height;
                boolean hovered = mouseX >= xPosition && mouseX < xPosition + width && mouseY >= optY
                        && mouseY < optY + height;

                if (hovered) {
                    drawRect(xPosition + 1, optY, xPosition + width - 1, optY + height, COL_HOVER);
                }

                String optText = options.get(i);
                if (i == selectedIndex) {
                    optText = EnumChatFormatting.YELLOW + optText;
                }

                mc.fontRenderer.drawStringWithShadow(optText, xPosition + 4, optY + (height - 8) / 2, COL_TEXT);
            }

            GL11.glPopMatrix();
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.visible || !this.isEnabled)
            return false;

        // Check toggle
        if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height) {
            isExpanded = !isExpanded;
            // Play sound
            Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1.0F, 1.0F);
            return true;
        }

        // Check selection if expanded
        if (isExpanded) {
            int listH = options.size() * height;
            int listY = yPosition + height;

            if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= listY && mouseY < listY + listH) {
                int idx = (mouseY - listY) / height;
                if (idx >= 0 && idx < options.size()) {
                    selectedIndex = idx;
                    isExpanded = false;
                    // Play sound
                    Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1.0F, 1.0F);
                    return true;
                }
            }

            // Click outside -> close
            isExpanded = false;
        }

        return false;
    }

    private void drawHollowRect(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color); // Top
        drawRect(x, y + h - 1, x + w, y + h, color); // Bottom
        drawRect(x, y, x + 1, y + h, color); // Left
        drawRect(x + w - 1, y, x + w, y + h, color); // Right
    }
}
