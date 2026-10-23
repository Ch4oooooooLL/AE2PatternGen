package com.github.ae2patterngen.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.config.ForgeConfig;
import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketBrowseRequest;
import com.github.ae2patterngen.network.PacketBrowseResponse;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.github.ae2patterngen.util.I18nUtil;
import com.gtnewhorizons.modularui.api.ModularUITextures;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.ItemDrawable;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

/**
 * 配方浏览器界面：配方池 -> 规则组 -> 配方明细 / 预览过滤，四层视图共用一扇窗口。
 * <p>
 * 全部数据来自服务端（配方缓存派生），客户端只负责展示与发请求。
 */
public final class GuiRecipeBrowser {

    private static final int ROW_HEIGHT = 17;
    private static final int HEADER_HEIGHT = 15;
    private static final int ICON_SIZE = 16;
    private static final int CONTENT_TOP = 30;
    private static final int FOOTER_HEIGHT = 42;
    private static final int BUTTON_HEIGHT = 16;
    private static final int ROW_BG = 0xFF1E1E32;
    private static final int ROW_BG_HOVER = 0xFF33335C;
    private static final int HEADER_BG = 0xFF121223;
    private static final int GENERATE_BUTTON_WIDTH = 74;

    private static final String[] TIERS = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV", "UXV", "MAX" };

    private static RecipeBrowserState state;
    private static int windowToken = 0;
    private static boolean ignoreLateResponses = false;

    private GuiRecipeBrowser() {}

