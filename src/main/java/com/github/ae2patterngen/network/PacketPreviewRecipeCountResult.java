package com.github.ae2patterngen.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.ae2patterngen.gui.GuiPatternGenStatusBridge;
import com.github.ae2patterngen.util.I18nUtil;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: preview count result from the persisted recipe cache.
 */
public class PacketPreviewRecipeCountResult implements IMessage {

    private boolean cacheValid;
    private String requestedKeyword;
    private int matchedMapCount;
    private int totalLoadedCount;
    private int totalFilteredCount;

    public PacketPreviewRecipeCountResult() {}

    public PacketPreviewRecipeCountResult(boolean cacheValid, String requestedKeyword, int matchedMapCount,
        int totalLoadedCount, int totalFilteredCount) {
        this.cacheValid = cacheValid;
        this.requestedKeyword = requestedKeyword;
        this.matchedMapCount = matchedMapCount;
        this.totalLoadedCount = totalLoadedCount;
        this.totalFilteredCount = totalFilteredCount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        cacheValid = buf.readBoolean();
        requestedKeyword = ByteBufUtils.readUTF8String(buf);
        matchedMapCount = buf.readInt();
        totalLoadedCount = buf.readInt();
        totalFilteredCount = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(cacheValid);
        ByteBufUtils.writeUTF8String(buf, requestedKeyword != null ? requestedKeyword : "");
        buf.writeInt(matchedMapCount);
        buf.writeInt(totalLoadedCount);
        buf.writeInt(totalFilteredCount);
    }

    public static class Handler implements IMessageHandler<PacketPreviewRecipeCountResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketPreviewRecipeCountResult message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> {
                    if (!message.cacheValid) {
                        String text = EnumChatFormatting.RED
                            + I18nUtil.tr("ae2patterngen.msg.cache.missing_or_invalid");
                        GuiPatternGenStatusBridge.setStatus(text);
                        if (Minecraft.getMinecraft().thePlayer != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(text));
                        }
                        return;
                    }

                    if (message.matchedMapCount <= 0) {
                        GuiPatternGenStatusBridge.setStatus(
                            EnumChatFormatting.RED
                                + I18nUtil.tr("ae2patterngen.msg.generate.no_matching_map", message.requestedKeyword));
                        return;
                    }

                    GuiPatternGenStatusBridge.setStatus(
                        I18nUtil.tr(
                            "ae2patterngen.gui.pattern_gen.status.filter_result",
                            message.totalLoadedCount,
                            message.totalFilteredCount));
                });
            return null;
        }
    }
}
