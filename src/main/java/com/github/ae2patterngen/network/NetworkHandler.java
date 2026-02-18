package com.github.ae2patterngen.network;

import com.github.ae2patterngen.AE2PatternGen;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * 网络通信管理
 */
public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(AE2PatternGen.MODID);

    private static int packetId = 0;

    public static void init() {
        INSTANCE.registerMessage(
            PacketGeneratePatterns.Handler.class,
            PacketGeneratePatterns.class,
            packetId++,
            Side.SERVER);

        INSTANCE.registerMessage(PacketSaveFields.Handler.class, PacketSaveFields.class, packetId++, Side.SERVER);
    }

    public static void sendToServer(PacketGeneratePatterns packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendSaveFieldsToServer(PacketSaveFields packet) {
        INSTANCE.sendToServer(packet);
    }
}
