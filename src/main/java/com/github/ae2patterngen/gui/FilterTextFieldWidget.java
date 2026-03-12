package com.github.ae2patterngen.gui;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.modularui.api.widget.IDragAndDropHandler;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

class FilterTextFieldWidget extends TextFieldWidget implements IDragAndDropHandler {

    private final Function<ItemStack, String> stackFormatter;

    FilterTextFieldWidget() {
        this(ExplicitFilterDropFormatter::format);
    }

    FilterTextFieldWidget(Function<ItemStack, String> stackFormatter) {
        this.stackFormatter = stackFormatter != null ? stackFormatter : ExplicitFilterDropFormatter::format;
    }

    @Override
    public boolean handleDragAndDrop(ItemStack draggedStack, int button) {
        if (draggedStack == null) {
            return false;
        }

        String formatted = stackFormatter.apply(draggedStack);
        if (formatted == null || formatted.trim().isEmpty()) {
            return false;
        }

        setText(formatted);
        markForUpdate();
        return true;
    }
}
