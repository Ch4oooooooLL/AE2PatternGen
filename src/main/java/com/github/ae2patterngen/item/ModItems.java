package com.github.ae2patterngen.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModItems {

    public static Item itemPatternGenerator;
    public static Item itemRecipeBrowser;

    public static void init() {
        itemPatternGenerator = new ItemPatternGenerator();
        GameRegistry.registerItem(itemPatternGenerator, "pattern_generator");

        itemRecipeBrowser = new ItemRecipeBrowser();
        GameRegistry.registerItem(itemRecipeBrowser, "recipe_browser");
    }
}
