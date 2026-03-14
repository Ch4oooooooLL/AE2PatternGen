package com.github.ae2patterngen.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.ae2patterngen.filter.CompositeFilter;
import com.github.ae2patterngen.filter.RecipeFilterFactory;
import com.github.ae2patterngen.storage.CacheQueryResult;
import com.github.ae2patterngen.storage.RecipeCacheService;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: request preview counts from the persisted recipe cache.
 */
public class PacketPreviewRecipeCount implements IMessage {

    private String recipeMapId;
    private String outputOreDict;
    private String inputOreDict;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;
    private int targetTier;

    public PacketPreviewRecipeCount() {}

    public PacketPreviewRecipeCount(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput, int targetTier) {
        this.recipeMapId = recipeMapId;
        this.outputOreDict = outputOreDict;
        this.inputOreDict = inputOreDict;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
        this.targetTier = targetTier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        outputOreDict = ByteBufUtils.readUTF8String(buf);
        inputOreDict = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
        targetTier = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMapId != null ? recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, outputOreDict != null ? outputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, inputOreDict != null ? inputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
        buf.writeInt(targetTier);
    }

    public static class Handler implements IMessageHandler<PacketPreviewRecipeCount, IMessage> {

        @Override
        public IMessage onMessage(PacketPreviewRecipeCount message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            CompositeFilter filter = RecipeFilterFactory.build(
                message.outputOreDict,
                message.inputOreDict,
                message.ncItem,
                message.blacklistInput,
                message.blacklistOutput,
                message.targetTier);
            CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes(message.recipeMapId, filter);
            NetworkHandler.INSTANCE.sendTo(
                new PacketPreviewRecipeCountResult(
                    result.cacheValid,
                    message.recipeMapId,
                    result.matchedMapIds.size(),
                    result.totalLoadedCount,
                    result.totalFilteredCount),
                player);
            return null;
        }
    }
}
