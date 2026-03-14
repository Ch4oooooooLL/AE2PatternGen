package com.github.ae2patterngen.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.config.ForgeConfig;
import com.github.ae2patterngen.item.ItemPatternGenerator;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketCreateCache;
import com.github.ae2patterngen.network.PacketGeneratePatterns;
import com.github.ae2patterngen.network.PacketPreviewRecipeCount;
import com.github.ae2patterngen.network.PacketSaveFields;
import com.github.ae2patterngen.util.I18nUtil;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.Text;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

public class GuiPatternGen {

    private static final int DRAG_SELECTOR_W = 36;
    private static final int DRAG_SELECTOR_BG = 0xEE2A2A42;

    public static ModularWindow createWindow(UIBuildContext buildContext, ItemStack held) {
        int guiWidth = ForgeConfig.getPatternGenGuiWidth();
        int guiHeight = ForgeConfig.getPatternGenGuiHeight();

        ModularWindow.Builder builder = ModularWindow.builder(guiWidth, guiHeight);

        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.title"));
        titleText.setScale(1.2f);
        titleText.setSize(guiWidth - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 24);
        scrollable.setSize(guiWidth - 16, guiHeight - 24 - 40);

        int refY = 0;
        int fieldW = guiWidth - 16 - 80 - 12;
        int fullFieldW = guiWidth - 16 - 12;
        int inputX = 80;

        TextWidget labelRecipe = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.section.recipe"));
        labelRecipe.setPos(6, refY + 3);
        scrollable.widget(labelRecipe);

        TextFieldWidget tfRecipeMap = new TextFieldWidget();
        tfRecipeMap.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_RECIPE_MAP));
        tfRecipeMap.setPos(6, refY + 14);
        tfRecipeMap.setSize(fullFieldW, 14);
        tfRecipeMap.setTextColor(0xFFFFFF);
        tfRecipeMap.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfRecipeMap.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfRecipeMap);
        refY += 38;

        TextWidget labelFilter = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.section.filter"));
        labelFilter.setPos(6, refY + 3);
        scrollable.widget(labelFilter);

        TextWidget labelOutOre = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.output_ore"));
        labelOutOre.setPos(6, refY + 14 + 3);
        scrollable.widget(labelOutOre);

        FilterTextFieldWidget tfOutputOre = new FilterTextFieldWidget();
        tfOutputOre.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_OUTPUT_ORE));
        tfOutputOre.setPos(inputX, refY + 14);
        tfOutputOre.setSize(fieldW, 14);
        tfOutputOre.setTextColor(0xFFFFFF);
        tfOutputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfOutputOre.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfOutputOre);
        attachDragChoiceSelector(scrollable, tfOutputOre, inputX, refY + 14, fieldW);

        TextWidget labelInOre = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.input_ore"));
        labelInOre.setPos(6, refY + 32 + 3);
        scrollable.widget(labelInOre);

        FilterTextFieldWidget tfInputOre = new FilterTextFieldWidget();
        tfInputOre.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_INPUT_ORE));
        tfInputOre.setPos(inputX, refY + 32);
        tfInputOre.setSize(fieldW, 14);
        tfInputOre.setTextColor(0xFFFFFF);
        tfInputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfInputOre.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfInputOre);
        attachDragChoiceSelector(scrollable, tfInputOre, inputX, refY + 32, fieldW);

        TextWidget labelNC = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.nc_item"));
        labelNC.setPos(6, refY + 50 + 3);
        scrollable.widget(labelNC);

        FilterTextFieldWidget tfNCItem = new FilterTextFieldWidget();
        tfNCItem.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_NC_ITEM));
        tfNCItem.setPos(inputX, refY + 50);
        tfNCItem.setSize(fieldW, 14);
        tfNCItem.setTextColor(0xFFFFFF);
        tfNCItem.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfNCItem.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfNCItem);
        attachDragChoiceSelector(scrollable, tfNCItem, inputX, refY + 50, fieldW);

        TextWidget labelTier = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.tier"));
        labelTier.setPos(6, refY + 68 + 3);
        scrollable.widget(labelTier);

        final List<String> tiers = Arrays.asList(
            I18nUtil.tr("ae2patterngen.gui.common.any"),
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
        int savedTier = ItemPatternGenerator.getSavedInt(held, ItemPatternGenerator.NBT_TARGET_TIER, -1);
        final int[] currentTierIndex = new int[] { Math.max(0, Math.min(tiers.size() - 1, savedTier + 1)) };

        ButtonWidget btnTier = new ButtonWidget();
        btnTier.setSynced(false, false);
        btnTier.setPos(inputX, refY + 68);
        btnTier.setSize(fieldW, 14);
        btnTier.setBackground(new Rectangle().setColor(0xFF1E1E30));

        TextWidget btnTierText = new TextWidget("");
        btnTierText.setStringSupplier(() -> EnumChatFormatting.WHITE + tiers.get(currentTierIndex[0]));
        btnTierText.setPos(inputX + 4, refY + 68 + 3);

        btnTier.setOnClick((clickData, widget) -> {
            if (clickData.mouseButton == 0) {
                currentTierIndex[0] = (currentTierIndex[0] + 1) % tiers.size();
            } else if (clickData.mouseButton == 1) {
                currentTierIndex[0] = (currentTierIndex[0] - 1 + tiers.size()) % tiers.size();
            }
        });
        scrollable.widget(btnTier);
        scrollable.widget(btnTierText);

        refY += 84;

        TextWidget labelBL = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.section.blacklist"));
        labelBL.setPos(6, refY + 3);
        scrollable.widget(labelBL);

        TextWidget labelBLIn = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.blacklist_input"));
        labelBLIn.setPos(6, refY + 14 + 3);
        scrollable.widget(labelBLIn);

        FilterTextFieldWidget tfBlacklistIn = new FilterTextFieldWidget();
        tfBlacklistIn.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_INPUT));
        tfBlacklistIn.setPos(inputX, refY + 14);
        tfBlacklistIn.setSize(fieldW, 14);
        tfBlacklistIn.setTextColor(0xFFFFFF);
        tfBlacklistIn.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistIn.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfBlacklistIn);
        attachDragChoiceSelector(scrollable, tfBlacklistIn, inputX, refY + 14, fieldW);

        TextWidget labelBLOut = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.blacklist_output"));
        labelBLOut.setPos(6, refY + 32 + 3);
        scrollable.widget(labelBLOut);

        FilterTextFieldWidget tfBlacklistOut = new FilterTextFieldWidget();
        tfBlacklistOut.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_OUTPUT));
        tfBlacklistOut.setPos(inputX, refY + 32);
        tfBlacklistOut.setSize(fieldW, 14);
        tfBlacklistOut.setTextColor(0xFFFFFF);
        tfBlacklistOut.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistOut.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfBlacklistOut);
        attachDragChoiceSelector(scrollable, tfBlacklistOut, inputX, refY + 32, fieldW);

        TextWidget regexHint = new TextWidget(
            EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.pattern_gen.hint.regex"));
        regexHint.setPos(6, refY + 50 + 3);
        scrollable.widget(regexHint);

        TextWidget blacklistHint = new TextWidget(
            EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.pattern_gen.hint.blacklist"));
        blacklistHint.setPos(6, refY + 60 + 3);
        scrollable.widget(blacklistHint);

        TextWidget blacklistExamples = new TextWidget(
            EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.pattern_gen.hint.blacklist_examples"));
        blacklistExamples.setPos(6, refY + 70 + 3);
        scrollable.widget(blacklistExamples);

        refY += 82;

        int loadedRuleCount = com.github.ae2patterngen.config.ReplacementConfig.load();

        TextWidget labelRep = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.section.replacements"));
        labelRep.setPos(6, refY + 3);
        scrollable.widget(labelRep);

        TextWidget labelRepCount = new TextWidget(
            I18nUtil.tr("ae2patterngen.gui.pattern_gen.replacements.count", loadedRuleCount));
        labelRepCount.setPos(6, refY + 20);
        scrollable.widget(labelRepCount);

        int btnCfgX = guiWidth - 16 - 6 - 80;
        int btnCfgY = refY + 14;
        ButtonWidget btnConfig = new ButtonWidget();
        btnConfig.setSynced(false, false);
        btnConfig.setPos(btnCfgX, btnCfgY);
        btnConfig.setSize(80, 20);
        btnConfig.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnConfigText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.open_config"));
        btnConfigText.setPos(btnCfgX + 16, btnCfgY + 6);

        btnConfig.setOnClick((cd, w) -> {
            try {
                java.awt.Desktop.getDesktop()
                    .open(com.github.ae2patterngen.config.ReplacementConfig.getConfigFile());
            } catch (Exception e) {}
        });
        scrollable.widget(btnConfig);
        scrollable.widget(btnConfigText);

        builder.widget(scrollable);

        TextWidget statusWidget = new TextWidget("");
        GuiPatternGenStatusBridge.setStatus(I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.ready"));
        statusWidget.setStringSupplier(GuiPatternGenStatusBridge::getStatus);
        statusWidget.setPos(8, guiHeight - 12);
        builder.widget(statusWidget);

        int btnW = 57;
        int btnH = 20;
        int btnGap = 3;
        int btnStartX = (guiWidth - (btnW * 3 + btnGap * 2)) / 2;
        int btnY = guiHeight - 32;

        Runnable saveFunction = () -> {
            NetworkHandler.INSTANCE.sendToServer(
                new PacketSaveFields(
                    tfRecipeMap.getText(),
                    tfOutputOre.getText(),
                    tfInputOre.getText(),
                    tfNCItem.getText(),
                    tfBlacklistIn.getText(),
                    tfBlacklistOut.getText(),
                    "",
                    currentTierIndex[0] - 1));
        };

        ButtonWidget btnCache = new ButtonWidget();
        btnCache.setSynced(false, false);
        btnCache.setPos(btnStartX, btnY);
        btnCache.setSize(btnW, btnH);
        btnCache.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnCacheText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.build_cache"));
        btnCacheText.setPos(btnStartX + 10, btnY + 6);

        btnCache.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketCreateCache());
            GuiPatternGenStatusBridge.setStatus(I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.cache_requested"));
        });
        builder.widget(btnCache);
        builder.widget(btnCacheText);

        int btnPBX = btnStartX + btnW + btnGap;
        ButtonWidget btnPreview = new ButtonWidget();
        btnPreview.setSynced(false, false);
        btnPreview.setPos(btnPBX, btnY);
        btnPreview.setSize(btnW, btnH);
        btnPreview.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnPreviewText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.preview_count"));
        btnPreviewText.setPos(btnPBX + 14, btnY + 6);

        btnPreview.setOnClick((cd, w) -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge
                    .setStatus(EnumChatFormatting.RED + I18nUtil.tr("ae2patterngen.gui.pattern_gen.error.empty_map"));
                return;
            }
            saveFunction.run();
            NetworkHandler.INSTANCE.sendToServer(
                new PacketPreviewRecipeCount(
                    tfRecipeMap.getText(),
                    tfOutputOre.getText(),
                    tfInputOre.getText(),
                    tfNCItem.getText(),
                    tfBlacklistIn.getText(),
                    tfBlacklistOut.getText(),
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus(I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.preview_requested"));
        });
        builder.widget(btnPreview);
        builder.widget(btnPreviewText);

        int btnGBX = btnStartX + (btnW + btnGap) * 2;
        ButtonWidget btnGenerate = new ButtonWidget();
        btnGenerate.setSynced(false, false);
        btnGenerate.setPos(btnGBX, btnY);
        btnGenerate.setSize(btnW, btnH);
        btnGenerate.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnGenerateText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.generate"));
        btnGenerateText.setPos(btnGBX + 10, btnY + 6);

        btnGenerate.setOnClick((cd, w) -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                GuiPatternGenStatusBridge
                    .setStatus(EnumChatFormatting.RED + I18nUtil.tr("ae2patterngen.gui.pattern_gen.error.empty_map"));
                return;
            }
            saveFunction.run();
            NetworkHandler.INSTANCE.sendToServer(
                new PacketGeneratePatterns(
                    tfRecipeMap.getText(),
                    tfOutputOre.getText(),
                    tfInputOre.getText(),
                    tfNCItem.getText(),
                    tfBlacklistIn.getText(),
                    tfBlacklistOut.getText(),
                    "",
                    currentTierIndex[0] - 1));
            GuiPatternGenStatusBridge.setStatus(I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.generate_requested"));
        });
        builder.widget(btnGenerate);
        builder.widget(btnGenerateText);

        buildContext.addCloseListener(saveFunction);

        return builder.build();
    }

    private static void attachDragChoiceSelector(Scrollable scrollable, FilterTextFieldWidget field, int fieldX,
        int fieldY, int fieldWidth) {
        final ExplicitFilterDropFormatter.DropChoices[] currentChoices = new ExplicitFilterDropFormatter.DropChoices[] {
            ExplicitFilterDropFormatter.DropChoices.empty() };
        final int[] currentIndex = new int[] { -1 };

        FilterDragChoiceButtonWidget selector = new FilterDragChoiceButtonWidget(field);
        selector.setSynced(false, false);
        selector.setPos(fieldX + fieldWidth - DRAG_SELECTOR_W, fieldY);
        selector.setSize(DRAG_SELECTOR_W, 14);
        selector.setEnabled(widget -> hasAlternativeChoices(currentChoices[0]));
        selector.setBackground(() -> buildSelectorBackground(currentChoices[0], currentIndex[0]));
        selector.addTooltip(I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.tooltip.line1"));
        selector.addTooltip(I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.tooltip.line2"));
        selector.setOnClick((clickData, widget) -> {
            if (!hasAlternativeChoices(currentChoices[0])) {
                return;
            }

            int direction = clickData.mouseButton == 1 ? -1 : 1;
            currentIndex[0] = cycleIndex(currentIndex[0], currentChoices[0].size(), direction);
            field.applyDropChoice(
                currentChoices[0].getOptions()
                    .get(currentIndex[0]));
        });
        scrollable.widget(selector);

        field.setDropChoicesListener(choices -> {
            currentChoices[0] = choices != null ? choices : ExplicitFilterDropFormatter.DropChoices.empty();
            currentIndex[0] = currentChoices[0].getDefaultIndex();
        });
    }

    private static boolean hasAlternativeChoices(ExplicitFilterDropFormatter.DropChoices choices) {
        return choices != null && choices.size() > 1;
    }

    private static int cycleIndex(int currentIndex, int size, int direction) {
        if (size <= 0) {
            return -1;
        }

        int safeCurrent = currentIndex >= 0 ? currentIndex : 0;
        int next = (safeCurrent + direction) % size;
        return next < 0 ? next + size : next;
    }

    private static IDrawable[] buildSelectorBackground(ExplicitFilterDropFormatter.DropChoices choices,
        int currentIndex) {
        return new IDrawable[] { new Rectangle().setColor(DRAG_SELECTOR_BG),
            new Text(resolveChoiceLabel(choices, currentIndex)).color(0xFFFFFF)
                .alignment(com.gtnewhorizons.modularui.api.math.Alignment.Center) };
    }

    private static String resolveChoiceLabel(ExplicitFilterDropFormatter.DropChoices choices, int currentIndex) {
        if (choices == null || choices.isEmpty()) {
            return "";
        }

        int safeIndex = currentIndex;
        if (safeIndex < 0 || safeIndex >= choices.size()) {
            safeIndex = choices.getDefaultIndex();
        }
        if (safeIndex < 0 || safeIndex >= choices.size()) {
            return "";
        }

        ExplicitFilterDropFormatter.DropChoice choice = choices.getOptions()
            .get(safeIndex);
        switch (choice.getSource()) {
            case ITEM_ID:
                return I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.id");
            case ORE_DICT:
                return I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.ore");
            case DISPLAY_NAME:
                return I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.name");
            case CUSTOM:
            default:
                return I18nUtil.tr("ae2patterngen.gui.pattern_gen.drag_choice.custom");
        }
    }
}
