package com.github.ae2patterngen.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketRecipeConflictBatch;
import com.github.ae2patterngen.network.PacketRecipeConflicts;
import com.github.ae2patterngen.network.PacketResolveConflictsBatch;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiRecipePicker {

    private static final int GUI_W = 404;
    private static final int MIN_GUI_H = 250;
    private static final int IDEAL_GUI_H = 296;
    private static final int ROW_H = 30;
    private static final int ROW_GAP = 1;
    private static final int SELECT_BTN_W = 54;
    private static ClientBatchState activeState;

    public static void open(PacketRecipeConflicts message) {
        // Backward compatibility for single-group packet.
        List<String> productNames = new ArrayList<>();
        productNames.add(message != null && message.productName != null ? message.productName : "Unknown");

        List<List<RecipeEntry>> groups = new ArrayList<>();
        groups.add(message != null && message.recipes != null ? message.recipes : new ArrayList<RecipeEntry>());

        int startIndex = message != null ? message.currentIndex : 1;
        int total = message != null ? message.totalConflicts : groups.size();
        int maxCandidates = !groups.isEmpty() && groups.get(0) != null ? groups.get(0)
            .size() : 1;
        openBatch(new PacketRecipeConflictBatch(startIndex, total, Math.max(1, maxCandidates), productNames, groups));
    }

    public static void openBatch(PacketRecipeConflictBatch message) {
        if (message == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (activeState != null && mc.currentScreen instanceof ModularGui && activeState.applyPacket(message)) {
            return;
        }

        ClientBatchState state = ClientBatchState.from(message);
        if (state == null) return;
        activeState = state;
        openStateWindow(state);
    }

    private static void openStateWindow(ClientBatchState state) {
        UIBuildContext buildContext = new UIBuildContext(Minecraft.getMinecraft().thePlayer);
        buildContext.addCloseListener(() -> {
            if (activeState == state) {
                activeState = null;
            }
        });
        com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
            buildContext,
            () -> {});
        ModularWindow window = createWindow(buildContext, state);
        Minecraft.getMinecraft()
            .displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, window)));
    }

    public static ModularWindow createWindow(UIBuildContext buildContext, ClientBatchState state) {
        int rowCapacity = state.rowCapacity;

        net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(
            Minecraft.getMinecraft(),
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight);
        int maxH = Minecraft.getMinecraft().displayHeight / res.getScaleFactor() - 20;
        int guiH = Math.min(maxH, Math.max(MIN_GUI_H, IDEAL_GUI_H));

        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, guiH);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        TextWidget titleText = new TextWidget("");
        titleText
            .setStringSupplier(() -> EnumChatFormatting.BOLD + trimToPixelWidth(buildTitleText(state), GUI_W - 20));
        titleText.setScale(1.2f);
        titleText.setSize(GUI_W - 16, 20);
        titleText.setPos(8, 8);
        builder.widget(titleText);

        final int topY = 34;
        final int footerH = 12;
        final int contentH = guiH - topY - footerH - 6;
        final int leftW = 218;
        final int gap = 6;
        final int rightX = 8 + leftW + gap;
        final int rightW = GUI_W - rightX - 8;

        TextWidget listTitle = new TextWidget("");
        listTitle.setStringSupplier(
            () -> EnumChatFormatting.BOLD + "候选样板 ("
                + state.getCurrentRecipes()
                    .size()
                + ")");
        listTitle.setPos(8, topY - 10);
        builder.widget(listTitle);

        TextWidget detailTitle = new TextWidget(EnumChatFormatting.BOLD + "右侧详情");
        detailTitle.setPos(rightX, topY - 10);
        builder.widget(detailTitle);

        Scrollable candidateList = new Scrollable().setVerticalScroll();
        candidateList.setPos(8, topY);
        candidateList.setSize(leftW, contentH);

        int previewBtnW = leftW - 6 - SELECT_BTN_W - 4;
        int chooseLabelW = Minecraft.getMinecraft().fontRenderer.getStringWidth("选择");

        for (int i = 0; i < rowCapacity; i++) {
            final int recipeIndex = i;
            int rowY = i * (ROW_H + ROW_GAP);

            ButtonWidget previewBtn = new ButtonWidget();
            previewBtn.setPos(2, rowY + 1);
            previewBtn.setSize(previewBtnW, ROW_H);
            previewBtn.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
            previewBtn.setEnabled(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size())
                    && !state.awaitingServer);
            previewBtn.setOnClick((cd, w) -> {
                if (state.awaitingServer || state.inputLocked) return;
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) return;
                state.selectedRecipeIndex = recipeIndex;
                state.statusText = EnumChatFormatting.DARK_GRAY + "已预览 #" + (recipeIndex + 1) + "，如确认请点右侧“选择”";
            });
            candidateList.widget(previewBtn);

            TextWidget rowTitle = new TextWidget("");
            rowTitle.setStringSupplier(() -> {
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) return "";
                RecipeEntry recipe = currentRecipes.get(recipeIndex);
                return formatCandidateTitle(recipe, recipeIndex, state.selectedRecipeIndex == recipeIndex);
            });
            rowTitle.setEnabled(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size()));
            rowTitle.setPos(6, rowY + 6);
            candidateList.widget(rowTitle);

            TextWidget rowPreview = new TextWidget("");
            rowPreview.setStringSupplier(() -> {
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                if (recipeIndex < 0 || recipeIndex >= currentRecipes.size()) return "";
                RecipeEntry recipe = currentRecipes.get(recipeIndex);
                return formatInputPreview(recipe, state.selectedRecipeIndex == recipeIndex);
            });
            rowPreview.setEnabled(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size()));
            rowPreview.setPos(6, rowY + 18);
            candidateList.widget(rowPreview);

            int selectBtnX = 2 + previewBtnW + 4;
            ButtonWidget selectBtn = new ButtonWidget();
            selectBtn.setPos(selectBtnX, rowY + 1);
            selectBtn.setSize(SELECT_BTN_W, ROW_H);
            selectBtn.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
            selectBtn.setEnabled(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size())
                    && !state.awaitingServer);
            selectBtn.setOnClick((cd, w) -> {
                if (state.awaitingServer || state.inputLocked) return;
                state.inputLocked = true;
                List<RecipeEntry> currentRecipes = state.getCurrentRecipes();
                int chosenIndex = resolveChosenIndex(recipeIndex, state.selectedRecipeIndex, currentRecipes.size());
                if (chosenIndex < 0) {
                    state.statusText = EnumChatFormatting.RED + "当前冲突组无可用候选，无法提交。";
                    state.inputLocked = false;
                    return;
                }

                state.selectedRecipeIndex = chosenIndex;
                state.selections[state.localIndex] = chosenIndex;

                int batchSize = state.recipeGroups.size();
                if (state.localIndex < batchSize - 1) {
                    // Continue selecting next conflict locally without server round-trip and without reopening GUI.
                    state.localIndex++;
                    state.selectedRecipeIndex = state.getCurrentRecipes()
                        .isEmpty() ? -1 : 0;
                    state.statusText = buildDefaultStatusText(state);
                    state.inputLocked = false;
                    return;
                }

                // Final local selection in this batch: submit once.
                int[] payload = buildSubmittedSelections(state.selections, batchSize);
                state.inputLocked = false;
                NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflictsBatch(state.startIndex, false, payload));
                boolean isFinalConflict = state.currentConflictIndex() >= state.totalConflicts;
                if (isFinalConflict) {
                    state.statusText = EnumChatFormatting.YELLOW + "已提交最终冲突选择，正在完成生成...";
                    activeState = null;
                    Minecraft.getMinecraft()
                        .displayGuiScreen(null);
                    return;
                }
                state.awaitingServer = true;
                state.statusText = EnumChatFormatting.YELLOW + "已提交本批冲突选择，等待下一批...";
            });
            candidateList.widget(selectBtn);

            TextWidget selectBtnText = new TextWidget(EnumChatFormatting.BLACK + "选择");
            selectBtnText.setEnabled(
                widget -> isValidRecipeIndex(
                    recipeIndex,
                    state.getCurrentRecipes()
                        .size()));
            selectBtnText.setPos(selectBtnX + (SELECT_BTN_W - chooseLabelW) / 2, rowY + 12);
            candidateList.widget(selectBtnText);
        }

        builder.widget(candidateList);

        ButtonWidget detailPanel = new ButtonWidget();
        detailPanel.setPos(rightX, topY);
        detailPanel.setSize(rightW, contentH);
        detailPanel.setBackground(new Rectangle().setColor(0xD0141422));
        detailPanel.setOnClick((cd, w) -> {});
        builder.widget(detailPanel);

        Scrollable detailScroll = new Scrollable().setVerticalScroll();
        detailScroll.setPos(rightX + 6, topY + 6);
        detailScroll.setSize(rightW - 12, contentH - 12);

        final int maxDetailLines = 90;
        for (int lineIndex = 0; lineIndex < maxDetailLines; lineIndex++) {
            final int idx = lineIndex;
            TextWidget line = new TextWidget("");
            line.setStringSupplier(() -> getDetailLine(state.getCurrentRecipes(), state.selectedRecipeIndex, idx));
            line.setPos(2, idx * 10);
            detailScroll.widget(line);
        }
        builder.widget(detailScroll);

        TextWidget statusWidget = new TextWidget("");
        statusWidget.setStringSupplier(() -> trimToPixelWidth(state.statusText, GUI_W - 16));
        statusWidget.setPos(8, guiH - 11);
        builder.widget(statusWidget);

        final int cancelBtnW = 54;
        final int cancelBtnH = 12;
        final int cancelBtnX = GUI_W - 8 - cancelBtnW;
        final int cancelBtnY = guiH - 24;

        ButtonWidget cancelBtn = new ButtonWidget();
        cancelBtn.setPos(cancelBtnX, cancelBtnY);
        cancelBtn.setSize(cancelBtnW, cancelBtnH);
        cancelBtn.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        cancelBtn.setEnabled(widget -> !state.awaitingServer && !state.inputLocked);
        cancelBtn.setOnClick((cd, w) -> {
            if (state.awaitingServer || state.inputLocked) return;
            state.inputLocked = true;
            NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflictsBatch(state.startIndex, true, new int[0]));
            activeState = null;
            Minecraft.getMinecraft()
                .displayGuiScreen(null);
        });
        builder.widget(cancelBtn);

        TextWidget cancelBtnText = new TextWidget(EnumChatFormatting.BLACK + "放弃");
        cancelBtnText.setPos(cancelBtnX + 15, cancelBtnY + 2);
        builder.widget(cancelBtnText);

        return builder.build();
    }

    private static String formatCandidateTitle(RecipeEntry recipe, int index, boolean selected) {
        String prefix = selected ? EnumChatFormatting.DARK_BLUE + ">> " : EnumChatFormatting.DARK_GRAY + "   ";
        String displayName = trimText(getPrimaryOutputName(recipe), 18);
        return prefix + EnumChatFormatting.DARK_GRAY + "#" + (index + 1) + " " + displayName;
    }

    private static String formatInputPreview(RecipeEntry recipe, boolean selected) {
        String color = selected ? EnumChatFormatting.DARK_AQUA.toString() : EnumChatFormatting.DARK_GRAY.toString();
        return color + "输入: " + trimText(buildInputPreview(recipe, 2), 24);
    }

    private static String buildInputPreview(RecipeEntry recipe, int maxParts) {
        if (recipe == null) return "(无)";
        List<String> parts = new ArrayList<>();

        if (recipe.inputs != null) {
            for (ItemStack input : recipe.inputs) {
                if (input == null) continue;
                parts.add(safeText(input.getDisplayName()));
                if (parts.size() >= maxParts) break;
            }
        }

        if (parts.size() < maxParts && recipe.fluidInputs != null) {
            for (FluidStack fluid : recipe.fluidInputs) {
                if (fluid == null || fluid.getFluid() == null) continue;
                parts.add(safeText(fluid.getLocalizedName()) + " " + Math.max(0, fluid.amount) + "L");
                if (parts.size() >= maxParts) break;
            }
        }

        int totalCount = countItemStacks(recipe.inputs) + countFluidStacks(recipe.fluidInputs);
        if (parts.isEmpty()) return "(无)";
        String preview = String.join(", ", parts);
        if (totalCount > parts.size()) {
            preview += ", ...";
        }
        return preview;
    }

    private static String getDetailLine(List<RecipeEntry> recipes, int selectedIndex, int lineIndex) {
        RecipeEntry selected = getSelectedRecipe(recipes, selectedIndex);
        List<String> lines = buildDetailLines(selected);
        return lineIndex >= 0 && lineIndex < lines.size() ? lines.get(lineIndex) : "";
    }

    private static RecipeEntry getSelectedRecipe(List<RecipeEntry> recipes, int selectedIndex) {
        if (recipes == null) return null;
        if (selectedIndex < 0 || selectedIndex >= recipes.size()) return null;
        return recipes.get(selectedIndex);
    }

    private static List<String> buildDetailLines(RecipeEntry recipe) {
        ArrayList<String> lines = new ArrayList<>();
        if (recipe == null) {
            lines.add(EnumChatFormatting.RED + "未选择候选样板");
            return lines;
        }

        lines.add(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + "元信息");
        lines.add(EnumChatFormatting.WHITE + "机器: " + trimText(safeText(recipe.machineDisplayName), 24));
        lines.add(EnumChatFormatting.WHITE + "配方表: " + trimText(safeText(recipe.recipeMapId), 24));
        lines.add(EnumChatFormatting.WHITE + "耗时: " + recipe.duration + " tick");
        lines.add(EnumChatFormatting.WHITE + "EU/t: " + recipe.euPerTick);
        lines.add("");

        appendItemSection(lines, "输入物品", recipe.inputs, 20);
        appendFluidSection(lines, "输入流体", recipe.fluidInputs, 20);
        appendItemSection(lines, "输出物品", recipe.outputs, 20);
        appendFluidSection(lines, "输出流体", recipe.fluidOutputs, 20);
        appendItemSection(lines, "特殊项", recipe.specialItems, 20);
        return lines;
    }

    private static void appendItemSection(List<String> lines, String title, ItemStack[] stacks, int maxLen) {
        int count = countItemStacks(stacks);
        lines.add(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + title + " (" + count + ")");
        if (count == 0) {
            lines.add(EnumChatFormatting.GRAY + " - (无)");
            lines.add("");
            return;
        }

        for (ItemStack stack : stacks) {
            if (stack == null) continue;
            String name = safeText(stack.getDisplayName());
            int amount = stack.stackSize;
            lines.add(
                EnumChatFormatting.GRAY + " - " + EnumChatFormatting.WHITE + trimText(name, maxLen) + " x" + amount);
        }
        lines.add("");
    }

    private static void appendFluidSection(List<String> lines, String title, FluidStack[] fluids, int maxLen) {
        int count = countFluidStacks(fluids);
        lines.add(EnumChatFormatting.AQUA + "" + EnumChatFormatting.BOLD + title + " (" + count + ")");
        if (count == 0) {
            lines.add(EnumChatFormatting.GRAY + " - (无)");
            lines.add("");
            return;
        }

        for (FluidStack fluid : fluids) {
            if (fluid == null || fluid.getFluid() == null) continue;
            String name = safeText(fluid.getLocalizedName());
            lines.add(
                EnumChatFormatting.GRAY + " - "
                    + EnumChatFormatting.AQUA
                    + trimText(name, maxLen)
                    + EnumChatFormatting.GRAY
                    + " "
                    + Math.max(0, fluid.amount)
                    + "L");
        }
        lines.add("");
    }

    private static String getPrimaryOutputName(RecipeEntry recipe) {
        if (recipe == null) return "(未知)";
        if (recipe.outputs != null) {
            for (ItemStack out : recipe.outputs) {
                if (out != null) {
                    return safeText(out.getDisplayName());
                }
            }
        }
        if (recipe.fluidOutputs != null) {
            for (FluidStack out : recipe.fluidOutputs) {
                if (out != null && out.getFluid() != null) {
                    return safeText(out.getLocalizedName());
                }
            }
        }
        return "(未知)";
    }

    private static int countItemStacks(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) return 0;
        int count = 0;
        for (ItemStack stack : stacks) {
            if (stack != null) {
                count++;
            }
        }
        return count;
    }

    private static int countFluidStacks(FluidStack[] stacks) {
        if (stacks == null || stacks.length == 0) return 0;
        int count = 0;
        for (FluidStack stack : stacks) {
            if (stack != null && stack.getFluid() != null) {
                count++;
            }
        }
        return count;
    }

    private static String trimText(String text, int maxLen) {
        String safe = safeText(text);
        if (safe.length() <= maxLen) return safe;
        return safe.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private static String trimToPixelWidth(String text, int maxWidth) {
        String safe = text != null ? text : "";
        net.minecraft.client.gui.FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        if (fr == null || fr.getStringWidth(safe) <= maxWidth) return safe;

        String suffix = "...";
        int suffixWidth = fr.getStringWidth(suffix);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            String candidate = sb.toString() + ch;
            if (fr.getStringWidth(candidate) + suffixWidth > maxWidth) break;
            sb.append(ch);
        }
        return sb + suffix;
    }

    private static String safeText(String text) {
        return text != null && !text.isEmpty() ? text : "(无)";
    }

    private static int[] buildSubmittedSelections(int[] selections, int expectedCount) {
        if (selections == null || expectedCount <= 0) {
            return new int[0];
        }
        int size = Math.min(expectedCount, selections.length);
        for (int i = 0; i < size; i++) {
            if (selections[i] < 0) {
                return new int[0];
            }
        }
        return java.util.Arrays.copyOf(selections, size);
    }

    private static boolean isValidRecipeIndex(int index, int recipeCount) {
        return recipeCount > 0 && index >= 0 && index < recipeCount;
    }

    private static int resolveChosenIndex(int clickedIndex, int selectedIndex, int recipeCount) {
        if (isValidRecipeIndex(clickedIndex, recipeCount)) {
            return clickedIndex;
        }
        if (isValidRecipeIndex(selectedIndex, recipeCount)) {
            return selectedIndex;
        }
        return recipeCount > 0 ? 0 : -1;
    }

    private static int getMaxRecipeCount(List<List<RecipeEntry>> groups) {
        if (groups == null || groups.isEmpty()) return 0;
        int max = 0;
        for (List<RecipeEntry> recipes : groups) {
            if (recipes != null && recipes.size() > max) {
                max = recipes.size();
            }
        }
        return max;
    }

    private static String buildTitleText(ClientBatchState state) {
        String productName = state.getCurrentProductName();
        int current = state.currentConflictIndex();
        int total = state.totalConflicts;
        int remaining = current > 0 ? Math.max(0, total - current + 1) : Math.max(0, total);
        return "配方冲突: " + productName + " | 剩余待选: " + remaining;
    }

    private static String buildDefaultStatusText(ClientBatchState state) {
        int recipeCount = state.getCurrentRecipes()
            .size();
        int current = state.currentConflictIndex();
        int total = state.totalConflicts;
        return recipeCount > 0
            ? EnumChatFormatting.DARK_GRAY + "候选样板 " + current + "/" + total + "，点击条目预览，点右侧按钮提交，按 ESC 仅关闭"
            : EnumChatFormatting.RED + "当前冲突组没有可选配方，可点“放弃”取消";
    }

    public static class ClientBatchState {

        public int startIndex;
        public int totalConflicts;
        public List<String> productNames;
        public List<List<RecipeEntry>> recipeGroups;
        public int[] selections;
        public final int rowCapacity;
        public int localIndex;
        public int selectedRecipeIndex;
        public String statusText;
        public boolean awaitingServer;
        public boolean inputLocked;

        private ClientBatchState(int rowCapacity) {
            this.startIndex = 1;
            this.totalConflicts = 1;
            this.productNames = new ArrayList<>();
            this.recipeGroups = new ArrayList<>();
            this.selections = new int[0];
            this.rowCapacity = Math.max(1, rowCapacity);
            this.localIndex = 0;
            this.selectedRecipeIndex = -1;
            this.statusText = "";
            this.awaitingServer = false;
            this.inputLocked = false;
        }

        static ClientBatchState from(PacketRecipeConflictBatch packet) {
            int capacity = resolveRowCapacity(packet);
            ClientBatchState state = new ClientBatchState(capacity);
            return state.applyPacket(packet) ? state : null;
        }

        boolean applyPacket(PacketRecipeConflictBatch packet) {
            if (packet == null || packet.productNames == null || packet.recipeGroups == null) return false;
            if (packet.productNames.isEmpty() || packet.recipeGroups.isEmpty()) return false;

            int count = Math.min(packet.productNames.size(), packet.recipeGroups.size());
            if (count <= 0) return false;

            int requiredCapacity = resolveRowCapacity(packet);
            if (requiredCapacity > this.rowCapacity) {
                return false;
            }

            List<String> names = new ArrayList<>(count);
            List<List<RecipeEntry>> groups = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                names.add(packet.productNames.get(i));
                List<RecipeEntry> recipes = packet.recipeGroups.get(i);
                groups.add(recipes != null ? recipes : new ArrayList<RecipeEntry>());
            }

            this.startIndex = Math.max(1, packet.startIndex);
            this.totalConflicts = Math.max(1, packet.totalConflicts);
            this.productNames = names;
            this.recipeGroups = groups;
            this.selections = new int[groups.size()];
            java.util.Arrays.fill(this.selections, -1);
            this.localIndex = 0;
            this.selectedRecipeIndex = groups.get(0)
                .isEmpty() ? -1 : 0;
            this.awaitingServer = false;
            this.inputLocked = false;
            this.statusText = buildDefaultStatusText(this);
            return true;
        }

        private static int resolveRowCapacity(PacketRecipeConflictBatch packet) {
            if (packet == null) return 1;
            int announcedMax = Math.max(0, packet.maxCandidatesPerGroup);
            int packetMax = getMaxRecipeCount(packet.recipeGroups);
            return Math.max(1, Math.max(announcedMax, packetMax));
        }

        int currentConflictIndex() {
            return startIndex + localIndex;
        }

        String getCurrentProductName() {
            if (localIndex < 0 || localIndex >= productNames.size()) return "(未知产物)";
            String name = productNames.get(localIndex);
            return name != null ? name : "(未知产物)";
        }

        List<RecipeEntry> getCurrentRecipes() {
            if (localIndex < 0 || localIndex >= recipeGroups.size()) return new ArrayList<RecipeEntry>();
            List<RecipeEntry> recipes = recipeGroups.get(localIndex);
            return recipes != null ? recipes : new ArrayList<RecipeEntry>();
        }
    }
}
