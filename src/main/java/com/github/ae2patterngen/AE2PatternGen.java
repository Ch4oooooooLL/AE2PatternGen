package com.github.ae2patterngen;

import com.github.ae2patterngen.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = AE2PatternGen.MODID,
    name = AE2PatternGen.MODNAME,
    version = Tags.VERSION,
    dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:NotEnoughItems")
public class AE2PatternGen {

    public static final String MODID = "ae2patterngen";
    public static final String MODNAME = "AE2 Pattern Generator";

    @Mod.Instance(MODID)
    public static AE2PatternGen instance;

    @SidedProxy(
        clientSide = "com.github.ae2patterngen.proxy.ClientProxy",
        serverSide = "com.github.ae2patterngen.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event, instance);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new com.github.ae2patterngen.command.CommandPatternGen());
    }
}
