package com.github.ae2patterngen.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.ae2patterngen.storage.CacheStatistics;
import com.github.ae2patterngen.storage.RecipeCacheService;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: request recipe cache creation or refresh.
 */
public class PacketCreateCache implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketCreateCache, IMessage> {

        @Override
        public IMessage onMessage(PacketCreateCache message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            boolean started = RecipeCacheService.createOrRefreshCache(new RecipeCacheService.ProgressNotifier() {

                @Override
                public void onProgress(String progressMessage, int current, int total) {
                    NetworkHandler.INSTANCE.sendTo(
                        new PacketCacheProgress(PacketCacheProgress.STAGE_PROGRESS, progressMessage, current, total),
                        player);
                }

                @Override
                public void onComplete(CacheStatistics statistics) {
                    NetworkHandler.INSTANCE.sendTo(new PacketCacheStatistics(statistics), player);
                }

                @Override
                public void onError(String message) {
                    NetworkHandler.INSTANCE
                        .sendTo(new PacketCacheProgress(PacketCacheProgress.STAGE_ERROR, message, 0, 0), player);
                }
            });

            if (started) {
                NetworkHandler.INSTANCE
                    .sendTo(new PacketCacheProgress(PacketCacheProgress.STAGE_STARTED, "", 0, 0), player);
            } else {
                NetworkHandler.INSTANCE
                    .sendTo(new PacketCacheProgress(PacketCacheProgress.STAGE_ALREADY_RUNNING, "", 0, 0), player);
            }
            return null;
        }
    }
}
