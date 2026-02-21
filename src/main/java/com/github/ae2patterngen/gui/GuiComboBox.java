package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

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

    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ITEMS = 8;

    // Light Theme Colors (1.21 AE Style)
    private static final int COL_BG = 0xFFFFFFFF;
    private static final int COL_BORDER = 0xFF373737;
    private static final int COL_HOVER = 0xFFE0E0E0;
    private static final int COL_TEXT = 0xFF404040;
    private static final int COL_TEXT_DISABLED = 0xFFAAAAAA;
    private static final int COL_SCROLLBAR = 0xFF8B8B8B;
    private static final int COL_BTN_BG = 0xFFD6D6D6;

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

    public boolean isExpanded() {
        return isExpanded;
    }

    public void drawComboBox(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, COL_BTN_BG);
        drawHollowRect(xPosition, yPosition, width, height, COL_BORDER);

        String text = getSelectedValue();
        int textColor = isEnabled ? COL_TEXT : COL_TEXT_DISABLED;
        mc.fontRenderer.drawString(text, xPosition + 4, yPosition + (height - 8) / 2, textColor);

        String arrow = isExpanded ? "\u25B2" : "\u25BC";
        mc.fontRenderer.drawString(arrow, xPosition + width - 12, yPosition + (height - 8) / 2, COL_TEXT_DISABLED);
    }

    public void drawComboBoxList(Minecraft mc, int mouseX, int mouseY, int translateOffsetY) {
        if (!this.visible || !this.isExpanded) return;

        GL11.glPushMatrix();
        GL11.glTranslatef(0, translateOffsetY, 300);

        int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int listH = visibleCount * height;
        int listY = yPosition + height;

        drawRect(xPosition, listY, xPosition + width, listY + listH, COL_BG);
        drawHollowRect(xPosition, listY, width, listH, COL_BORDER);

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
                optText = EnumChatFormatting.DARK_GREEN + optText;
            }

            // Clean the formatting just to place our own colors
            mc.fontRenderer.drawString(optText, xPosition + 4, optY + (height - 8) / 2, COL_TEXT);
        }

        if (options.size() > MAX_VISIBLE_ITEMS) {
            int scrollTrackH = listH - 4;
            float ratio = (float) MAX_VISIBLE_ITEMS / options.size();
            int thumbH = Math.max(10, (int) (ratio * scrollTrackH));
            float scrollProgress = (float) scrollOffset / (options.size() - MAX_VISIBLE_ITEMS);
            int thumbY = listY + 2 + (int) (scrollProgress * (scrollTrackH - thumbH));
            drawRect(xPosition + width - 4, thumbY, xPosition + width - 1, thumbY + thumbH, COL_SCROLLBAR);
        }

        GL11.glPopMatrix();
    }

    public void handleMouseWheel(int dWheel) {
        if (!isExpanded || options.size() <= MAX_VISIBLE_ITEMS) return;

        if (dWheel > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (dWheel < 0) {
            scrollOffset = Math.min(options.size() - MAX_VISIBLE_ITEMS, scrollOffset + 1);
        }
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!this.visible || !this.isEnabled) return false;

        if (mouseX >= xPosition && mouseX < xPosition + width && mouseY >= yPosition && mouseY < yPosition + height) {
            isExpanded = !isExpanded;
            if (isExpanded) {
                scrollOffset = Math
                    .max(0, Math.min(selectedIndex - MAX_VISIBLE_ITEMS / 2, options.size() - MAX_VISIBLE_ITEMS));
            }
            Minecraft.getMinecraft().thePlayer.playSound("gui.button.press", 1.0F, 1.0F);
            return true;
        }

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

            isExpanded = false;
        }

        return false;
    }

    private void drawHollowRect(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }
}