    /** 物品右键入口：清掉"用户已关闭界面"标记，并请求池列表。 */
    public static void requestOpen() {
        ignoreLateResponses = false;
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.open());
    }

    public static void open(PacketBrowseResponse response) {
        if (response == null) {
            return;
        }
        if (response.type == PacketBrowseResponse.TYPE_POOLS || response.type == PacketBrowseResponse.TYPE_STORAGE) {
            // 明确的打开请求（右键 / 蹲下右键），无论之前是否刚关闭界面都要开窗。
            ignoreLateResponses = false;
        }
        if (state == null) {
            if (ignoreLateResponses) {
                return;
            }
            state = new RecipeBrowserState();
        }
        state.applyResponse(response);
        showWindow();
    }

    private static void showWindow() {
        if (state == null) {
            return;
        }
        final int token = ++windowToken;

        UIBuildContext buildContext = new UIBuildContext(Minecraft.getMinecraft().thePlayer);
        buildContext.addCloseListener(() -> {
            if (token == windowToken) {
                state = null;
                ignoreLateResponses = true;
            }
        });

        com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
            buildContext,
            () -> {});
        ModularWindow window = createWindow(buildContext);
        Minecraft.getMinecraft()
            .displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, window)));
    }

    private static ModularWindow createWindow(UIBuildContext buildContext) {
        net.minecraft.client.gui.ScaledResolution resolution = new net.minecraft.client.gui.ScaledResolution(
            Minecraft.getMinecraft(),
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight);
        int maxHeight = Minecraft.getMinecraft().displayHeight / resolution.getScaleFactor() - 20;
        int guiWidth = ForgeConfig.getRecipePickerGuiWidth();
        int guiHeight = Math
            .min(maxHeight, Math.max(ForgeConfig.getRecipePickerMinHeight(), ForgeConfig.getRecipePickerIdealHeight()));

        ModularWindow.Builder builder = ModularWindow.builder(guiWidth, guiHeight);
        builder.setBackground(ModularUITextures.VANILLA_BACKGROUND);

        int contentWidth = guiWidth - 16;
        int rowCapacity = Math.max(1, (guiHeight - CONTENT_TOP - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);

        TextWidget title = new TextWidget(EnumChatFormatting.BOLD + titleText(guiWidth - 16));
        title.setScale(1.2f);
        title.setSize(guiWidth - 16, 12);
        title.setPos(8, 6);
        builder.widget(title);

        boolean hasStatus = state.status != null && !state.status.isEmpty();
        TextWidget info = new TextWidget(
            (hasStatus ? EnumChatFormatting.RED : EnumChatFormatting.GRAY)
                + trim(hasStatus ? state.status : infoText(), guiWidth - 20));
        info.setPos(8, 19);
        info.setSize(guiWidth - 16, 10);
        builder.widget(info);

        switch (state.view) {
            case RecipeBrowserState.VIEW_RULES:
                buildRules(builder, contentWidth, rowCapacity);
                break;
            case RecipeBrowserState.VIEW_RECIPES:
                buildRecipes(builder, contentWidth, rowCapacity);
                break;
            case RecipeBrowserState.VIEW_PREVIEW:
                buildPreview(builder, guiWidth, guiHeight, contentWidth);
                break;
            case RecipeBrowserState.VIEW_STORAGE:
                buildStorage(builder, contentWidth, rowCapacity);
                break;
            case RecipeBrowserState.VIEW_POOLS:
            default:
                buildPools(builder, contentWidth, rowCapacity);
                break;
        }

        buildFooter(builder, guiWidth, guiHeight, rowCapacity);
        return builder.build();
    }

    // ---------------------------------------------------------------- 文本

    private static String titleText(int maxWidth) {
        String suffix;
        switch (state.view) {
            case RecipeBrowserState.VIEW_RULES:
                suffix = I18nUtil.tr(
                    "ae2patterngen.gui.browser.breadcrumb.rules",
                    RecipeBrowserState.displayNameOfPool(state.mapId));
                break;
            case RecipeBrowserState.VIEW_RECIPES:
                suffix = I18nUtil.tr(
                    "ae2patterngen.gui.browser.breadcrumb.recipes",
                    RecipeBrowserState.displayNameOfPool(state.mapId));
                break;
            case RecipeBrowserState.VIEW_PREVIEW:
                suffix = I18nUtil.tr(
                    "ae2patterngen.gui.browser.breadcrumb.preview",
                    RecipeBrowserState.displayNameOfPool(state.mapId));
                break;
            case RecipeBrowserState.VIEW_STORAGE:
                suffix = I18nUtil.tr("ae2patterngen.gui.browser.breadcrumb.storage");
                break;
            case RecipeBrowserState.VIEW_POOLS:
            default:
                suffix = I18nUtil.tr("ae2patterngen.gui.browser.breadcrumb.pools");
                break;
        }
        return trim(I18nUtil.tr("ae2patterngen.gui.browser.title") + " · " + suffix, maxWidth);
    }

    private static String infoText() {
        switch (state.view) {
            case RecipeBrowserState.VIEW_RULES:
                return I18nUtil.tr(
                    "ae2patterngen.gui.browser.info.rules",
                    state.totalRecipeCount,
                    state.rules.size(),
                    state.hiddenSingletonCount,
                    state.hiddenUnruledRecipeCount);
            case RecipeBrowserState.VIEW_RECIPES:
                return I18nUtil
                    .tr("ae2patterngen.gui.browser.info.recipes", state.recipes.size(), state.recipeTotalCount);
            case RecipeBrowserState.VIEW_PREVIEW:
                return I18nUtil.tr(
                    "ae2patterngen.gui.browser.info.preview",
                    ruleLabel(state.groupRule),
                    state.groupTotalCount,
                    state.matchedCount);
            case RecipeBrowserState.VIEW_STORAGE:
                return I18nUtil.tr("ae2patterngen.gui.browser.info.storage", state.storageCount, state.storageSource);
            case RecipeBrowserState.VIEW_POOLS:
            default:
                return I18nUtil.tr(
                    "ae2patterngen.gui.browser.info.pools",
                    state.filteredPoolIndexes()
                        .size(),
                    state.poolMapIds.size());
        }
    }

    // ---------------------------------------------------------------- 一级：配方池

    private static void buildPools(ModularWindow.Builder builder, int contentWidth, int rowCapacity) {
        addListHeader(
            builder,
            contentWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.column.pool"),
            I18nUtil.tr("ae2patterngen.gui.browser.column.recipe_count"),
            84);

        List<Integer> indexes = state.filteredPoolIndexes();
        int pageCount = pageCount(indexes.size(), rowCapacity);
        clampPage(pageCount);
        List<Integer> rows = slice(indexes, rowCapacity);
        int top = CONTENT_TOP + HEADER_HEIGHT;

        if (rows.isEmpty()) {
            addEmptyHint(builder, contentWidth, I18nUtil.tr("ae2patterngen.gui.browser.empty.pools"));
            return;
        }

        for (int row = 0; row < rows.size(); row++) {
            int y = top + row * ROW_HEIGHT;
            int poolIndex = rows.get(row);
            String mapId = state.poolMapIds.get(poolIndex);
            int recipeCount = poolIndex < state.poolRecipeCounts.size() ? state.poolRecipeCounts.get(poolIndex) : 0;
            String modId = poolIndex < state.poolModIds.size() ? state.poolModIds.get(poolIndex) : "";

            addRowButton(builder, 8, y, contentWidth, ROW_HEIGHT - 2, () -> requestGroups(mapId), () -> {
                java.util.List<String> tip = new java.util.ArrayList<String>();
                tip.add(EnumChatFormatting.WHITE + I18nUtil.tr("ae2patterngen.gui.browser.tooltip.pool", mapId, modId));
                tip.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.gui.browser.count.recipes", recipeCount));
                tip.add(EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.browser.hint.open_rules"));
                return tip;
            });

            TextWidget label = new TextWidget(
                EnumChatFormatting.WHITE + trim(RecipeBrowserState.displayNameOfPool(mapId), contentWidth - 100));
            label.setPos(12, y + 4);
            label.setSize(contentWidth - 100, 10);
            builder.widget(label);

            if (modId != null && !modId.isEmpty()) {
                TextWidget mod = new TextWidget(EnumChatFormatting.DARK_GRAY + trim(modId, 108));
                mod.setPos(8 + contentWidth - 96, y + 4);
                mod.setSize(56, 10);
                mod.setTextAlignment(Alignment.CenterRight);
                builder.widget(mod);
            }

            TextWidget count = new TextWidget(
                EnumChatFormatting.YELLOW + I18nUtil.tr("ae2patterngen.gui.browser.count.recipes", recipeCount));
            count.setPos(8 + contentWidth - 40, y + 4);
            count.setSize(36, 10);
            count.setTextAlignment(Alignment.CenterRight);
            builder.widget(count);
        }
    }

    // ---------------------------------------------------------------- 二级：规则组

    private static void buildRules(ModularWindow.Builder builder, int contentWidth, int rowCapacity) {
        addListHeader(
            builder,
            contentWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.column.rule"),
            I18nUtil.tr("ae2patterngen.gui.browser.column.recipe_count"),
            84);

        int pageCount = pageCount(state.rules.size(), rowCapacity);
        clampPage(pageCount);
        List<PacketBrowseResponse.GroupSummary> rows = slice(state.rules, rowCapacity);
        int top = CONTENT_TOP + HEADER_HEIGHT;

        if (rows.isEmpty()) {
            addEmptyHint(builder, contentWidth, I18nUtil.tr("ae2patterngen.gui.browser.empty.rules"));
            return;
        }

        int iconsBase = 8 + Math.max(150, contentWidth - GENERATE_BUTTON_WIDTH - 64 - 118);
        for (int row = 0; row < rows.size(); row++) {
            int y = top + row * ROW_HEIGHT;
            final PacketBrowseResponse.GroupSummary summary = rows.get(row);

            addRowButton(
                builder,
                8,
                y,
                contentWidth - GENERATE_BUTTON_WIDTH - 4,
                ROW_HEIGHT - 2,
                () -> openRecipes(summary),
                () -> ruleTooltipLines(summary));

            int textX = 12;
            if (summary.circuitDisplay != null) {
                addItemIcon(builder, textX, y + 1, summary.circuitDisplay, circuitTooltip(summary));
                textX += ICON_SIZE + 3;
            }

            TextWidget label = new TextWidget(EnumChatFormatting.WHITE + ruleRuleText(summary));
            label.setPos(textX, y + 4);
            label.setSize(Math.max(40, iconsBase - textX - 4), 10);
            builder.widget(label);

            int ncEnd = addIcons(builder, iconsBase, y + 1, summary.ncItems, 3);
            addIcons(builder, summary.ncItems.length > 0 ? ncEnd + 2 : iconsBase + 4, y + 1, summary.outputSamples, 3);

            TextWidget count = new TextWidget(
                EnumChatFormatting.YELLOW
                    + I18nUtil.tr("ae2patterngen.gui.browser.count.recipes", summary.recipeCount));
            count.setPos(8 + contentWidth - GENERATE_BUTTON_WIDTH - 40, y + 4);
            count.setSize(36, 10);
            count.setTextAlignment(Alignment.CenterRight);
            builder.widget(count);

            addButton(
                builder,
                8 + contentWidth - GENERATE_BUTTON_WIDTH,
                y,
                GENERATE_BUTTON_WIDTH,
                ROW_HEIGHT - 2,
                I18nUtil.tr("ae2patterngen.gui.browser.button.generate_group"),
                () -> openPreview(summary));
        }
    }

    private static String ruleRuleText(PacketBrowseResponse.GroupSummary summary) {
        if (summary == null) {
            return I18nUtil.tr("ae2patterngen.gui.browser.rule.unruled");
        }
        return ruleLabel(summary);
    }

    // ---------------------------------------------------------------- 三级：配方明细

    private static void buildRecipes(ModularWindow.Builder builder, int contentWidth, int rowCapacity) {
        addListHeader(
            builder,
            contentWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.column.recipe"),
            I18nUtil.tr("ae2patterngen.gui.browser.column.action"),
            84);

        int pageCount = pageCount(state.recipes.size(), rowCapacity);
        clampPage(pageCount);
        List<RecipeEntry> rows = slice(state.recipes, rowCapacity);
        int top = CONTENT_TOP + HEADER_HEIGHT;

        if (rows.isEmpty()) {
            addEmptyHint(builder, contentWidth, I18nUtil.tr("ae2patterngen.gui.browser.empty.recipes"));
            return;
        }

        for (int row = 0; row < rows.size(); row++) {
            int y = top + row * ROW_HEIGHT;
            int globalIndex = state.page * rowCapacity + row;
            RecipeEntry recipe = rows.get(row);

            addRowButton(builder, 8, y, contentWidth - GENERATE_BUTTON_WIDTH - 4, ROW_HEIGHT - 2, null, () -> {
                java.util.List<String> tip = new java.util.ArrayList<String>();
                tip.add(EnumChatFormatting.WHITE + inputPreview(recipe));
                if (recipe.duration > 0) {
                    tip.add(
                        EnumChatFormatting.GRAY + I18nUtil
                            .tr("ae2patterngen.gui.browser.tooltip.duration", recipe.duration, recipe.euPerTick));
                }
                return tip;
            });

            ItemStack product = firstOutput(recipe);
            if (product != null) {
                addItemIcon(
                    builder,
                    12,
                    y + 1,
                    product,
                    RecipeBrowserState.displayNameOf(product) + " x" + Math.max(1, product.stackSize));
            }

            TextWidget label = new TextWidget(EnumChatFormatting.WHITE + recipeLabel(recipe, contentWidth));
            label.setPos(12 + ICON_SIZE + 3, y + 4);
            label.setSize(Math.max(40, contentWidth - GENERATE_BUTTON_WIDTH - ICON_SIZE - 30), 10);
            builder.widget(label);

            addButton(
                builder,
                8 + contentWidth - GENERATE_BUTTON_WIDTH,
                y,
                GENERATE_BUTTON_WIDTH,
                ROW_HEIGHT - 2,
                I18nUtil.tr("ae2patterngen.gui.browser.button.generate_one"),
                () -> requestGenerate(state.groupId, globalIndex));
        }
    }

    // ---------------------------------------------------------------- 预览与过滤

    private static void buildPreview(ModularWindow.Builder builder, int guiWidth, int guiHeight, int contentWidth) {
        int labelX = 8;
        int fieldX = 74;
        int fieldWidth = contentWidth - 66;
        int y = CONTENT_TOP;

        addTextField(
            builder,
            labelX,
            y,
            fieldX,
            fieldWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.filter.output"),
            state.filterOutput,
            text -> state.filterOutput = text);
        y += 18;
        addTextField(
            builder,
            labelX,
            y,
            fieldX,
            fieldWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.filter.input"),
            state.filterInput,
            text -> state.filterInput = text);
        y += 18;
        addTextField(
            builder,
            labelX,
            y,
            fieldX,
            fieldWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.filter.nc"),
            state.filterNc,
            text -> state.filterNc = text);
        y += 18;
        addTextField(
            builder,
            labelX,
            y,
            fieldX,
            fieldWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.filter.exclude_in"),
            state.excludeInput,
            text -> state.excludeInput = text);
        y += 18;
        addTextField(
            builder,
            labelX,
            y,
            fieldX,
            fieldWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.filter.exclude_out"),
            state.excludeOutput,
            text -> state.excludeOutput = text);
        y += 18;

        // 电压 + 语法提示
        TextWidget tierLabel = new TextWidget(I18nUtil.tr("ae2patterngen.gui.browser.filter.tier"));
        tierLabel.setPos(labelX, y + 4);
        tierLabel.setSize(64, 10);
        builder.widget(tierLabel);

        addButton(builder, fieldX, y, 96, BUTTON_HEIGHT, EnumChatFormatting.WHITE + tierText(), () -> {
            state.tierIndex = (state.tierIndex + 1) % (TIERS.length + 1);
            showWindow();
        });

        TextWidget hint = new TextWidget(
            EnumChatFormatting.DARK_GRAY + trim(I18nUtil.tr("ae2patterngen.gui.browser.filter.syntax"), 200));
        hint.setPos(fieldX + 102, y + 4);
        hint.setSize(Math.max(40, contentWidth - 102 - 66), 10);
        builder.widget(hint);
        y += 20;

        int footerTop = guiHeight - FOOTER_HEIGHT;
        boolean listsFit = footerTop - y >= 62;
        if (!listsFit) {
            return;
        }

        y = addPreviewSection(
            builder,
            contentWidth,
            y,
            I18nUtil.tr("ae2patterngen.gui.browser.preview.inputs"),
            state.inputSamples,
            state.distinctInputCount);
        y = addPreviewSection(
            builder,
            contentWidth,
            y,
            I18nUtil.tr("ae2patterngen.gui.browser.preview.outputs"),
            state.outputSamples,
            state.distinctOutputCount);

        if (!state.previewLoaded) {
            TextWidget notLoaded = new TextWidget(
                EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.browser.preview.not_loaded"));
            notLoaded.setPos(12, Math.min(y + 2, footerTop - 12));
            notLoaded.setSize(contentWidth - 8, 10);
            builder.widget(notLoaded);
        }
    }

    private static int addPreviewSection(ModularWindow.Builder builder, int contentWidth, int y, String title,
        ItemStack[] samples, int distinctCount) {
        TextWidget label = new TextWidget(
            EnumChatFormatting.YELLOW + I18nUtil.tr("ae2patterngen.gui.browser.preview.section", title, distinctCount));
        label.setPos(12, y + 2);
        label.setSize(contentWidth - 8, 10);
        builder.widget(label);
        y += 11;

        int x = 12;
        int shown = 0;
        if (samples != null) {
            for (ItemStack stack : samples) {
                if (stack == null || stack.getItem() == null) {
                    continue;
                }
                if (x + ICON_SIZE > 8 + contentWidth) {
                    break;
                }
                addItemIcon(builder, x, y, stack, RecipeBrowserState.displayNameOf(stack));
                x += ICON_SIZE + 2;
                shown++;
            }
        }
        if (distinctCount > shown) {
            TextWidget more = new TextWidget(
                EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.gui.browser.preview.more", distinctCount - shown));
            more.setPos(Math.min(x + 2, 8 + contentWidth - 40), y + 4);
            more.setSize(40, 10);
            builder.widget(more);
        }
        return y + ICON_SIZE + 4;
    }

    private static String tierText() {
        if (state.tierIndex <= 0) {
            return I18nUtil.tr("ae2patterngen.gui.common.any");
        }
        return TIERS[Math.min(state.tierIndex, TIERS.length) - 1];
    }

    private static void addTextField(ModularWindow.Builder builder, int labelX, int y, int fieldX, int fieldWidth,
        String labelText, String value, java.util.function.Consumer<String> setter) {
        TextWidget label = new TextWidget(labelText);
        label.setPos(labelX, y + 4);
        label.setSize(64, 10);
        builder.widget(label);

        FilterTextFieldWidget field = new FilterTextFieldWidget();
        field.setSynced(false, false);
        field.setText(value != null ? value : "");
        field.setPos(fieldX, y);
        field.setSize(fieldWidth, 16);
        field.setTextColor(0xFFFFFF);
        field.setBackground(new Rectangle().setColor(0xFF1E1E30));
        field.setTextAlignment(Alignment.CenterLeft);
        field.setSetter(setter);
        builder.widget(field);
    }

    // ---------------------------------------------------------------- 浏览器仓储

    private static void buildStorage(ModularWindow.Builder builder, int contentWidth, int rowCapacity) {
        addListHeader(
            builder,
            contentWidth,
            I18nUtil.tr("ae2patterngen.gui.browser.column.storage_item"),
            I18nUtil.tr("ae2patterngen.gui.browser.column.action"),
            64);

        int capacity = Math.min(rowCapacity, Math.max(1, state.storagePageSize));
        int top = CONTENT_TOP + HEADER_HEIGHT;
        List<String> rows = state.storagePreviews;

        if (rows.isEmpty()) {
            addEmptyHint(builder, contentWidth, I18nUtil.tr("ae2patterngen.gui.browser.empty.storage"));
            addHintLine(
                builder,
                contentWidth,
                CONTENT_TOP + HEADER_HEIGHT + 16,
                I18nUtil.tr("ae2patterngen.gui.browser.storage.deliver_hint"));
            return;
        }

        for (int row = 0; row < rows.size() && row < capacity; row++) {
            int y = top + row * ROW_HEIGHT;
            int globalIndex = state.page * Math.max(1, state.storagePageSize) + row;
            final int rowIndex = globalIndex;

            addRowButton(builder, 8, y, contentWidth - 62, ROW_HEIGHT - 2, null, () -> {
                java.util.List<String> tip = new java.util.ArrayList<String>();
                tip.add(
                    EnumChatFormatting.WHITE + I18nUtil.tr("ae2patterngen.gui.browser.storage.index", rowIndex + 1));
                return tip;
            });

            TextWidget label = new TextWidget(
                EnumChatFormatting.WHITE + trim((rowIndex + 1) + ". " + rows.get(row), contentWidth - 78));
            label.setPos(12, y + 4);
            label.setSize(contentWidth - 78, 10);
            builder.widget(label);

            addButton(
                builder,
                8 + contentWidth - 56,
                y,
                56,
                ROW_HEIGHT - 2,
                I18nUtil.tr("ae2patterngen.gui.browser.button.delete"),
                () -> requestStorageDelete(rowIndex));
        }

        addHintLine(
            builder,
            contentWidth,
            top + capacity * ROW_HEIGHT + 2,
            I18nUtil.tr("ae2patterngen.gui.browser.storage.deliver_hint"));
    }

    private static void addHintLine(ModularWindow.Builder builder, int contentWidth, int y, String text) {
        TextWidget hint = new TextWidget(EnumChatFormatting.DARK_GRAY + trim(text, contentWidth - 8));
        hint.setPos(12, y);
        hint.setSize(contentWidth - 8, 10);
        builder.widget(hint);
    }

    // ---------------------------------------------------------------- 页脚

    private static void buildFooter(ModularWindow.Builder builder, int guiWidth, int guiHeight, int rowCapacity) {
        int left = 8;
        int right = guiWidth - 8;
        int row1Y = guiHeight - FOOTER_HEIGHT + 4;
        int row2Y = guiHeight - 22;

        if (state.view != RecipeBrowserState.VIEW_POOLS) {
            addButton(
                builder,
                left,
                row1Y,
                54,
                BUTTON_HEIGHT,
                I18nUtil.tr("ae2patterngen.gui.browser.button.back"),
                () -> {
                    state.view = state.view == RecipeBrowserState.VIEW_POOLS ? RecipeBrowserState.VIEW_POOLS
                        : (state.view == RecipeBrowserState.VIEW_PREVIEW ? RecipeBrowserState.VIEW_RULES
                            : (state.view == RecipeBrowserState.VIEW_RECIPES ? RecipeBrowserState.VIEW_RULES
                                : RecipeBrowserState.VIEW_POOLS));
                    state.page = 0;
                    state.status = "";
                    showWindow();
                });
        }

        int pageCount = currentPageCount(rowCapacity);
        if (pageCount > 1) {
            int nextX = right - 24;
            int pageX = nextX - 64;
            int prevX = pageX - 28;
            addButton(builder, prevX, row1Y, 24, BUTTON_HEIGHT, "<", () -> goToPage(state.page - 1));
            TextWidget pageText = new TextWidget(
                EnumChatFormatting.WHITE
                    + I18nUtil.tr("ae2patterngen.gui.browser.page", Math.min(state.page + 1, pageCount), pageCount));
            pageText.setPos(pageX, row1Y + 4);
            pageText.setSize(60, 10);
            pageText.setTextAlignment(Alignment.Center);
            builder.widget(pageText);
            addButton(builder, nextX, row1Y, 24, BUTTON_HEIGHT, ">", () -> goToPage(state.page + 1));
        }

        addButton(
            builder,
            right - 24 - 64 - 28 - 4 - 54,
            row1Y,
            54,
            BUTTON_HEIGHT,
            I18nUtil.tr("ae2patterngen.gui.browser.button.close"),
            () -> Minecraft.getMinecraft()
                .displayGuiScreen(null));

        switch (state.view) {
            case RecipeBrowserState.VIEW_RULES:
                addButton(
                    builder,
                    left,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.refresh"),
                    () -> requestGroups(state.mapId));
                addButton(builder, left + 58, row2Y, 96, BUTTON_HEIGHT, singletonLabel(), () -> {
                    state.showSingletonRules = !state.showSingletonRules;
                    requestGroups(state.mapId);
                });
                addButton(builder, left + 158, row2Y, 96, BUTTON_HEIGHT, unruledLabel(), () -> {
                    state.showUnruledGroup = !state.showUnruledGroup;
                    requestGroups(state.mapId);
                });
                break;
            case RecipeBrowserState.VIEW_RECIPES:
                addButton(
                    builder,
                    left,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.refresh"),
                    () -> requestRecipes(state.groupId));
                addButton(
                    builder,
                    left + 58,
                    row2Y,
                    96,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.filter_page"),
                    () -> openPreview(state.groupRule));
                break;
            case RecipeBrowserState.VIEW_PREVIEW:
                addButton(
                    builder,
                    left,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.reset"),
                    () -> {
                        state.clearFilters();
                        showWindow();
                        requestPreview();
                    });
                addButton(
                    builder,
                    left + 58,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.preview"),
                    GuiRecipeBrowser::requestPreview);
                addButton(
                    builder,
                    left + 116,
                    row2Y,
                    GENERATE_BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    EnumChatFormatting.WHITE
                        + I18nUtil.tr("ae2patterngen.gui.browser.button.generate_matched", state.matchedCount),
                    GuiRecipeBrowser::requestGenerateFiltered);
                break;
            case RecipeBrowserState.VIEW_STORAGE:
                addButton(
                    builder,
                    left,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.refresh"),
                    () -> requestStoragePage(state.page));
                addButton(
                    builder,
                    left + 58,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.clear"),
                    () -> NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.storageClear()));
                break;
            case RecipeBrowserState.VIEW_POOLS:
            default:
                TextFieldWidget search = new TextFieldWidget();
                search.setSynced(false, false);
                search.setMaxLength(48);
                search.setText(state.search);
                search.setPos(left, row2Y);
                search.setSize(250, 16);
                search.setTextColor(0xFFFFFF);
                search.setBackground(new Rectangle().setColor(0xFF1E1E30));
                search.setTextAlignment(Alignment.CenterLeft);
                search.setSetter(text -> state.search = text != null ? text : "");
                builder.widget(search);

                addButton(
                    builder,
                    left + 254,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.search"),
                    () -> {
                        state.page = 0;
                        showWindow();
                    });
                addButton(
                    builder,
                    left + 312,
                    row2Y,
                    54,
                    BUTTON_HEIGHT,
                    I18nUtil.tr("ae2patterngen.gui.browser.button.refresh"),
                    () -> requestOpen());
                break;
        }
    }

    private static String singletonLabel() {
        return I18nUtil.tr(
            state.showSingletonRules ? "ae2patterngen.gui.browser.button.singleton_on"
                : "ae2patterngen.gui.browser.button.singleton_off");
    }

    private static String unruledLabel() {
        return I18nUtil.tr(
            state.showUnruledGroup ? "ae2patterngen.gui.browser.button.unruled_on"
                : "ae2patterngen.gui.browser.button.unruled_off");
    }

    // ---------------------------------------------------------------- 行组件

    private static void addListHeader(ModularWindow.Builder builder, int contentWidth, String left, String right,
        int rightWidth) {
        DrawableWidget background = new DrawableWidget();
        background.setPos(8, CONTENT_TOP);
        background.setSize(contentWidth, HEADER_HEIGHT - 2);
        background.setDrawable(new Rectangle().setColor(HEADER_BG));
        builder.widget(background);

        TextWidget leftText = new TextWidget(EnumChatFormatting.GRAY + trim(left, contentWidth - rightWidth - 8));
        leftText.setPos(12, CONTENT_TOP + 3);
        leftText.setSize(contentWidth - rightWidth - 8, 10);
        builder.widget(leftText);

        TextWidget rightText = new TextWidget(EnumChatFormatting.GRAY + right);
        rightText.setPos(8 + contentWidth - rightWidth, CONTENT_TOP + 3);
        rightText.setSize(rightWidth - 4, 10);
        rightText.setTextAlignment(Alignment.CenterRight);
        builder.widget(rightText);
    }

    /** 带悬停高亮的整行按钮；action 为 null 时只做预览（不可点）。 */
    private static void addRowButton(ModularWindow.Builder builder, int x, int y, int width, int height,
        Runnable action, java.util.function.Supplier<java.util.List<String>> tooltip) {
        final IDrawable normal = new Rectangle().setColor(ROW_BG);
        final IDrawable hover = new Rectangle().setColor(ROW_BG_HOVER);

        ButtonWidget rowButton = new ButtonWidget();
        rowButton.setSynced(false, false);
        rowButton.setPos(x, y);
        rowButton.setSize(width, height);
        rowButton.setBackground(() -> new IDrawable[] { rowButton.isHovering() ? hover : normal });
        if (action != null) {
            rowButton.setOnClick((clickData, widget) -> action.run());
        }
        if (tooltip != null) {
            java.util.List<String> lines = tooltip.get();
            if (lines != null) {
                for (String line : lines) {
                    rowButton.addTooltip(line);
                }
            }
        }
        builder.widget(rowButton);
    }

    private static void addEmptyHint(ModularWindow.Builder builder, int contentWidth, String text) {
        TextWidget hint = new TextWidget(EnumChatFormatting.GRAY + text);
        hint.setPos(12, CONTENT_TOP + HEADER_HEIGHT + 2);
        hint.setSize(contentWidth - 8, 10);
        builder.widget(hint);
    }

    private static void addItemIcon(ModularWindow.Builder builder, int x, int y, ItemStack stack, String tooltip) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        DrawableWidget icon = new DrawableWidget();
        icon.setPos(x, y);
        icon.setSize(ICON_SIZE, ICON_SIZE);
        icon.setDrawable(new ItemDrawable(stack));
        if (tooltip != null && !tooltip.isEmpty()) {
            icon.addTooltip(tooltip);
        }
        builder.widget(icon);
    }

    private static int addIcons(ModularWindow.Builder builder, int x, int y, ItemStack[] stacks, int limit) {
        int currentX = x;
        int shown = 0;
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                if (stack == null || stack.getItem() == null || shown >= limit) {
                    continue;
                }
                addItemIcon(builder, currentX, y, stack, RecipeBrowserState.displayNameOf(stack));
                currentX += ICON_SIZE + 2;
                shown++;
            }
        }
        return currentX;
    }

    private static void addButton(ModularWindow.Builder builder, int x, int y, int width, int height, String label,
        Runnable action) {
        ButtonWidget button = new ButtonWidget();
        button.setSynced(false, false);
        button.setPos(x, y);
        button.setSize(width, height);
        button.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        button.setOnClick((clickData, widget) -> action.run());
        builder.widget(button);

        TextWidget text = new TextWidget(label);
        text.setPos(x + 2, y + (height - 8) / 2);
        text.setSize(Math.max(1, width - 4), 8);
        text.setTextAlignment(Alignment.Center);
        builder.widget(text);
    }

    // ---------------------------------------------------------------- 请求

    private static void openRecipes(PacketBrowseResponse.GroupSummary summary) {
        if (summary == null) {
            return;
        }
        state.groupRule = summary;
        state.groupId = summary.groupId;
        state.status = "";
        requestRecipes(summary.groupId);
    }

    private static void openPreview(PacketBrowseResponse.GroupSummary summary) {
        if (summary == null) {
            return;
        }
        state.groupRule = summary;
        state.groupId = summary.groupId;
        state.view = RecipeBrowserState.VIEW_PREVIEW;
        state.status = "";
        state.previewLoaded = false;
        state.clearPreviewData();
        showWindow();
        requestPreview();
    }

    private static void requestGroups(String mapId) {
        if (mapId == null || mapId.isEmpty()) {
            return;
        }
        NetworkHandler.INSTANCE
            .sendToServer(PacketBrowseRequest.groups(mapId, state.showSingletonRules, state.showUnruledGroup));
    }

    private static void requestRecipes(String groupId) {
        if (state.mapId.isEmpty() || groupId == null || groupId.isEmpty()) {
            return;
        }
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.recipes(state.mapId, groupId));
    }

    private static void requestGenerate(String groupId, int recipeIndex) {
        if (state.mapId.isEmpty() || groupId == null || groupId.isEmpty()) {
            return;
        }
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.generate(state.mapId, groupId, recipeIndex));
    }

    private static void requestPreview() {
        if (state.mapId.isEmpty() || state.groupId.isEmpty()) {
            return;
        }
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.preview(state.mapId, state.groupId, state.filters()));
    }

    private static void requestGenerateFiltered() {
        if (state.mapId.isEmpty() || state.groupId.isEmpty()) {
            return;
        }
        NetworkHandler.INSTANCE
            .sendToServer(PacketBrowseRequest.generateFiltered(state.mapId, state.groupId, state.filters()));
    }

    private static void requestStoragePage(int page) {
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.storageList(Math.max(0, page)));
    }

    private static void requestStorageDelete(int itemIndex) {
        NetworkHandler.INSTANCE.sendToServer(PacketBrowseRequest.storageDelete(itemIndex));
    }

    // ---------------------------------------------------------------- 纯文本工具

    private static String ruleLabel(PacketBrowseResponse.GroupSummary summary) {
        if (summary == null) {
            return "";
        }
        if (summary.unruled) {
            return I18nUtil.tr("ae2patterngen.gui.browser.rule.unruled");
        }
        String circuit = summary.circuitDamages.length == 0 ? I18nUtil.tr("ae2patterngen.gui.browser.rule.no_circuit")
            : I18nUtil.tr("ae2patterngen.gui.browser.rule.circuit", joinCircuits(summary.circuitDamages));
        if (summary.ncItems.length == 0) {
            return circuit;
        }
        return circuit + " · " + I18nUtil.tr("ae2patterngen.gui.browser.rule.nc_count", summary.ncItems.length);
    }

    private static String joinCircuits(int[] damages) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < damages.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(damages[i]);
        }
        return builder.toString();
    }

    private static String circuitTooltip(PacketBrowseResponse.GroupSummary summary) {
        return I18nUtil.tr("ae2patterngen.gui.browser.tooltip.circuit", joinCircuits(summary.circuitDamages));
    }

    private static java.util.List<String> ruleTooltipLines(PacketBrowseResponse.GroupSummary summary) {
        java.util.List<String> lines = new java.util.ArrayList<String>();
        lines.add(EnumChatFormatting.WHITE + ruleLabel(summary));
        if (summary.ncItems.length > 0) {
            lines.add(EnumChatFormatting.GRAY + I18nUtil.tr("ae2patterngen.gui.browser.tooltip.nc", ncNames(summary)));
        }
        if (summary.outputSamples.length > 0) {
            lines.add(
                EnumChatFormatting.GRAY
                    + I18nUtil.tr("ae2patterngen.gui.browser.tooltip.outputs", sampleNames(summary)));
        }
        lines.add(EnumChatFormatting.DARK_GRAY + I18nUtil.tr("ae2patterngen.gui.browser.hint.open_preview"));
        return lines;
    }

    private static String ncNames(PacketBrowseResponse.GroupSummary summary) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < summary.ncItems.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(RecipeBrowserState.displayNameOf(summary.ncItems[i]));
        }
        return builder.toString();
    }

    private static String sampleNames(PacketBrowseResponse.GroupSummary summary) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < summary.outputSamples.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(RecipeBrowserState.displayNameOf(summary.outputSamples[i]));
        }
        return builder.toString();
    }

    private static String recipeLabel(RecipeEntry recipe, int contentWidth) {
        ItemStack product = firstOutput(recipe);
        String productText = product != null
            ? RecipeBrowserState.displayNameOf(product) + " x" + Math.max(1, product.stackSize)
            : I18nUtil.tr("ae2patterngen.gui.browser.unknown_item");
        int productWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(productText);
        int previewWidth = Math.max(40, contentWidth - 140 - Math.min(productWidth, 130));
        return EnumChatFormatting.WHITE + trim(productText, 130)
            + EnumChatFormatting.GRAY
            + " <- "
            + trim(inputPreview(recipe), previewWidth);
    }

    private static String inputPreview(RecipeEntry recipe) {
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        if (recipe.inputs != null) {
            for (ItemStack stack : recipe.inputs) {
                if (stack == null || stack.getItem() == null || stack.stackSize <= 0) {
                    continue;
                }
                if (shown >= 3) {
                    builder.append("…");
                    break;
                }
                if (shown > 0) {
                    builder.append(" + ");
                }
                builder.append(RecipeBrowserState.displayNameOf(stack))
                    .append(" x")
                    .append(stack.stackSize);
                shown++;
            }
        }
        if (recipe.fluidInputs != null) {
            for (FluidStack fluid : recipe.fluidInputs) {
                if (fluid == null || fluid.getFluid() == null || fluid.amount <= 0) {
                    continue;
                }
                if (shown >= 4) {
                    builder.append("…");
                    break;
                }
                if (shown > 0) {
                    builder.append(" + ");
                }
                builder.append(fluid.amount)
                    .append("mB ")
                    .append(fluid.getLocalizedName());
                shown++;
            }
        }
        return builder.length() == 0 ? I18nUtil.tr("ae2patterngen.gui.browser.no_input") : builder.toString();
    }

    private static ItemStack firstOutput(RecipeEntry recipe) {
        if (recipe.outputs != null) {
            for (ItemStack stack : recipe.outputs) {
                if (stack != null && stack.getItem() != null) {
                    return stack;
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 分页

    private static int pageCount(int total, int capacity) {
        if (capacity <= 0) {
            return 1;
        }
        return Math.max(1, (total + capacity - 1) / capacity);
    }

    private static int currentPageCount(int rowCapacity) {
        switch (state.view) {
            case RecipeBrowserState.VIEW_RULES:
                return pageCount(state.rules.size(), rowCapacity);
            case RecipeBrowserState.VIEW_RECIPES:
                return pageCount(state.recipes.size(), rowCapacity);
            case RecipeBrowserState.VIEW_STORAGE:
                return pageCount(state.storageCount, Math.max(1, state.storagePageSize));
            case RecipeBrowserState.VIEW_PREVIEW:
                return 1;
            case RecipeBrowserState.VIEW_POOLS:
            default:
                return pageCount(
                    state.filteredPoolIndexes()
                        .size(),
                    rowCapacity);
        }
    }

    /** 翻页：仓储视图要回服务端取下一页，其它视图是本地数据直接重建。 */
    private static void goToPage(int page) {
        int target = Math.max(0, page);
        if (state.view == RecipeBrowserState.VIEW_STORAGE) {
            state.page = target;
            requestStoragePage(target);
            return;
        }
        state.page = target;
        showWindow();
    }

    private static void clampPage(int pageCount) {
        if (state.page < 0) {
            state.page = 0;
        }
        if (state.page >= pageCount) {
            state.page = Math.max(0, pageCount - 1);
        }
    }

    private static <T> List<T> slice(List<T> source, int capacity) {
        int from = Math.min(source.size(), state.page * capacity);
        int to = Math.min(source.size(), from + capacity);
        return source.subList(from, to);
    }

    private static String trim(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        int width = Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
        if (width <= maxWidth) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        int current = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            int charWidth = Minecraft.getMinecraft().fontRenderer.getCharWidth(character);
            if (current + charWidth + 6 > maxWidth) {
                break;
            }
            builder.append(character);
            current += charWidth;
        }
        return builder.append("...")
            .toString();
    }
}
