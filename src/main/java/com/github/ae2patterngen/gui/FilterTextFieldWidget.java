package com.github.ae2patterngen.gui;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.modularui.api.widget.IDragAndDropHandler;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

class FilterTextFieldWidget extends TextFieldWidget implements IDragAndDropHandler {

    private final Function<ItemStack, String> stackFormatter;
    private final Function<ItemStack, ExplicitFilterDropFormatter.DropChoices> dropChoicesBuilder;
    private Consumer<ExplicitFilterDropFormatter.DropChoices> dropChoicesListener;

    FilterTextFieldWidget() {
        this(ExplicitFilterDropFormatter::format, ExplicitFilterDropFormatter::buildChoices);
    }

    FilterTextFieldWidget(Function<ItemStack, String> stackFormatter) {
        this(stackFormatter, null);
    }

    private FilterTextFieldWidget(Function<ItemStack, String> stackFormatter,
        Function<ItemStack, ExplicitFilterDropFormatter.DropChoices> dropChoicesBuilder) {
        this.stackFormatter = stackFormatter != null ? stackFormatter : ExplicitFilterDropFormatter::format;
        this.dropChoicesBuilder = dropChoicesBuilder;
    }

    FilterTextFieldWidget setDropChoicesListener(
        Consumer<ExplicitFilterDropFormatter.DropChoices> dropChoicesListener) {
        this.dropChoicesListener = dropChoicesListener;
        return this;
    }

    void applyDropChoice(ExplicitFilterDropFormatter.DropChoice choice) {
        if (choice == null) {
            return;
        }
        setText(choice.getToken());
        markForUpdate();
    }

    @Override
    public boolean handleDragAndDrop(ItemStack draggedStack, int button) {
        if (draggedStack == null) {
            return false;
        }

        ExplicitFilterDropFormatter.DropChoices choices = dropChoicesBuilder != null
            ? dropChoicesBuilder.apply(draggedStack)
            : null;
        String formatted = choices != null ? choices.getDefaultToken() : stackFormatter.apply(draggedStack);
        if (formatted == null || formatted.trim()
            .isEmpty()) {
            return false;
        }

        setText(formatted);
        markForUpdate();
        if (dropChoicesListener != null) {
            dropChoicesListener.accept(
                choices != null && !choices.isEmpty() ? choices : ExplicitFilterDropFormatter.singleChoice(formatted));
        }
        return true;
    }
}
