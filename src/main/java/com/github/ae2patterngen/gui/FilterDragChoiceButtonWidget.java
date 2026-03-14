package com.github.ae2patterngen.gui;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.modularui.api.widget.IDragAndDropHandler;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;

class FilterDragChoiceButtonWidget extends ButtonWidget implements IDragAndDropHandler {

    private final FilterTextFieldWidget delegate;

    FilterDragChoiceButtonWidget(FilterTextFieldWidget delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean handleDragAndDrop(ItemStack draggedStack, int button) {
        return delegate != null && delegate.handleDragAndDrop(draggedStack, button);
    }
}
