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

    // Scrolling support
    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ITEMS = 8;

    // Colors
    private static final int COL_BG = 0xFF1E1E30;
    private static final int COL_BORDER = 0xFF33335A;
    private static final int COL_HOVER = 0xFF2E3B5A;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DISABLED = 0xFFAAAAAA;
    private static final int COL_SCROLLBAR = 0xFF5B89FF;

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
        if (!this.visible) return;

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
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 300); // Elevate z-level

            int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
            int listH = visibleCount * height;
            int listY = yPosition + height;

            // Background
            drawRect(xPosition, listY, xPosition + width, listY + listH, COL_BG);
            drawHollowRect(xPosition, listY, width, listH, COL_BORDER);

            // Draw items
            for (int i = 0; i < visibleCount; i++) {
                int actualIdx = i + scrollOffset;
                if (actualIdx >= options.size()) break;

                int optY = listY + i * height;
                boolean hovered = mouseX >= xPosition && mouseX < xPosition + width
                    && mouseY >= optY
                    && mouseY < optY + height;

                if (hovered) {
                    drawRect(xPosition + 1, optY, xPosition + width - 1, optY + height, COL_HOVER);
                }

                String optText = options.get(actualIdx);
                if (actualIdx == selectedIndex) {
                    optText = EnumChatFormatting.YELLOW + optText;
                }

                mc.fontRenderer.drawStringWithShadow(optText, xPosition + 4, optY + (height - 8) / 2, COL_TEXT);
            }

            // Draw Scrollbar hint if needed
            if (options.size() > MAX_VISIBLE_ITEMS) {
                int scrollTrackH = listH - 4;
                int thumbH = Math.max(10, (int) ((float) MAX_VISIBLE_ITEMS / options.size() * scrollTrackH));
                int thumbY = listY + 2
                    + (int) ((float) scrollOffset / (options.size() - MAX_VISIBLE_ITEMS) * (scrollTrackH - thumbH));
                drawRect(xPosition + width - 3, thumbY, xPosition + width - 1, thumbY + thumbH, COL_SCROLLBAR);
            }

            GL11.glPopMatrix();
        }
    }

    public void handleMouseWheel(int dWheel) {
        if (!isExpanded || options.size() <= MAX_VISIBLE_ITEMS) return;

        if (dWheel > 0) { // Scroll up
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (dWheel < 0) { // Scroll down
            scrollOffset = Math.min(options.size() - MAX_VISIBLE_ITEMS, scrollOffset + 1);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.visible || !this.isEnabled) return false;

        // Check toggle
        if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height) {
            isExpanded = !isExpanded;
            if (isExpanded) {
                // Reset scroll when opening
                scrollOffset = Math
                    .max(0, Math.min(selectedIndex - MAX_VISIBLE_ITEMS / 2, options.size() - MAX_VISIBLE_ITEMS));
            }
            Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1.0F, 1.0F);
            return true;
        }

        // Check selection if expanded
        if (isExpanded) {
            int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
            int listH = visibleCount * height;
            int listY = yPosition + height;

            if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= listY && mouseY < listY + listH) {
                int relativeIdx = (mouseY - listY) / height;
                int actualIdx = relativeIdx + scrollOffset;
                if (actualIdx >= 0 && actualIdx < options.size()) {
                    selectedIndex = actualIdx;
                    isExpanded = false;
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
