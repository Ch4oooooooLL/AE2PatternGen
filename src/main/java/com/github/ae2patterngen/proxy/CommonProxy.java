package com.github.ae2patterngen.proxy;

import com.github.ae2patterngen.item.ModItems;
import com.github.ae2patterngen.network.NetworkHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModItems.init();
        NetworkHandler.init();
    }

    public void init(FMLInitializationEvent event, Object modInstance) {
        cpw.mods.fml.common.network.NetworkRegistry.INSTANCE
            .registerGuiHandler(modInstance, new com.github.ae2patterngen.gui.GuiHandler());
        cpw.mods.fml.common.registry.GameRegistry.addShapedRecipe(
            new net.minecraft.item.ItemStack(ModItems.itemPatternGenerator),
            "ABA",
            "BCB",
            "ABA",
            'A',
            new net.minecraft.item.ItemStack(
                cpw.mods.fml.common.registry.GameRegistry.findItem("gregtech", "gt.metaitem.01"),
                1,
                32653),
            'B',
            new net.minecraft.item.ItemStack(
                cpw.mods.fml.common.registry.GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"),
                1,
                52),
            'C',
            new net.minecraft.item.ItemStack(
                cpw.mods.fml.common.registry.GameRegistry.findItem("appliedenergistics2", "item.ItemMultiPart"),
                1,
                340));
    }
}
