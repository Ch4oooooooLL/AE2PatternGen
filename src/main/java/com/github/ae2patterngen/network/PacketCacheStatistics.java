package com.github.ae2patterngen.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.gui.GuiPatternGenStatusBridge;
import com.github.ae2patterngen.storage.CacheStatistics;
import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: cache statistics after build completion.
 */
public class PacketCacheStatistics implements IMessage {

    private int totalRecipeCount;
    private int totalRecipeMaps;
    private int totalModCount;
    private long directoryBytes;

    public PacketCacheStatistics() {}

    public PacketCacheStatistics(CacheStatistics statistics) {
        this.totalRecipeCount = statistics.totalRecipeCount;
        this.totalRecipeMaps = statistics.totalRecipeMaps;
        this.totalModCount = statistics.totalModCount;
        this.directoryBytes = statistics.directoryBytes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        totalRecipeCount = buf.readInt();
        totalRecipeMaps = buf.readInt();
        totalModCount = buf.readInt();
        directoryBytes = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(totalRecipeCount);
        buf.writeInt(totalRecipeMaps);
        buf.writeInt(totalModCount);
        buf.writeLong(directoryBytes);
    }

    public static class Handler implements IMessageHandler<PacketCacheStatistics, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketCacheStatistics message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    String status = I18nUtil.tr(
                        "ae2patterngen.gui.pattern_gen.status.cache_ready",
                        message.totalRecipeMaps,
                        message.totalRecipeCount);
                    GuiPatternGenStatusBridge.setStatus(status);

                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(
                                EnumChatFormatting.GREEN + I18nUtil.tr(
                                    "ae2patterngen.msg.cache.statistics",
                                    message.totalRecipeMaps,
                                    message.totalRecipeCount,
                                    message.totalModCount,
                                    message.directoryBytes)));
                    }
                });
            return null;
        }
    }
}
