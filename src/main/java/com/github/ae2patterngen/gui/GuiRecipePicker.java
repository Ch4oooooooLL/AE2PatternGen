package com.github.ae2patterngen.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.network.NetworkHandler;
import com.github.ae2patterngen.network.PacketRecipeConflicts;
import com.github.ae2patterngen.network.PacketResolveConflicts;
import com.github.ae2patterngen.recipe.RecipeEntry;
import com.gtnewhorizons.modularui.api.drawable.ItemDrawable;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularUIContainer;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

public class GuiRecipePicker {

    private static final int GUI_W = 320;
    private static final int RECIPE_H = 50;

    public static void open(PacketRecipeConflicts message) {
        UIBuildContext buildContext = new UIBuildContext(Minecraft.getMinecraft().thePlayer);
        com.gtnewhorizons.modularui.api.screen.ModularUIContext muiContext = new com.gtnewhorizons.modularui.api.screen.ModularUIContext(
            buildContext,
            () -> {});
        ModularWindow window = createWindow(buildContext, message);
        Minecraft.getMinecraft()
            .displayGuiScreen(new ModularGui(new ModularUIContainer(muiContext, window)));
    }

    public static ModularWindow createWindow(UIBuildContext buildContext, PacketRecipeConflicts data) {
        int idealH = 40 + data.recipes.size() * (RECIPE_H + 5) + 35;
        net.minecraft.client.gui.ScaledResolution res = new net.minecraft.client.gui.ScaledResolution(
            Minecraft.getMinecraft(),
            Minecraft.getMinecraft().displayWidth,
            Minecraft.getMinecraft().displayHeight);
        int maxH = Minecraft.getMinecraft().displayHeight / res.getScaleFactor() - 20;
        int guiH = Math.min(idealH, maxH);

        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, guiH);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        String strTitle = EnumChatFormatting.BOLD + "配方冲突: " + EnumChatFormatting.WHITE + data.productName;
        TextWidget titleText = new TextWidget(strTitle);
        titleText.setScale(1.2f);
        titleText.setSize(300, 20);
        titleText
            .setPos(GUI_W / 2 - (int) (Minecraft.getMinecraft().fontRenderer.getStringWidth(strTitle) * 1.2f) / 2, 8);
        builder.widget(titleText);

        String strProgress = "(" + data.currentIndex + " / " + data.totalConflicts + ")";
        TextWidget progressText = new TextWidget(strProgress);
        progressText.setPos(GUI_W / 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(strProgress) / 2, 20);
        builder.widget(progressText);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 35);
        scrollable.setSize(GUI_W - 16, guiH - 35 - 35);

        for (int i = 0; i < data.recipes.size(); i++) {
            RecipeEntry re = data.recipes.get(i);
            int ry = i * (RECIPE_H + 5);

            TextWidget inLabel = new TextWidget(EnumChatFormatting.BOLD + "原料:");
            inLabel.setPos(4, ry + 20);
            scrollable.widget(inLabel);

            drawRecipeItems(scrollable, re.inputs, 4 + 30, ry + 15);

            TextWidget outLabel = new TextWidget(EnumChatFormatting.BOLD + "产物:");
            outLabel.setPos(142, ry + 20);
            scrollable.widget(outLabel);

            drawRecipeItems(scrollable, re.outputs, 142 + 30, ry + 15);

            final int recipeIndex = i;
            ButtonWidget btnSelect = new ButtonWidget();
            int btnX = GUI_W - 16 - 60 - 8;
            int btnY = ry + 15;
            btnSelect.setPos(btnX, btnY);
            btnSelect.setSize(50, 20);
            btnSelect.setOnClick(
                (cd, w) -> { NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflicts(recipeIndex, false)); });
            btnSelect.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
            scrollable.widget(btnSelect);

            TextWidget btnText = new TextWidget("选择");
            btnText.setPos(btnX + 13, btnY + 6);
            scrollable.widget(btnText);
        }

        builder.widget(scrollable);

        ButtonWidget btnCancel = new ButtonWidget();
        int cancelX = (GUI_W - 80) / 2;
        int cancelY = guiH - 25;
        btnCancel.setPos(cancelX, cancelY);
        btnCancel.setSize(80, 20);
        btnCancel.setOnClick((cd, w) -> {
            NetworkHandler.INSTANCE.sendToServer(new PacketResolveConflicts(0, true));
            Minecraft.getMinecraft()
                .displayGuiScreen(null);
        });
        btnCancel.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        builder.widget(btnCancel);

        TextWidget btnCancelText = new TextWidget("放弃 (Cancel)");
        btnCancelText.setPos(cancelX + 8, cancelY + 6);
        builder.widget(btnCancelText);

        return builder.build();
    }

    private static void drawRecipeItems(Scrollable parent, ItemStack[] items, int startX, int startY) {
        if (items == null) return;
        int count = 0;
        for (ItemStack stack : items) {
            if (stack == null || count >= 7) continue;

            DrawableWidget slot = new DrawableWidget();
            slot.setDrawable(new ItemDrawable(stack));
            slot.setPos(startX + count * 18, startY);
            slot.setSize(18, 18);
            parent.widget(slot);
            count++;
        }
    }
}
