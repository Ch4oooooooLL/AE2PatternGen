package com.github.ae2patterngen.gui;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.item.ItemPatternGenerator;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketGeneratePatterns;
import com.github.ae2patterngen.network.PacketSaveFields;
import com.github.ae2patterngen.recipe.GTRecipeSource;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

public class GuiPatternGen {

    private static final int GUI_W = 260;
    private static final int GUI_H = 315;

    public static ModularWindow createWindow(UIBuildContext buildContext, ItemStack held) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);

        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.title"));
        titleText.setScale(1.2f);
        titleText.setSize(GUI_W - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 24);
        scrollable.setSize(GUI_W - 16, GUI_H - 24 - 40);

        int refY = 0;
        int fieldW = GUI_W - 16 - 80 - 12;
        int fullFieldW = GUI_W - 16 - 12;
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

        TextFieldWidget tfOutputOre = new TextFieldWidget();
        tfOutputOre.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_OUTPUT_ORE));
        tfOutputOre.setPos(inputX, refY + 14);
        tfOutputOre.setSize(fieldW, 14);
        tfOutputOre.setTextColor(0xFFFFFF);
        tfOutputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfOutputOre.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfOutputOre);

        TextWidget labelInOre = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.input_ore"));
        labelInOre.setPos(6, refY + 32 + 3);
        scrollable.widget(labelInOre);

        TextFieldWidget tfInputOre = new TextFieldWidget();
        tfInputOre.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_INPUT_ORE));
        tfInputOre.setPos(inputX, refY + 32);
        tfInputOre.setSize(fieldW, 14);
        tfInputOre.setTextColor(0xFFFFFF);
        tfInputOre.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfInputOre.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfInputOre);

        TextWidget labelNC = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.nc_item"));
        labelNC.setPos(6, refY + 50 + 3);
        scrollable.widget(labelNC);

        TextFieldWidget tfNCItem = new TextFieldWidget();
        tfNCItem.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_NC_ITEM));
        tfNCItem.setPos(inputX, refY + 50);
        tfNCItem.setSize(fieldW, 14);
        tfNCItem.setTextColor(0xFFFFFF);
        tfNCItem.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfNCItem.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfNCItem);

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

        TextFieldWidget tfBlacklistIn = new TextFieldWidget();
        tfBlacklistIn.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_INPUT));
        tfBlacklistIn.setPos(inputX, refY + 14);
        tfBlacklistIn.setSize(fieldW, 14);
        tfBlacklistIn.setTextColor(0xFFFFFF);
        tfBlacklistIn.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistIn.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfBlacklistIn);

        TextWidget labelBLOut = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.label.blacklist_output"));
        labelBLOut.setPos(6, refY + 32 + 3);
        scrollable.widget(labelBLOut);

        TextFieldWidget tfBlacklistOut = new TextFieldWidget();
        tfBlacklistOut.setText(ItemPatternGenerator.getSavedField(held, ItemPatternGenerator.NBT_BLACKLIST_OUTPUT));
        tfBlacklistOut.setPos(inputX, refY + 32);
        tfBlacklistOut.setSize(fieldW, 14);
        tfBlacklistOut.setTextColor(0xFFFFFF);
        tfBlacklistOut.setBackground(new Rectangle().setColor(0xFF1E1E30));
        tfBlacklistOut.setTextAlignment(com.gtnewhorizons.modularui.api.math.Alignment.CenterLeft);
        scrollable.widget(tfBlacklistOut);

        TextWidget regexHint = new TextWidget(
            EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.pattern_gen.hint.regex"));
        regexHint.setPos(6, refY + 50 + 3);
        scrollable.widget(regexHint);

        refY += 62;

        int loadedRuleCount = com.github.ae2patterngen.config.ReplacementConfig.load();

        TextWidget labelRep = new TextWidget(
            EnumChatFormatting.BOLD + I18nUtil.tr("ae2patterngen.gui.pattern_gen.section.replacements"));
        labelRep.setPos(6, refY + 3);
        scrollable.widget(labelRep);

        TextWidget labelRepCount = new TextWidget(
            I18nUtil.tr("ae2patterngen.gui.pattern_gen.replacements.count", loadedRuleCount));
        labelRepCount.setPos(6, refY + 20);
        scrollable.widget(labelRepCount);

        int btnCfgX = GUI_W - 16 - 6 - 80;
        int btnCfgY = refY + 14;
        ButtonWidget btnConfig = new ButtonWidget();
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

        String[] statusMsg = new String[] { I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.ready") };
        TextWidget statusWidget = new TextWidget("");
        statusWidget.setStringSupplier(() -> statusMsg[0]);
        statusWidget.setPos(8, GUI_H - 12);
        builder.widget(statusWidget);

        int btnW = 76;
        int btnH = 20;
        int btnStartX = (GUI_W - (btnW * 3 + 6)) / 2;
        int btnY = GUI_H - 32;

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

        ButtonWidget btnList = new ButtonWidget();
        btnList.setPos(btnStartX, btnY);
        btnList.setSize(btnW, btnH);
        btnList.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnListText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.list_maps"));
        btnListText.setPos(btnStartX + 14, btnY + 6);

        btnList.setOnClick((cd, w) -> {
            saveFunction.run();
            List<String> matched = GTRecipeSource.findMatchingRecipeMaps(tfRecipeMap.getText());
            statusMsg[0] = I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.map_matches", matched.size());
        });
        builder.widget(btnList);
        builder.widget(btnListText);

        int btnPBX = btnStartX + btnW + 3;
        ButtonWidget btnPreview = new ButtonWidget();
        btnPreview.setPos(btnPBX, btnY);
        btnPreview.setSize(btnW, btnH);
        btnPreview.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnPreviewText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.preview_count"));
        btnPreviewText.setPos(btnPBX + 14, btnY + 6);

        btnPreview.setOnClick((cd, w) -> {
            saveFunction.run();
            List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(tfRecipeMap.getText());
            statusMsg[0] = I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.recipes_before_filter", recipes.size());
        });
        builder.widget(btnPreview);
        builder.widget(btnPreviewText);

        int btnGBX = btnStartX + (btnW + 3) * 2;
        ButtonWidget btnGenerate = new ButtonWidget();
        btnGenerate.setPos(btnGBX, btnY);
        btnGenerate.setSize(btnW, btnH);
        btnGenerate.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);

        TextWidget btnGenerateText = new TextWidget(I18nUtil.tr("ae2patterngen.gui.pattern_gen.button.generate"));
        btnGenerateText.setPos(btnGBX + 10, btnY + 6);

        btnGenerate.setOnClick((cd, w) -> {
            if (tfRecipeMap.getText()
                .isEmpty()) {
                statusMsg[0] = EnumChatFormatting.RED + I18nUtil.tr("ae2patterngen.gui.pattern_gen.error.empty_map");
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
            statusMsg[0] = I18nUtil.tr("ae2patterngen.gui.pattern_gen.status.generate_requested");
        });
        builder.widget(btnGenerate);
        builder.widget(btnGenerateText);

        buildContext.addCloseListener(saveFunction);

        return builder.build();
    }
}
