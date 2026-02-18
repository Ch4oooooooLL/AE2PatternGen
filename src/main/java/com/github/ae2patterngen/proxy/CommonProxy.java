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

    public void init(FMLInitializationEvent event) {
        // nothing on common side
    }
}
