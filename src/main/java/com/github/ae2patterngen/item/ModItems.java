package com.github.ae2patterngen.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModItems {

    public static Item itemPatternGenerator;

    public static void init() {
        itemPatternGenerator = new ItemPatternGenerator();
        GameRegistry.registerItem(itemPatternGenerator, "pattern_generator");
    }
}
